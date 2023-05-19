package com.hcmut.test.object;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import com.hcmut.test.algorithm.TileSystem;
import com.hcmut.test.geometry.BoundBox;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.local.DbDao;
import com.hcmut.test.local.WayEntity;
import com.hcmut.test.mapnik.Layer;
import com.hcmut.test.osm.Node;
import com.hcmut.test.osm.Way;
import com.hcmut.test.remote.BaseResponse;
import com.hcmut.test.remote.LayerRequest;
import com.hcmut.test.remote.LayerResponse;
import com.hcmut.test.remote.NodeResponse;
import com.hcmut.test.remote.WayResponse;
import com.hcmut.test.utils.Config;
import com.hcmut.test.utils.Debounce;

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
    private static final int[][] OFFSETS = new int[][]{
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1}, {0, 0}, {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };
    private final HashMap<String, Layer> layersMap = new HashMap<>(78);
    private List<Layer> layersOrder;
    private final Config config;
    private final Nav nav;
    private HashSet<Long> curWayIds = new HashSet<>(0);
    private final Debounce debounce = new Debounce(100);

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

    public void setCurLocation(float lon, float lat, float radius) {
        debounce.debounce(() -> {
            Point curPoint = new Point(lon, lat);
            long tileId = TileSystem.getTileId(lon, lat);
            HashSet<Long> possibleTiles = new HashSet<>(9);

            for (int[] offset : OFFSETS) {
                long id = TileSystem.getTileId(tileId, offset[0], offset[1]);
                float[] bbox = TileSystem.getBoundBox(id);
                Point bboxMin = new Point(bbox[0], bbox[1]);
                Point bboxMax = new Point(bbox[2], bbox[3]);
                Point bboxCenter = bboxMin.midPoint(bboxMax);
                float bboxRadius = bboxCenter.distance(bboxMax);

                if (curPoint.distance(bboxCenter) < radius * 1.5 + bboxRadius) {
                    possibleTiles.add(id);
                }
            }


            request(possibleTiles);
        });
    }

    void request(HashSet<Long> possibleTiles) {
        if (possibleTiles == null || possibleTiles.size() == 0) {
            return;
        }

        HashSet<Long> diff = new HashSet<>(possibleTiles);
        diff.removeAll(curWayIds);

        Log.d(TAG, "diff.size(): " + diff.size());

        if (diff.size() == 0) {
            return;
        }

        List<Long> missingTiles = requestLocal(diff);
        CompletableFuture<?> fromAPI = requestAPI(missingTiles);


        HashSet<Long> removeTiles = new HashSet<>(curWayIds);
        removeTiles.removeAll(possibleTiles);
        curWayIds = possibleTiles;


        fromAPI.thenRun(() -> {
            Log.d(TAG, "possibleTiles: " + possibleTiles);
            Log.d(TAG, "removeTiles: " + removeTiles);
            for (Layer layer : layersOrder) {
                THREAD_POOL_EXECUTOR.execute(() -> {
                    layer.removeWays(removeTiles);
                    layer.save();
                });
            }
        });
    }

    private List<Long> requestLocal(HashSet<Long> tiles) {
        DbDao dbDao = config.dbDao;
        List<Long> localTiles = dbDao.getAllTileIds(new ArrayList<>(tiles));
        if (localTiles.size() == 0) {
            return new ArrayList<>(tiles);
        }

        List<Long> missingTiles = new ArrayList<>(tiles.size() - localTiles.size());
        HashMap<String, HashMap<Long, List<Way>>> waysInLayerInTiles = new HashMap<>(layersMap.size());

        for (long tileId : localTiles) {
            if (tiles.contains(tileId)) {
                List<Way> ways = dbDao.getWaysByTileId(tileId).stream().map(WayEntity::toWay).collect(Collectors.toList());
                ways.forEach((way) -> {
                    for (String layerName : way.tags.keySet()) {
                        waysInLayerInTiles.computeIfAbsent(layerName, k -> new HashMap<>(localTiles.size())).computeIfAbsent(tileId, k -> new ArrayList<>()).add(way);
                    }
                });
            } else {
                missingTiles.add(tileId);
            }
        }

        List<Future<?>> futures = new ArrayList<>(waysInLayerInTiles.size());

        for (String layerName : waysInLayerInTiles.keySet()) {
            Layer curLayer = layersMap.get(layerName);
            HashMap<Long, List<Way>> waysInTiles = waysInLayerInTiles.get(layerName);
            if (curLayer == null || waysInTiles == null) {
                Log.e(TAG, "layer not found: " + layerName + " or waysInTiles == null");
                continue;
            }
            Future<?> curFuture = THREAD_POOL_EXECUTOR.submit(() -> {
                for (long tileId : waysInTiles.keySet()) {
                    List<Way> ways = waysInTiles.get(tileId);
                    if (ways == null) {
                        Log.e(TAG, "ways == null in " + layerName + " for tileId = " + tileId);
                        continue;
                    }
                    curLayer.addWays(ways, tileId);
                }
            });
            futures.add(curFuture);
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        for (String layerName : waysInLayerInTiles.keySet()) {
            Layer curLayer = layersMap.get(layerName);
            if (curLayer == null) {
                Log.e(TAG, "layer not found: " + layerName);
                continue;
            }
            curLayer.save();
        }


        return missingTiles;
    }

    private CompletableFuture<?> requestAPI(List<Long> missingTiles) {
        List<CompletableFuture<?>> futures = new ArrayList<>(missingTiles.size());
        CompletableFuture<?> rv = new CompletableFuture<>();
        AtomicInteger counter = new AtomicInteger(missingTiles.size());

        for (int i = 0; i < missingTiles.size(); i++) {
            long tileId = missingTiles.get(i);

            float[] bbox = TileSystem.getBoundBox(tileId);
            LayerRequest layerRequest = new LayerRequest(bbox);
            int finalI = i;
            layerRequest.post(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BaseResponse<LayerResponse>> call, @NonNull Response<BaseResponse<LayerResponse>> response) {
                    if (response.body() == null) {
                        Log.e(TAG, "response.body() == null");
                        return;
                    }

                    CompletableFuture<?> prevFuture = futures.size() > 0 ? futures.get(futures.size() - 1) : null;
                    CompletableFuture<?> curFuture = new CompletableFuture<>();
                    futures.add(curFuture);

                    LayerResponse layerResponse = response.body().getData();
//                    Log.d(TAG, "layerResponse = " + layerResponse + " prevFuture = " + (prevFuture == null ? null : prevFuture.isDone()) + " at index = " + finalI);

                    THREAD_POOL_EXECUTOR.execute(() -> {
                        try {
                            if (prevFuture != null) prevFuture.get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
//                        Log.d(TAG, "validating layerResponse: " + tileId);
                        validateResponse(curFuture, layerResponse, tileId);
                        try {
                            curFuture.get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                        if (counter.decrementAndGet() == 0) {
                            rv.complete(null);
                        }
                    });
                }

                @Override
                public void onFailure(@NonNull Call<BaseResponse<LayerResponse>> call, @NonNull Throwable t) {
                    Log.e(TAG, "onFailure: " + t.getMessage());
                }
            });
        }

        return rv;
    }

    public void validateResponse(CompletableFuture<?> completableFuture, LayerResponse layer, long tileId) {
        NodeResponse[] nodeResponses = layer.nodes;
        WayResponse[] wayResponses = layer.ways;

        HashMap<Long, Node> nodeMap = new HashMap<>(nodeResponses.length);
        for (NodeResponse nodeResponse : nodeResponses) {
            nodeMap.put(nodeResponse.id, new Node(nodeResponse.lon, nodeResponse.lat));
        }
        HashMap<String, List<Way>> waysInLayer = new HashMap<>(layersMap.size());
        List<WayEntity> wayEntities = new ArrayList<>();
        for (WayResponse wayResponse : wayResponses) {
            List<Node> wayNodes = new ArrayList<>(wayResponse.refs.length);
            boolean nodeNotFound = false;
            for (long nodeId : wayResponse.refs) {
                Node node = nodeMap.get(nodeId);
                if (node == null) {
                    Log.e(TAG, "node not found: " + nodeId + ", skip way " + wayResponse.id);
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

        List<Future<?>> futures = new ArrayList<>(waysInLayer.size());

        for (String layerName : waysInLayer.keySet()) {
            Layer curLayer = layersMap.get(layerName);
            List<Way> ways = waysInLayer.get(layerName);
            if (curLayer == null || ways == null) {
                Log.e(TAG, "layer not found: " + layerName);
                continue;
            }

            Future<?> curFuture = THREAD_POOL_EXECUTOR.submit(() -> curLayer.addWays(ways, tileId));

            futures.add(curFuture);
        }

        THREAD_POOL_EXECUTOR.execute(() -> {
//            Log.d(TAG, "waiting for futures.size() = " + futures.size() + ", tileId = " + tileId);
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

//            Log.d(TAG, "saving tileId = " + tileId);
            for (String layerName : waysInLayer.keySet()) {
                Layer curLayer = layersMap.get(layerName);
                if (curLayer == null) {
                    Log.e(TAG, "layer not found: " + layerName);
                    continue;
                }
                curLayer.save();
            }
            completableFuture.complete(null);
        });
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
