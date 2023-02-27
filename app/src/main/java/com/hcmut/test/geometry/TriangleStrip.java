package com.hcmut.test.geometry;

import com.hcmut.test.data.VertexData;

import java.util.ArrayList;
import java.util.List;

public class TriangleStrip {
    public final List<Point> points;
    private final List<Integer> indices = new ArrayList<>();

    public TriangleStrip() {
        this.points = new ArrayList<>();
    }

    public TriangleStrip(List<Point> points) {
        this.points = points;
    }

    public TriangleStrip(Point[] points) {
        this.points = List.of(points);
    }

    public TriangleStrip(Point p1, Point p2, Point p3) {
        this.points = List.of(p1, p2, p3);
    }

    public TriangleStrip add(Point p) {
        this.points.add(p);
        return this;
    }

    public TriangleStrip add(Point[] points) {
        this.points.addAll(List.of(points));
        return this;
    }

    public TriangleStrip add(List<Point> points) {
        this.points.addAll(points);
        return this;
    }

    public TriangleStrip extend(TriangleStrip strip) {
        if (points.size() > 0) {
            indices.add(points.size() - 1);
            this.points.add(points.get(points.size() - 1));
            this.points.add(strip.points.get(0));
        }
        this.points.addAll(strip.points);
        return this;
    }

    public float[] toVertexData() {
        float[] rv = new float[0];
        for (int i = 0; i < indices.size(); i++) {
            int prevIndex = i == 0 ? 0 : indices.get(i - 1);
            float[] data = VertexData.toVertexData(points.subList(prevIndex, indices.get(i)), true, true);
            float[] newData = new float[rv.length + data.length];
            System.arraycopy(rv, 0, newData, 0, rv.length);
            System.arraycopy(data, 0, newData, rv.length, data.length);
            rv = newData;
        }

        float[] data = VertexData.toVertexData(points.subList(indices.size() == 0 ? 0 : indices.get(indices.size() - 1), points.size()), true, true);
        float[] newData = new float[rv.length + data.length];
        System.arraycopy(rv, 0, newData, 0, rv.length);
        System.arraycopy(data, 0, newData, rv.length, data.length);
        rv = newData;

        return rv;
    }

//    public float[] toVertexData() {
//        return VertexData.toVertexData(points, true, false);
//    }

    public float[] toVertexData(float r, float g, float b, float a) {
        return VertexData.toVertexData(points, r, g, b, a);
    }
}
