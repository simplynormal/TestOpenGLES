package com.hcmut.test.remote;

import androidx.annotation.NonNull;

import java.util.HashMap;

public class LayerResponse {
    public final NodeResponse[] nodes;
    public final WayResponse[] ways;

    public LayerResponse(NodeResponse[] nodes, WayResponse[] ways) {
        this.nodes = nodes;
        this.ways = ways;
    }

    @NonNull
    @Override
    public String toString() {
        return "LayerResponse{" +
                "nodes length=" + nodes.length +
                ", ways length=" + ways.length +
                '}';
    }
}
