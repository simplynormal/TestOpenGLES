package com.hcmut.test.object;

import android.opengl.GLES20;
import android.os.Build;

import androidx.annotation.NonNull;

import com.hcmut.test.data.Node;
import com.hcmut.test.data.Triangle;

import java.util.ArrayList;

public class Way {
    public final ArrayList<Node> nodes;

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

    public boolean isClockwise() {
        float area = 0;
        for (int i = 0; i < nodes.size() - 1; i++) {
            area += (nodes.get(i + 1).lat - nodes.get(i).lat) * (nodes.get(i + 1).lon + nodes.get(i).lon);
        }
        return area < 0;
    }

    public void sortClockwise() {
        if (isClockwise()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            nodes.sort((o1, o2) -> {
                float angle1 = (float) Math.atan2(o1.lat - nodes.get(0).lat, o1.lon - nodes.get(0).lon);
                float angle2 = (float) Math.atan2(o2.lat - nodes.get(0).lat, o2.lon - nodes.get(0).lon);
                return Float.compare(angle1, angle2);
            });
        } else {
            for (int i = 0; i < nodes.size() - 1; i++) {
                for (int j = i + 1; j < nodes.size(); j++) {
                    float angle1 = (float) Math.atan2(nodes.get(i).lat - nodes.get(0).lat, nodes.get(i).lon - nodes.get(0).lon);
                    float angle2 = (float) Math.atan2(nodes.get(j).lat - nodes.get(0).lat, nodes.get(j).lon - nodes.get(0).lon);
                    if (angle1 > angle2) {
                        Node temp = nodes.get(i);
                        nodes.set(i, nodes.get(j));
                        nodes.set(j, temp);
                    }
                }
            }
        }
    }

    public Node getCenter() {
        float lat = 0;
        float lon = 0;
        for (Node node : nodes) {
            lat += node.lat;
            lon += node.lon;
        }
        lat /= nodes.size();
        lon /= nodes.size();
        return new Node(lon, lat);
    }

    public boolean isConvex() {
        if (nodes.size() < 3) return false;
        for (int i = 0; i < nodes.size(); i++) {
            Node p1 = nodes.get((i - 1 + nodes.size()) % nodes.size());
            Node p2 = nodes.get(i);
            Node p3 = nodes.get((i + 1) % nodes.size());
            float angle = (float) Math.atan2(p2.lat - p1.lat, p2.lon - p1.lon) - (float) Math.atan2(p3.lat - p2.lat, p3.lon - p2.lon);
            if (angle < 0) angle += 2 * Math.PI;
            if (angle > Math.PI) return false;
        }
        return true;
    }

    private boolean isPointInTriangle(Node p, Node p1, Node p2, Node p3) {
        float a = (float) Math.abs(0.5 * (p1.lon * p2.lat - p1.lat * p2.lon + (p2.lat - p1.lat) * p.lon + (p1.lon - p2.lon) * p.lat));
        float b = (float) Math.abs(0.5 * (p2.lon * p3.lat - p2.lat * p3.lon + (p3.lat - p2.lat) * p.lon + (p2.lon - p3.lon) * p.lat));
        float c = (float) Math.abs(0.5 * (p3.lon * p1.lat - p3.lat * p1.lon + (p1.lat - p3.lat) * p.lon + (p3.lon - p1.lon) * p.lat));
        float s = (float) Math.abs(0.5 * (p1.lon * p2.lat - p1.lat * p2.lon + p1.lat * (p3.lon - p2.lon) + p2.lat * (p1.lon - p3.lon) + p3.lat * (p2.lon - p1.lon)));
        return Math.abs(a + b + c - s) < 0.0001;
    }

    public float[] getDrawableConvexVertices() {
//        sortClockwise();
        float[] vertices = new float[(nodes.size() + 1) * 3];
        for (int i = 0; i < nodes.size(); i++) {
            vertices[i * 3] = nodes.get(i).lon;
            vertices[i * 3 + 1] = nodes.get(i).lat;
            vertices[i * 3 + 2] = 0;
        }
        vertices[vertices.length - 3] = nodes.get(0).lon;
        vertices[vertices.length - 2] = nodes.get(0).lat;
        vertices[vertices.length - 1] = 0;
        return vertices;
    }

    public float[] getDrawableConvexVertices(float originX, float originY, float scale) {
//        sortClockwise();
        float[] vertices = new float[(nodes.size() + 2) * 3];
        Node center = getCenter();
        vertices[0] = (center.lon - originX) * scale;
        vertices[1] = (center.lat - originY) * scale;
        vertices[2] = 0;
        for (int i = 0; i < nodes.size(); i++) {
            vertices[(i + 1) * 3] = (nodes.get(i).lon - originX) * scale;
            vertices[(i + 1) * 3 + 1] = (nodes.get(i).lat - originY) * scale;
            vertices[(i + 1) * 3 + 2] = 0;
        }
        vertices[vertices.length - 3] = (nodes.get(0).lon - originX) * scale;
        vertices[vertices.length - 2] = (nodes.get(0).lat - originY) * scale;
        vertices[vertices.length - 1] = 0;
        return vertices;
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
