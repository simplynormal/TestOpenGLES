package com.hcmut.test.object;

import android.opengl.GLES20;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.mapnik.symbolizer.SymMeta;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.programs.TextSymbShaderProgram;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.List;

public class UserIcon {
    private static final float RADIUS = 0.08f;
    private static final float RADIUS2 = 0.09f;
    private static final int NUM_OF_VERTICES = 50;
    private static final float[] COLOR_1 = new float[] {1, 1, 1, 1};
    private static final float[] COLOR_2 = new float[] {0, 0, 0, 1};
    private static final float[] ARROW_COLOR = new float[]{0.0f, 1f, 0.0f, 1.0f};
    private static final int NUM_OF_ARROW_VERTICES = 3;
    private final Config config;
    VertexArray vertexArray = null;
    private float[] drawable;

    public void relocate(Point center) {
        List<Point> points = new ArrayList<>(NUM_OF_VERTICES + 2);
        List<Point> points2 = new ArrayList<>(NUM_OF_VERTICES + 2);
        Point o = new Point(0, 0);
        points.add(o);
        points2.add(o);
        for (int i = 0; i < NUM_OF_VERTICES; i++) {
            float angle = (float) (2 * Math.PI * i / NUM_OF_VERTICES);
            float dx = (float) (RADIUS * Math.cos(angle));
            float dy = (float) (RADIUS * Math.sin(angle));
            points.add(new Point(dx, dy));
            dx = (float) (RADIUS2 * Math.cos(angle));
            dy = (float) (RADIUS2 * Math.sin(angle));
            points2.add(new Point(dx, dy));
        }
        points.add(points.get(1));
        points2.add(points2.get(1));

        List<Point> arrowPoints = new ArrayList<>(NUM_OF_ARROW_VERTICES);
        arrowPoints.add(new Point(0, 0 + RADIUS));
        arrowPoints.add(new Point(0 - RADIUS2 / 2, 0 - RADIUS2 / 2));
        arrowPoints.add(new Point(0 + RADIUS2 / 2, 0 - RADIUS2 / 2));

        float[] drawable = SymMeta.appendRegular(TextSymbShaderProgram.toPointVertexData(points, 10, center, center, COLOR_1), TextSymbShaderProgram.toPointVertexData(points2, 10, center, center, COLOR_2));
        drawable = SymMeta.appendRegular(drawable, TextSymbShaderProgram.toPointVertexData(arrowPoints, 10, center, center, ARROW_COLOR));

        if (vertexArray == null) {
            this.drawable = drawable;
        } else {
            vertexArray.changeData(drawable);
        }
    }

    public UserIcon(Config config, Point center) {
        this.config = config;
        relocate(center);
    }

    public void draw() {
        if (vertexArray == null) {
            vertexArray = new VertexArray(config.getTextSymbShaderProgram(), drawable, GLES20.GL_DYNAMIC_DRAW);
        }
        vertexArray.setDataFromVertexData();
        int pointCount = vertexArray.getVertexCount() - NUM_OF_ARROW_VERTICES;
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, pointCount / 2, pointCount / 2);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, pointCount / 2);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, pointCount, NUM_OF_ARROW_VERTICES);
    }
}
