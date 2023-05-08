package com.hcmut.test.osm;

import androidx.annotation.NonNull;

import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.geometry.Polygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Way {
    private PointList pointList = null;
    private Polygon polygon = null;
    public final HashMap<String, String> tags;
    public final ArrayList<Node> nodes;
    private boolean alreadyDrawnTextPoint = false;

    public Way() {
        nodes = new ArrayList<>();
        tags = new HashMap<>();
    }

    public Way(float[] vertices) {
        nodes = new ArrayList<>();
        for (int i = 0; i < vertices.length; i += 3) {
            nodes.add(new Node(vertices[i], vertices[i + 1]));
        }
        tags = new HashMap<>();
    }

    public Way(List<Node> nodes) {
        this.nodes = new ArrayList<>(nodes);
        tags = new HashMap<>();
    }

    public Way(List<Node> nodes, HashMap<String, String> tags) {
        this.nodes = new ArrayList<>(nodes);
        this.tags = tags;
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public void setAlreadyDrawnTextPoint(boolean alreadyDrawnTextPoint) {
        this.alreadyDrawnTextPoint = alreadyDrawnTextPoint;
    }

    public boolean isAlreadyDrawnTextPoint() {
        return alreadyDrawnTextPoint;
    }

    public void wrapUpNodes() {
        if (!isClosed()) return;
        try {
            float wayPixels = new Polygon(toPoints()).getArea();
            tags.put("way_pixels", String.valueOf(wayPixels));
        } catch (Exception e) {
            System.out.println("Error when calculating area:");
            System.out.println(this);
            e.printStackTrace();
        }
    }

    public boolean isClosed() {
        return nodes.get(0).equals(nodes.get(nodes.size() - 1));
    }

    public PointList toPointList(float originX, float originY, float scale) {
        if (pointList == null) {
            List<Point> points = new ArrayList<>();
            for (Node node : nodes) {
                points.add(node.toPoint(originX, originY, scale));
            }
            pointList = new PointList(points);
        }
        return pointList;
    }

    public Polygon toPolygon(float originX, float originY, float scale) {
        if (polygon == null) {
            polygon = new Polygon(toPointList(originX, originY, scale));
        }
        return polygon;
    }


    public List<Point> toPoints() {
        List<Point> points = new ArrayList<>();
        for (Node node : nodes) {
            points.add(node.toPoint());
        }
        return points;
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
