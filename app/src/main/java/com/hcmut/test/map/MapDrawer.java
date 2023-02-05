package com.hcmut.test.map;

import android.content.Context;

import com.hcmut.test.programs.ColorShaderProgram;

public class MapDrawer {
    private final Context ctx;
    private final MapReader mapReader;
    private ColorShaderProgram colorProgram;

    public MapDrawer(Context ctx, MapReader mapReader) {
        this.mapReader = mapReader;
        this.ctx = ctx;
    }

    private void drawWay() {

    }
}
