package com.hcmut.test.data;

import android.os.Build;

import androidx.annotation.NonNull;

import com.hcmut.test.geometry.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Way {
    public final ArrayList<Node> nodes;
    public final HashMap<String, String> tags = new HashMap<>();

    public Way() {
        nodes = new ArrayList<>();
    }

    public Way(ArrayList<Node> nodes) {
        this.nodes = nodes;
    }

    public Way(float[] vertices) {
        nodes = new ArrayList<>();
        for (int i = 0; i < vertices.length; i += 3) {
            nodes.add(new Node(vertices[i], vertices[i + 1]));
        }
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public boolean isClosed() {
        return nodes.get(0).equals(nodes.get(nodes.size() - 1));
    }

    public List<Point> toPoints() {
        List<Point> points = new ArrayList<>();
        for (Node node : nodes) {
            points.add(new Point(node.lon, node.lat));
        }
        return points;
    }

    public List<Point> toPoints(float originX, float originY, float scale) {
        List<Point> points = new ArrayList<>();
        for (Node node : nodes) {
            points.add(new Point((node.lon - originX) * scale, (node.lat - originY) * scale));
        }
        return points;
    }

    public List<Point> toPoints(float originX, float originY, float scale, float z) {
        List<Point> points = new ArrayList<>();
        for (Node node : nodes) {
            points.add(new Point((node.lon - originX) * scale, (node.lat - originY) * scale, z));
        }
        return points;
    }

    public void addTag(String key, String value) {
        tags.put(key, value);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Way{");
        for (Node node : nodes) {
            sb.append("\n\t");
            sb.append(node.toString());
            sb.append(", ");
        }
        sb.append("\n}");
        return sb.toString();
    }
}
