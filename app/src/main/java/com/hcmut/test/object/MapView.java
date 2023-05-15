package com.hcmut.test.object;

import android.annotation.SuppressLint;
import android.util.Log;

import com.hcmut.test.algorithm.TileDivision;
import com.hcmut.test.local.WayEntity;
import com.hcmut.test.mapnik.Layer;
import com.hcmut.test.osm.Node;
import com.hcmut.test.osm.Way;
import com.hcmut.test.remote.LayerResponse;
import com.hcmut.test.remote.NodeResponse;
import com.hcmut.test.remote.RetrofitClient;
import com.hcmut.test.remote.WayResponse;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SuppressLint("NewApi")
public class MapView {
    private static final String TAG = "MapView";
    private HashMap<Long, WayResponse> ways = new HashMap<>(0);
    private final HashMap<String, Layer> layersMap = new HashMap<>(78);
    private List<Layer> layersOrder;
    private final Config config;

    public MapView(Config config) {
        this.config = config;
    }

    public void setLayers(List<Layer> layers) {
        layersOrder = layers;
        for (Layer layer : layers) {
            layersMap.put(layer.name, layer);
        }
    }

    private void parseWays(WayResponse[] wayResponses) {
        ways = new HashMap<>(wayResponses.length) {
            {
                for (WayResponse wayResponse : wayResponses) {
                    put(wayResponse.id, wayResponse);
                }
            }
        };
    }

    public void validateResponse(LayerResponse layer) {
        NodeResponse[] nodeResponses = layer.nodes;
        WayResponse[] wayResponses = layer.ways;
        parseWays(wayResponses);

        HashMap<Long, Node> nodeMap = new HashMap<>(nodeResponses.length);
        HashMap<Long, Integer> nodeTileMap = new HashMap<>(nodeResponses.length);
        for (NodeResponse nodeResponse : nodeResponses) {
            nodeMap.put(nodeResponse.id, new Node(nodeResponse.lon, nodeResponse.lat));
//            nodeTileMap.put(nodeResponse.id, TileDivision.getTileId(nodeResponse.lat, nodeResponse.lon));
        }
        HashMap<String, List<Way>> waysInLayer = new HashMap<>(layersMap.size());
        List<WayEntity> wayEntities = new ArrayList<>();
        for (WayResponse wayResponse : wayResponses) {
            List<Node> wayNodes = new ArrayList<>(wayResponse.refs.length);
            Set<Integer> tileIds = new HashSet<>(wayResponse.refs.length);
            boolean nodeNotFound = false;
            for (long nodeId : wayResponse.refs) {
                Node node = nodeMap.get(nodeId);
//                int tileId = nodeTileMap.get(nodeId);
                if (node == null) {
                    Log.e(TAG, "node not found: " + nodeId + ", skip way " + wayResponse.id);
                    nodeNotFound = true;
                    break;
                }
                wayNodes.add(node);
//                tileIds.add(tileId);
            }

            if (nodeNotFound) {
                continue;
            }

            Way way = new Way(wayResponse.id, wayNodes, wayResponse.tags);
            WayEntity wayEntity = new WayEntity(wayResponse.id, wayNodes, wayResponse.tags, tileIds);
            wayEntities.add(wayEntity);

            for (String layerName : wayResponse.tags.keySet()) {
                waysInLayer.computeIfAbsent(layerName, k -> new ArrayList<>()).add(way);
            }
        }

        for (String layerName : waysInLayer.keySet()) {
            Layer curLayer = layersMap.get(layerName);
            List<Way> ways = waysInLayer.get(layerName);
            if (curLayer == null || ways == null) {
                Log.e(TAG, "layer not found: " + layerName);
                continue;
            }
            RetrofitClient.THREAD_POOL_EXECUTOR.execute(() -> {
                curLayer.addWays(ways);
                curLayer.save();
            });
        }

//        RetrofitClient.THREAD_POOL_EXECUTOR.execute(() -> config.wayDao.insertAll(wayEntities));
    }

    public void draw() {
        for (Layer layer : layersOrder) {
            layer.draw();
        }
    }
}
