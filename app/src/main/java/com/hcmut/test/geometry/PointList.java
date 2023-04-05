package com.hcmut.test.geometry;

import java.util.ArrayList;
import java.util.List;

public abstract class PointList {
    public final List<Point> points;

    protected PointList(List<Point> points) {
        this.points = new ArrayList<>(points);
    }
}
