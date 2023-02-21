package com.hcmut.test.geometry;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

public class Point {
    public final float x;
    public final float y;
    public final float z;
    public static final int[] VERTEX_ATTRIBS = new int[]{
            GLES20.GL_FLOAT, 3,
    };

    public static int getTotalComponentCount() {
        int result = 0;
        for (int i = 0; i < VERTEX_ATTRIBS.length; i += 2) {
            result += VERTEX_ATTRIBS[i + 1];
        }
        return result;
    }

    public Point(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Point(float x, float y) {
        this.x = x;
        this.y = y;
        this.z = 0;
    }

    public Point() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    public Point(Point p) {
        this.x = p.x;
        this.y = p.y;
        this.z = p.z;
    }

    public Point add(Vector v) {
        return new Point(this.x + v.x, this.y + v.y, this.z + v.z);
    }

    public float[] toVertexData() {
        return new float[]{x, y, z};
    }

    @NonNull
    @Override
    public String toString() {
        return "Point{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
