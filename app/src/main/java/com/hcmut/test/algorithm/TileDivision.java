package com.hcmut.test.algorithm;

import com.hcmut.test.geometry.BoundBox;

public class TileDivision {
    public static final int ZOOM = 16;

    public static int getTileId(float lon, float lat) {
        int tileX = (int) ((lon + 180) / 360 * Math.pow(2, ZOOM));
        int tileY = (int) ((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * Math.pow(2, ZOOM));
        return (int) (tileX + tileY * Math.pow(2, ZOOM));
    }

    public static BoundBox getBoundBox(int tileId) {
        int tileX = (int) (tileId % Math.pow(2, ZOOM));
        int tileY = (int) (tileId / Math.pow(2, ZOOM));
        float minLon = (float) (tileX * 360 / Math.pow(2, ZOOM) - 180);
        float minLat = (float) (Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * (tileY + 1) / Math.pow(2, ZOOM))))));
        float maxLon = (float) ((tileX + 1) * 360 / Math.pow(2, ZOOM) - 180);
        float maxLat = (float) (Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * tileY / Math.pow(2, ZOOM))))));
        return new BoundBox(minLon, minLat, maxLon, maxLat);
    }
}
