package com.hcmut.test.object;

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

public class MapView {
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
//            if (wayResponse.id != 165277514) continue;
            List<Node> wayNodes = new ArrayList<>();
            for (long nodeId : wayResponse.refs) {
                Node node = nodeMap.get(nodeId);
                if (node == null) {
                    Log.e("StyleParser", "node not found: " + nodeId);
                    continue;
                }
                wayNodes.add(node);
            }

            for (String layerName : wayResponse.tags.keySet()) {
                Layer curLayer = layersMap.get(layerName);
                if (curLayer == null) {
                    Log.e("StyleParser", "layer not found: " + layerName);
                    continue;
                }
                Way way = new Way(wayNodes, wayResponse.tags.get(layerName));
                curLayer.addWay(wayResponse.id, way);
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
