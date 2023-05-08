package com.hcmut.test.remote;

public class NodeResponse {
    public final long id;
    public final float lat;
    public final float lon;

    public NodeResponse(long id, float lat, float lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
    }
}
