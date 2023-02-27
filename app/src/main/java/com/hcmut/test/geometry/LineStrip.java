package com.hcmut.test.geometry;

import java.util.ArrayList;
import java.util.List;

public class LineStrip {
    public final List<Point> points;

    public LineStrip() {
        this.points = new ArrayList<>();
    }

    public LineStrip(List<Point> points) {
        this.points = points;
    }

    public LineStrip(Point[] points) {
        this.points = List.of(points);
    }
}
