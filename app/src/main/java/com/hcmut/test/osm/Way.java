package com.hcmut.test.osm;

import androidx.annotation.NonNull;

import com.hcmut.test.geometry.BoundBox;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.geometry.Polygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Way {
    public final long id;
    private PointList pointList = null;
    private Polygon polygon = null;
    public final HashMap<String, HashMap<String, String>> tags;
    public final ArrayList<Node> nodes;
    private static final HashMap<Long, Boolean> alreadyDrawnTextPointMap = new HashMap<>();
    private BoundBox boundBox = null;
    private boolean bboxTransformed = false;
    public Way() {
        nodes = new ArrayList<>();
        tags = new HashMap<>();
        id = -1;
    }

    public Way(float[] vertices) {
        nodes = new ArrayList<>();
        for (int i = 0; i < vertices.length; i += 3) {
            nodes.add(new Node(vertices[i], vertices[i + 1]));
        }
        tags = new HashMap<>();
        id = -1;
    }

    public Way(long id, List<Node> nodes, HashMap<String, HashMap<String, String>> tags) {
        this.nodes = new ArrayList<>(nodes);
        this.tags = tags;
        this.id = id;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        for (Node node : nodes) {
            minX = Math.min(minX, node.lon);
            minY = Math.min(minY, node.lat);
            maxX = Math.max(maxX, node.lon);
            maxY = Math.max(maxY, node.lat);
        }

        boundBox = new BoundBox(minX, minY, maxX, maxY);
    }

    public void setAlreadyDrawnTextPoint(boolean alreadyDrawnTextPoint) {
        alreadyDrawnTextPointMap.put(id, alreadyDrawnTextPoint);
    }

    public boolean hasAlreadyDrawnTextPoint() {
        return alreadyDrawnTextPointMap.containsKey(id) && Boolean.TRUE.equals(alreadyDrawnTextPointMap.get(id));
    }

    public boolean isClosed() {
        return nodes.get(0).equals(nodes.get(nodes.size() - 1));
    }

    private void transformBoundingBox(float originX, float originY, float scale) {
        if (bboxTransformed) {
            return;
        }
        float minX = (boundBox.minX - originX) * scale;
        float minY = (boundBox.minY - originY) * scale;
        float maxX = (boundBox.maxX - originX) * scale;
        float maxY = (boundBox.maxY - originY) * scale;

        boundBox = new BoundBox(minX, minY, maxX, maxY);
        bboxTransformed = true;
    }

    public PointList toPointList(float originX, float originY, float scale) {
        if (pointList == null) {
            List<Point> points = new ArrayList<>();
            for (Node node : nodes) {
                points.add(node.toPoint(originX, originY, scale));
            }
            pointList = new PointList(points);
            transformBoundingBox(originX, originY, scale);
        }
        return pointList;
    }

    public Polygon toPolygon(float originX, float originY, float scale) {
        if (polygon == null) {
            List<Point> points = new ArrayList<>();
            for (Node node : nodes) {
                points.add(node.toPoint(originX, originY, scale));
            }
            polygon = new Polygon(points);
            transformBoundingBox(originX, originY, scale);
        }
        return polygon;
    }

    public BoundBox getBoundBox() {
        return boundBox;
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
