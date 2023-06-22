package com.hcmut.admin.utrafficsystem.tbt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hcmut.admin.utrafficsystem.tbt.algorithm.TileSystem;
import com.hcmut.admin.utrafficsystem.tbt.local.AppDatabase;
import com.hcmut.admin.utrafficsystem.tbt.local.TileEntity;
import com.hcmut.admin.utrafficsystem.tbt.local.WayEntity;
import com.hcmut.admin.utrafficsystem.tbt.osm.Node;
import com.hcmut.admin.utrafficsystem.tbt.remote.BaseResponse;
import com.hcmut.admin.utrafficsystem.tbt.remote.Coord;
import com.hcmut.admin.utrafficsystem.tbt.remote.LayerRequest;
import com.hcmut.admin.utrafficsystem.tbt.remote.LayerResponse;
import com.hcmut.admin.utrafficsystem.tbt.remote.NodeResponse;
import com.hcmut.admin.utrafficsystem.tbt.remote.WayResponse;
import com.hcmut.admin.utrafficsystem.tbt.utils.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NavTest {
    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
            NUMBER_OF_CORES,
            NUMBER_OF_CORES,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());
    private static final String TAG = NavTest.class.getSimpleName();
    public static final double[] coords = {
            10.72946, 106.72807,
            10.72939, 106.7279,
            10.72936, 106.72777,
            10.72935, 106.72767,
            10.72932, 106.72759,
            10.72931, 106.72754,
            10.72929, 106.72746,
            10.72923, 106.72744,
            10.72922, 106.72753,
            10.72923, 106.72761,
            10.72927, 106.72775,
            10.72931, 106.72789,
            10.72933, 106.72798,
            10.72934, 106.72806,
            10.72937, 106.72817,
            10.7294, 106.72828,
            10.72943, 106.7284,
    };
    public static final List<Location> locations;
    public static double startLat = 10.727024752406857;
    public static double startLon = 106.72660142183304;
    public static double endLat = 10.768092710494013;
    public static double endLon = 106.64472229778767;

    static {
        locations = new ArrayList<>(coords.length / 2) {{
            for (int i = 0; i < coords.length; i += 2) {
                Location location = new Location("");
                location.setLatitude(coords[i]);
                location.setLongitude(coords[i + 1]);
                add(location);
            }
        }};

        // generate first bearing is bearing to the next point, then others are bearing from previous point to current point
        float bearing = locations.get(0).bearingTo(locations.get(1));
        locations.get(0).setBearing(bearing);
        for (int i = 1; i < locations.size(); i++) {
            bearing = locations.get(i - 1).bearingTo(locations.get(i));
            locations.get(i).setBearing(bearing);
        }
    }

    private static float getDistance(Location loc1, Location loc2) {
        return loc1.distanceTo(loc2);
    }

    public static Location getLatLon(double distance, int step, List<Coord> route) {
        float totalDistance = 0.0f;
        float[] cumDistances = new float[route.size()];
        for (int i = 0; i < route.size() - 1; i++) {
            Location loc1 = new Location("");
            loc1.setLatitude(route.get(i).getLat());
            loc1.setLongitude(route.get(i).getLng());

            Location loc2 = new Location("");
            loc2.setLatitude(route.get(i + 1).geteLat());
            loc2.setLongitude(route.get(i + 1).geteLng());

            float d = getDistance(loc1, loc2);
            totalDistance += d;
            cumDistances[i + 1] = totalDistance;
        }
        double stepDistance = step * distance;

        if (stepDistance - distance > totalDistance) return null;
        if (stepDistance > totalDistance) return new Location("") {{
            setLatitude(route.get(route.size() - 2).geteLat());
            setLongitude(route.get(route.size() - 2).geteLng());
        }};

        int segmentIndex = 0;
        for (; segmentIndex < cumDistances.length; segmentIndex++) {
            if (cumDistances[segmentIndex] > stepDistance) break;
        }

        float prevSegDist = cumDistances[segmentIndex - 1];
        double ratio = (stepDistance - prevSegDist) / (cumDistances[segmentIndex] - prevSegDist);
        Coord segStart = route.get(segmentIndex - 1);
        Coord segEnd = route.get(segmentIndex);

        double lat = segStart.getLat() + ratio * (segEnd.geteLat() - segStart.getLat());
        double lon = segStart.getLng() + ratio * (segEnd.geteLng() - segStart.getLng());

        return new Location("") {{
            setLatitude(lat);
            setLongitude(lon);
        }};
    }

    public static long getDatabaseSizeInBytes(Context context, String databaseName) {
        try {
            SQLiteDatabase database = SQLiteDatabase.openDatabase(
                    context.getDatabasePath(databaseName).getPath(),
                    null,
                    SQLiteDatabase.OPEN_READONLY);
            File databaseFile = new File(database.getPath());
            long size = databaseFile.length();
            database.close();
            return size;
        } catch (Exception e) {
            // Handle exception
            e.printStackTrace();
            return -1;
        }
    }

    public static void test(Config config) {
        Future<?> future = THREAD_POOL_EXECUTOR.submit(config.dbDao::clearAll);

        try {
            future.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File sourceFile = new File(documentsDir, "output.txt");
        String filePath = sourceFile.getAbsolutePath();

        double topLeftLat = 10.8974;
        double topLeftLon = 106.5605;
        double botRightLat = 10.6690;
        double botRightLon = 106.8022;

        long topLeftId = TileSystem.getTileId(topLeftLon, topLeftLat);
        long botRightId = TileSystem.getTileId(botRightLon, botRightLat);

        long fromX = TileSystem.getTileX(topLeftId);
        long fromY = TileSystem.getTileY(topLeftId);
        long toX = TileSystem.getTileX(botRightId);
        long toY = TileSystem.getTileY(botRightId);

        StringBuilder outputData = new StringBuilder();
        AtomicLong count = new AtomicLong(0);
        long total = (toX - fromX + 1) * (toY - fromY + 1);
        for (long x = fromX; x <= toX; x++) {
            for (long y = fromY; y <= toY; y++) {
                long tileId = TileSystem.toTileId(x, y);

                float[] bbox = TileSystem.getBoundBox(tileId);
                LayerRequest layerRequest = new LayerRequest(bbox);
                layerRequest.post(new Callback<>() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onResponse(@NonNull Call<BaseResponse<LayerResponse>> call, @NonNull Response<BaseResponse<LayerResponse>> response) {
                        if (response.body() == null || response.raw().body() == null) {
                            Log.e(TAG, "response.body() == null");
                            return;
                        }

//                        Log.d(TAG, "Fetching tile " + tileId + " success");

                        LayerResponse layerResponse = response.body().getData();
                        NodeResponse[] nodeResponses = layerResponse.nodes;
                        WayResponse[] wayResponses = layerResponse.ways;

                        HashMap<Long, Node> nodeMap = new HashMap<>(nodeResponses.length);
                        for (NodeResponse nodeResponse : nodeResponses) {
                            nodeMap.put(nodeResponse.id, new Node(nodeResponse.lon, nodeResponse.lat));
                        }
                        List<WayEntity> wayEntities = new ArrayList<>(wayResponses.length);
                        for (WayResponse wayResponse : wayResponses) {
                            List<Node> wayNodes = new ArrayList<>(wayResponse.refs.length);
                            boolean nodeNotFound = false;
                            for (long nodeId : wayResponse.refs) {
                                Node node = nodeMap.get(nodeId);
                                if (node == null) {
                                    nodeNotFound = true;
                                    break;
                                }
                                wayNodes.add(node);
                            }

                            if (nodeNotFound) {
                                continue;
                            }

                            WayEntity wayEntity = new WayEntity(wayResponse.id, wayNodes, wayResponse.tags);
                            wayEntities.add(wayEntity);
                        }
                        AtomicReference<String> cur = new AtomicReference<>("");
                        Future<?> future = THREAD_POOL_EXECUTOR.submit(() -> {
                            long sizeAfterClear = getDatabaseSizeInBytes(config.context, AppDatabase.DATABASE_NAME);
                            config.dbDao.insertWaysAndTile(wayEntities, new TileEntity(tileId));
                            long sizeAfterInsert = getDatabaseSizeInBytes(config.context, AppDatabase.DATABASE_NAME);

                            cur.set("Tile " + tileId + ": " + sizeAfterClear + " -> " + sizeAfterInsert + " = " + (sizeAfterInsert - sizeAfterClear));

//                            Log.v(TAG, cur);
                            outputData.append(cur.get()).append("\n");
                        });

                        try {
                            future.get();
                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }

                        long completed = count.incrementAndGet();
                        Log.d(TAG, "Progress: (" + completed + "/" + total + ")" + cur.get());

                        boolean isDone = completed == total;
                        if (isDone) {
                            Log.d(TAG, "Writing data to the file");
                            try {
                                FileWriter fileWriter = new FileWriter(filePath);
                                fileWriter.write(outputData.toString());
                                fileWriter.flush();
                                fileWriter.close();
                                Log.d(TAG, "Data written to the file: " + filePath);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<BaseResponse<LayerResponse>> call, @NonNull Throwable t) {
                        Log.e(TAG, "onFailure: " + t.getMessage());
                        long completed = count.incrementAndGet();

                        boolean isDone = completed == total;
                        if (isDone) {
                            Log.d(TAG, "Writing data to the file");
                            try {
                                FileWriter fileWriter = new FileWriter(filePath);
                                fileWriter.write(outputData.toString());
                                fileWriter.flush();
                                fileWriter.close();
                                Log.d(TAG, "Data written to the file: " + filePath);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        }
    }
}
