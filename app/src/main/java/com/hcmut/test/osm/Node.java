package com.hcmut.test.osm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hcmut.test.geometry.Point;

import java.util.HashMap;

public class Node {
    public final float lat;
    public final float lon;

    public Node(float lon, float lat) {
        this.lon = lon;
        this.lat = lat;
    }

    public Point toPoint() {
        return new Point(lon, lat);
    }

    public Point toPoint(float scale) {
        return new Point(lon * scale, lat * scale);
    }

    public Point toPoint(float originX, float originY, float scale) {
        return new Point((lon - originX) * scale, (lat - originY) * scale);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof Node && ((Node) obj).lat == lat && ((Node) obj).lon == lon;
    }

    @NonNull
    @Override
    public String toString() {
        return "Node{" +
                "lat=" + lat +
                ", lon=" + lon +
                '}';
    }
}
