package com.hcmut.test.osm;

import androidx.annotation.NonNull;

import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.geometry.Polygon;

import java.util.ArrayList;
import java.util.List;

public class Way extends Element {
    private Polygon polygon = null;
    public final ArrayList<Node> nodes;

    public Way() {
        nodes = new ArrayList<>();
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

    public void wrapUpNodes() {
        if (!isClosed()) return;
        polygon = new Polygon(toPoints());
        try {
            float wayPixels = polygon.getArea();
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

    public List<Point> toPoints() {
        List<Point> points = new ArrayList<>();
        for (Node node : nodes) {
            points.add(node.toPoint());
        }
        return points;
    }

    public List<Point> toPoints(float scale) {
        List<Point> points = new ArrayList<>();
        for (Node node : nodes) {
            points.add(node.toPoint(scale));
        }
        return points;
    }

    public List<Point> toPoints(float originX, float originY, float scale) {
        List<Point> points = new ArrayList<>();
        for (Node node : nodes) {
            points.add(node.toPoint(originX, originY, scale));
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

    public PointList toPointList(float scale) {
        if (polygon != null) return polygon.scale(scale);
        List<Point> points = new ArrayList<>();
        for (Node node : nodes) {
            points.add(node.toPoint(scale));
        }
        return new PointList(points);
    }

    public PointList toPointList(float originX, float originY, float scale) {
        if (polygon != null) return polygon.transform(originX, originY, scale);
        List<Point> points = new ArrayList<>();
        for (Node node : nodes) {
            points.add(node.toPoint(originX, originY, scale));
        }
        return new PointList(points);
    }

    @Override
    public List<PointList> toPointLists() {
        return toPointLists(0, 0, 1);
    }

    @Override
    public List<PointList> toPointLists(float scale) {
        return toPointLists(0, 0, scale);
    }

    @Override
    public List<PointList> toPointLists(float originX, float originY, float scale) {
        List<PointList> pointLists = new ArrayList<>(1);
        pointLists.add(toPointList(originX, originY, scale));
        return pointLists;
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
