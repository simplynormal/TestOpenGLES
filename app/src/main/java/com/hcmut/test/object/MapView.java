package com.hcmut.test.object;

import android.annotation.SuppressLint;
import android.util.Log;

import com.hcmut.test.mapnik.Layer;
import com.hcmut.test.osm.Node;
import com.hcmut.test.osm.Way;
import com.hcmut.test.remote.LayerResponse;
import com.hcmut.test.remote.NodeResponse;
import com.hcmut.test.remote.WayResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@SuppressLint("NewApi")
public class MapView {
    private static final String TAG = "MapView";
    private HashMap<Long, WayResponse> ways = new HashMap<>(0);
    private final HashMap<String, Layer> layersMap = new HashMap<>(78);
    private List<Layer> layersOrder;
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

        HashMap<Long, Node> nodeMap = new HashMap<>();
        for (NodeResponse nodeResponse : nodeResponses) {
            nodeMap.put(nodeResponse.id, new Node(nodeResponse.lon, nodeResponse.lat));
        }
        for (WayResponse wayResponse : wayResponses) {
            List<Node> wayNodes = new ArrayList<>();
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

            for (String layerName : wayResponse.tags.keySet()) {
                Layer curLayer = layersMap.get(layerName);
                if (curLayer == null) {
                    Log.e(TAG, "layer not found: " + layerName);
                    continue;
                }
                curLayer.addWay(way);
            }
        }

        for (Layer l : layersMap.values()) {
            l.save();
        }
    }

    public void draw() {
        for (Layer layer : layersOrder) {
            layer.draw();
        }
    }
}
