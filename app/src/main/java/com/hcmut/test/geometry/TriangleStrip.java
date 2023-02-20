package com.hcmut.test.geometry;

import java.util.ArrayList;
import java.util.List;

public class TriangleStrip {
    public final List<Point> points;

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
            this.points.add(points.get(points.size() - 1));
            this.points.add(strip.points.get(0));
        }
        this.points.addAll(strip.points);
        return this;
    }

    public float[] toVertexData() {
        float[] result = new float[points.size() * 3];
        for (int i = 0; i < points.size(); i++) {
            result[i * 3] = points.get(i).x;
            result[i * 3 + 1] = points.get(i).y;
            result[i * 3 + 2] = points.get(i).z;
        }
        return result;
    }
}
