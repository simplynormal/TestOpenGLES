package com.hcmut.test.geometry;

import com.hcmut.test.data.Node;
import com.hcmut.test.data.Way;

import java.util.ArrayList;
import java.util.List;

public class LineStrip {
    public final List<Point> points;

    public LineStrip(List<Point> points) {
        this.points = points;
    }

    public LineStrip(Point[] points) {
        this.points = List.of(points);
    }

    public LineStrip(Way way) {
        this.points = new ArrayList<>();
        for (Node node : way.nodes) {
            this.points.add(new Point(node.lon, node.lat));
        }
    }

    public LineStrip(float[] points) {
        this.points = new ArrayList<>();
        for (int i = 0; i < points.length; i += 3) {
            this.points.add(new Point(points[i], points[i + 1], points[i + 2]));
        }
    }
}
