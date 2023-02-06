package com.hcmut.test.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Node {
    public final float lat;
    public final float lon;

    public Node(float lat, float lon) {
        this.lat = lat;
        this.lon = lon;
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
