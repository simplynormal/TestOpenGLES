package com.hcmut.test.geometry;

import androidx.annotation.NonNull;

public class BoundBox {
    public final float minX;
    public final float minY;
    public final float maxX;
    public final float maxY;

    public BoundBox(float minX, float minY, float maxX, float maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public boolean within(BoundBox boundBox) {
        return minX >= boundBox.minX && minY >= boundBox.minY && maxX <= boundBox.maxX && maxY <= boundBox.maxY;
    }

    public boolean intersects(BoundBox boundBox) {
        return !(minX > boundBox.maxX || maxX < boundBox.minX || minY > boundBox.maxY || maxY < boundBox.minY);
    }

    public boolean withinOrIntersects(BoundBox boundBox) {
        return intersects(boundBox) || within(boundBox) || boundBox.within(this);
    }

    public BoundBox scale(float scale) {
        return new BoundBox(minX * scale, minY * scale, maxX * scale, maxY * scale);
    }

    @NonNull
    @Override
    public String toString() {
        return "BoundBox{" +
                "minX=" + minX +
                ", minY=" + minY +
                ", maxX=" + maxX +
                ", maxY=" + maxY +
                '}';
    }
}
