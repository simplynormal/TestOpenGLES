package com.hcmut.test.geometry;

import com.hcmut.test.data.VertexData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TriangleStrip {
    public final ArrayList<Point> points;
    private final List<Integer> indices = new ArrayList<>();

    public TriangleStrip(List<Point> points) {
        assert points.size() >= 3: "Triangle strip must have at least 3 points";
        this.points = new ArrayList<>(points);
    }

    public TriangleStrip(Point[] points) {
        this(Arrays.asList(points));
    }

    public TriangleStrip(Point p1, Point p2, Point p3) {
        this(List.of(p1, p2, p3));
    }

    public TriangleStrip(float[] points) {
        this(new ArrayList<>() {
            {
                for (int i = 0; i < points.length; i += 3) {
                    add(new Point(points[i], points[i + 1], points[i + 2]));
                }
            }
        });
    }

    public TriangleStrip(TriangleStrip strip) {
        this(strip.points);
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

    public void extend(TriangleStrip strip) {
        if (points.size() > 0) {
            indices.add(points.size() - 1);
            this.points.add(points.get(points.size() - 1));
            this.points.add(strip.points.get(0));
        }
        this.points.addAll(strip.points);
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

    public static float[] toVertexData(List<TriangleStrip> strips) {
        if (strips.size() == 0) return new float[0];
        TriangleStrip strip = new TriangleStrip(strips.get(0).points);
        for (int i = 1; i < strips.size(); i++) {
            strip.extend(strips.get(i));
        }
        return strip.toVertexData();
    }

    public static float[] toVertexData(List<TriangleStrip> strips, float r, float g, float b, float a) {
        if (strips.size() == 0) return new float[0];
        TriangleStrip strip = new TriangleStrip(strips.get(0));
        for (int i = 1; i < strips.size(); i++) {
            strip.extend(strips.get(i));
        }
        return strip.toVertexData(r, g, b, a);
    }
}
