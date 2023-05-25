package com.hcmut.test.object;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import com.hcmut.test.BuildConfig;
import com.hcmut.test.algorithm.CoordinateTransform;
import com.hcmut.test.algorithm.TileSystem;
import com.hcmut.test.geometry.BoundBox;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.local.DbDao;
import com.hcmut.test.local.TileEntity;
import com.hcmut.test.local.WayEntity;
import com.hcmut.test.mapnik.Layer;
import com.hcmut.test.osm.Node;
import com.hcmut.test.osm.Way;
import com.hcmut.test.remote.APIService;
import com.hcmut.test.remote.BaseResponse;
import com.hcmut.test.remote.DirectResponse;
import com.hcmut.test.remote.LayerRequest;
import com.hcmut.test.remote.LayerResponse;
import com.hcmut.test.remote.NodeResponse;
import com.hcmut.test.remote.RetrofitClient;
import com.hcmut.test.remote.WayResponse;
import com.hcmut.test.utils.Config;
import com.hcmut.test.utils.Debounce;

import org.osgeo.proj4j.ProjCoordinate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("NewApi")
public class MapView {
    private static final String TAG = "MapView";
    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
            NUMBER_OF_CORES,
            NUMBER_OF_CORES,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());
    private static final float USER_RADIUS_BUFFER = 1.2f;
    private static final float LAT_CHANGE_THRESHOLD = 0.0001f;
    private static final float LON_CHANGE_THRESHOLD = 0.0001f;
    private final ReentrantLock lock = new ReentrantLock();
    private final HashMap<String, Layer> layersMap = new HashMap<>(78);
    private List<Layer> layersOrder;
    private final Config config;
    private final Nav nav;
    private float curLon = 0;
    private float curLat = 0;
    private HashSet<Long> curTileIds = new HashSet<>(0);
    private final Debounce debounce = new Debounce(1000);

    public MapView(Config config) {
        this.config = config;
        nav = new Nav(config);
    }

    public void setLayers(List<Layer> layers) {
        layersOrder = layers;
        for (Layer layer : layers) {
            layersMap.put(layer.name, layer);
        }
    }

    private boolean isTileValid(long tileId, Point curPoint, float radius) {
        float[] bbox = TileSystem.getBoundBox(tileId);
        Point bboxMin = new Point(bbox[0], bbox[1]);
        Point bboxMax = new Point(bbox[2], bbox[3]);
        Point bboxCenter = bboxMin.midPoint(bboxMax);
        float bboxRadius = bboxCenter.distance(bboxMax);

        return curPoint.distance(bboxCenter) < radius * USER_RADIUS_BUFFER + bboxRadius;
    }

    private HashSet<Long> getTileIds(float lon, float lat, float radius, int curLevel) {
        assert curLevel >= 0;
        long tileId = TileSystem.getTileId(lon, lat);
        int n = 2 * curLevel + 1;
        HashSet<Long> rv = new HashSet<>(n * n);
        if (curLevel == 0) {
            rv.add(tileId);
            rv.addAll(getTileIds(lon, lat, radius, curLevel + 1));
            return rv;
        }

        Point curPoint = new Point(lon, lat);
        boolean atLeastOne = false;

        int startCol = -curLevel;
        int endCol = curLevel;
        int startRow = -curLevel;
        int endRow = curLevel;

        for (int i = startCol; i <= endCol; i++) {
            // Process the top and bottom row
            long id = TileSystem.getTileId(tileId, i, startRow);
            if (isTileValid(id, curPoint, radius)) {
                rv.add(id);
                atLeastOne = true;
            }
            id = TileSystem.getTileId(tileId, i, endRow);
            if (isTileValid(id, curPoint, radius)) {
                rv.add(id);
                atLeastOne = true;
            }
        }

        for (int i = startRow + 1; i < endRow; i++) {
            // Process the left and right column
            long id = TileSystem.getTileId(tileId, startCol, i);
            if (isTileValid(id, curPoint, radius)) {
                rv.add(id);
                atLeastOne = true;
            }
            id = TileSystem.getTileId(tileId, endCol, i);
            if (isTileValid(id, curPoint, radius)) {
                rv.add(id);
                atLeastOne = true;
            }
        }


//        Log.d(TAG, "curLevel: " + curLevel + ", rv.size(): " + rv.size());
        if (atLeastOne) {
            rv.addAll(getTileIds(lon, lat, radius, curLevel + 1));
        }

        return rv;
    }

    public void setCurLocation(float lon, float lat, float radius) {
//        if (curLon == 0 && BuildConfig.DEBUG) {
//            config.dbDao.clearAll();
//        }

        if (layersMap.isEmpty()) {
            Log.w(TAG, "layersMap is empty");
            return;
        }

        if (Math.abs(curLon - lon) < LON_CHANGE_THRESHOLD && Math.abs(curLat - lat) < LAT_CHANGE_THRESHOLD) {
            return;
        }

        curLon = lon;
        curLat = lat;

        if (radius > 0) {
            THREAD_POOL_EXECUTOR.execute(() -> {
                HashSet<Long> possibleTiles = getTileIds(lon, lat, radius, 0);
                Log.d(TAG, "radius: " + radius + ", possibleTiles.size(): " + possibleTiles.size());
                request(possibleTiles);
            });
        }
    }

    public void setRoute(double lon, double lat, double destLon, double destLat) {
        APIService apiService = RetrofitClient.getApiService();
        apiService.getFindDirect(lat, lon, destLat, destLon, "time").enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<DirectResponse>>> call, @NonNull Response<BaseResponse<List<DirectResponse>>> response) {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    List<DirectResponse> directResponses = response.body().getData();
                    if (directResponses != null && directResponses.size() > 0) {
                        DirectResponse directResponse = directResponses.get(0);
                        THREAD_POOL_EXECUTOR.execute(() -> nav.setRoute(directResponse));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<DirectResponse>>> call, Throwable t) {
                Log.e(TAG, "onFailure: ", t);
            }
        });

    }

    void request(HashSet<Long> possibleTiles) {
        Log.d(TAG, "request: " + possibleTiles.size());
        lock.lock();
        if (possibleTiles.size() == 0) {
            return;
        }

        HashSet<Long> diff = new HashSet<>(possibleTiles);
        diff.removeAll(curTileIds);
        HashSet<Long> removeTiles = new HashSet<>(curTileIds);
        removeTiles.removeAll(possibleTiles);

        Log.d(TAG, "diff.size(): " + diff.size() + ", removeTiles.size(): " + removeTiles.size());

        if (removeTiles.size() > 0) {
            removeTiles(removeTiles);
        }

        if (diff.size() > 0) {
            List<Long> missingTiles = requestLocal(diff);
            Log.d(TAG, "missingTiles.size(): " + missingTiles.size());
            requestAPI(missingTiles);
        }

        curTileIds = possibleTiles;
        Log.d(TAG, "curTileIds.size(): " + curTileIds.size());
        lock.unlock();
    }

    private void removeTiles(HashSet<Long> removeTiles) {
        List<Future<?>> futures = new ArrayList<>(layersOrder.size());
        for (Layer layer : layersOrder) {
            Future<?> curFuture = THREAD_POOL_EXECUTOR.submit(() -> {
                layer.removeWays(removeTiles);
                layer.save();
            });
            futures.add(curFuture);
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, "InterruptedException occurred: " + e.getMessage());
            }
        }
    }

    private void renderLayers(HashMap<String, List<Way>> waysInLayer, long tileId) {
        List<Future<?>> futures = new ArrayList<>(waysInLayer.size());

        HashMap<Future<?>, String> futureLayerMap = new HashMap<>(waysInLayer.size());
        for (String layerName : waysInLayer.keySet()) {
            Layer curLayer = layersMap.get(layerName);
            List<Way> ways = waysInLayer.get(layerName);
            if (curLayer == null || ways == null) {
                Log.e(TAG, "layer not found: " + layerName + ", ways: " + (ways == null ? null : ways.size()));
                continue;
            }

            Future<?> curFuture = THREAD_POOL_EXECUTOR.submit(() -> curLayer.addWays(ways, tileId));

            futures.add(curFuture);
            futureLayerMap.put(curFuture, layerName);
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, "InterruptedException occurred: " + e.getMessage()+ ", layer: " + futureLayerMap.get(future));
            }
        }

        for (String layerName : waysInLayer.keySet()) {
            Layer curLayer = layersMap.get(layerName);
            if (curLayer == null) {
                Log.e(TAG, "layer not found: " + layerName);
                continue;
            }
            curLayer.save();
        }
    }

    private List<Long> requestLocal(HashSet<Long> tiles) {
        DbDao dbDao = config.dbDao;
        HashSet<Long> localTiles = new HashSet<>(dbDao.getAllTileIds(new ArrayList<>(tiles)));
        if (localTiles.size() == 0) {
            return new ArrayList<>(tiles);
        }

        List<Long> missingTiles = new ArrayList<>(tiles.size() - localTiles.size());

        for (long tileId : tiles) {
            if (localTiles.contains(tileId)) {
                List<WayEntity> wayEntities = dbDao.getWaysByTileId(tileId);
                HashMap<String, List<Way>> waysInLayer = new HashMap<>(layersMap.size());
                for (WayEntity wayEntity : wayEntities) {
                    Way way = wayEntity.toWay();
                    for (String layer : way.tags.keySet()) {
                        waysInLayer.computeIfAbsent(layer, k -> new ArrayList<>()).add(way);
                    }
                }
                renderLayers(waysInLayer, tileId);
            } else {
                missingTiles.add(tileId);
            }
        }

        return missingTiles;
    }

    private void requestAPI(List<Long> missingTiles) {
        if (missingTiles.size() == 0) {
            return;
        }

        CompletableFuture<?> await = new CompletableFuture<>();
        AtomicInteger counter = new AtomicInteger(missingTiles.size());
        for (int i = 0; i < missingTiles.size(); i++) {
            long tileId = missingTiles.get(i);

            float[] bbox = TileSystem.getBoundBox(tileId);
            LayerRequest layerRequest = new LayerRequest(bbox);
            layerRequest.post(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BaseResponse<LayerResponse>> call, @NonNull Response<BaseResponse<LayerResponse>> response) {
                    if (response.body() == null) {
                        Log.e(TAG, "response.body() == null");
                        return;
                    }

                    LayerResponse layerResponse = response.body().getData();
                    validateResponse(layerResponse, tileId);

                    if (counter.decrementAndGet() == 0) {
                        await.complete(null);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BaseResponse<LayerResponse>> call, @NonNull Throwable t) {
                    Log.e(TAG, "onFailure: " + t.getMessage());
                    if (counter.decrementAndGet() == 0) {
                        await.complete(null);
                    }
                }
            });
        }

        try {
            await.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void validateResponse(LayerResponse layerResponse, long tileId) {
        Log.d(TAG, "layerResponse = " + layerResponse + " at titleId = " + tileId);
        NodeResponse[] nodeResponses = layerResponse.nodes;
        WayResponse[] wayResponses = layerResponse.ways;

        HashMap<Long, Node> nodeMap = new HashMap<>(nodeResponses.length);
        for (NodeResponse nodeResponse : nodeResponses) {
            nodeMap.put(nodeResponse.id, new Node(nodeResponse.lon, nodeResponse.lat));
        }
        HashMap<String, List<Way>> waysInLayer = new HashMap<>(layersMap.size());
        List<WayEntity> wayEntities = new ArrayList<>(wayResponses.length);
        for (WayResponse wayResponse : wayResponses) {
            List<Node> wayNodes = new ArrayList<>(wayResponse.refs.length);
            boolean nodeNotFound = false;
            for (long nodeId : wayResponse.refs) {
                Node node = nodeMap.get(nodeId);
                if (node == null) {
                    Log.w(TAG, "node not found: " + nodeId + ", skip way " + wayResponse.id);
                    nodeNotFound = true;
                    break;
                }
                wayNodes.add(node);
            }

            if (nodeNotFound) {
                continue;
            }

            Way way = new Way(wayResponse.id, wayNodes, wayResponse.tags);
            WayEntity wayEntity = new WayEntity(wayResponse.id, wayNodes, wayResponse.tags);
            wayEntities.add(wayEntity);

            for (String layerName : wayResponse.tags.keySet()) {
                waysInLayer.computeIfAbsent(layerName, k -> new ArrayList<>()).add(way);
            }
        }

        Future<?> rendering = THREAD_POOL_EXECUTOR.submit(() -> renderLayers(waysInLayer, tileId));
        Future<?> inserting = THREAD_POOL_EXECUTOR.submit(() -> config.dbDao.insertWaysAndTile(wayEntities, new TileEntity(tileId)));

        try {
            rendering.get();
            inserting.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
//        Log.d(TAG, "saved tileId = " + tileId);
    }

    public void draw() {
        boolean navDrawn = false;
        for (Layer layer : layersOrder) {
            if (!navDrawn && layer.hasTextSymbolizer()) {
                nav.draw();
                navDrawn = true;
            }
            layer.draw();
        }
    }
}
