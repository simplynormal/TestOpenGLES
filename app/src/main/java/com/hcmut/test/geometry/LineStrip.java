package com.hcmut.test.geometry;

import com.hcmut.test.data.VertexData;
import com.hcmut.test.data.Way;

import java.util.ArrayList;
import java.util.List;

public class LineStrip {
    public final ArrayList<Point> points;

    public LineStrip(List<Point> points) {
        this.points = new ArrayList<>(points);
        checkInit();
    }

    public LineStrip(Point[] points) {
        this(List.of(points));
    }

    public LineStrip(Way way) {
        this(way.toPoints());
    }

    public LineStrip(float[] points) {
        this(new ArrayList<>() {
            {
                for (int i = 0; i < points.length; i += 3) {
                    add(new Point(points[i], points[i + 1], points[i + 2]));
                }
            }
        });
    }

    private void checkInit() {
        assert points.size() >= 2 : "LineStrip must have at least 2 points";
    }

    public float[] toVertexData(float r, float g, float b, float a) {
        return VertexData.toVertexData(points, r, g, b, a);
    }

    public float[] toVertexData() {
        return VertexData.toVertexData(points, true);
    }
}
