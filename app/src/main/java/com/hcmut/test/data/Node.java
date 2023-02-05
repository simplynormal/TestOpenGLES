package com.hcmut.test.data;

import androidx.annotation.NonNull;

public class Node {
    public final float lat;
    public final float lon;

    public Node(float lat, float lon) {
        this.lat = lat;
        this.lon = lon;
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
