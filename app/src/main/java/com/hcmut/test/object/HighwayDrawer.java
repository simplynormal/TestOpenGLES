package com.hcmut.test.object;

import android.opengl.GLES20;

import com.hcmut.test.algorithm.StrokeGenerator;
import com.hcmut.test.algorithm.StrokeGenerator.Stroke;
import com.hcmut.test.data.VertexArray;
import com.hcmut.test.data.Way;
import com.hcmut.test.geometry.LineStrip;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.Triangle;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.programs.ColorShaderProgram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HighwayDrawer extends Drawable {
    HashMap<String, TriangleStrip> wayFill = new HashMap<>();
    HashMap<String, TriangleStrip> wayBorder = new HashMap<>();
    VertexArray wayVertexArray;
    VertexArray wayBorderVertexArray;
    float originX = 0;
    float originY = 0;
    float scale = 1;

    public HighwayDrawer(ColorShaderProgram colorShaderProgram) {
        super(colorShaderProgram);
    }

    public void addWay(String key, Way way, float originX, float originY, float scale) {
        this.originX = originX;
        this.originY = originY;
        this.scale = scale;
        try {
            addWay(key, way);
        } catch (Exception e) {
            System.err.println("Error drawing way " + key);
            for (Point point : way.toPoints(originX, originY, scale)) {
                System.err.println(point.x + "f, " + point.y + "f, " + point.z + "f,");
            }
            e.printStackTrace();
        }
    }

    public void addWay(String key, Way way) {
        LineStrip linePoints = new LineStrip(way.toPoints(originX, originY, scale));
        Stroke stroke = StrokeGenerator.generateStrokeT(linePoints, 8, 0.02f);
        TriangleStrip border = StrokeGenerator.generateBorderFromStroke(stroke, 8, 0.002f);

        wayFill.put(key, stroke.toTriangleStrip());
        wayBorder.put(key, border);
    }

    public void removeWay(String key) {
        wayFill.remove(key);
        wayBorder.remove(key);
    }

    public void finalizeDrawer() {
        if (wayFill.isEmpty() || wayBorder.isEmpty()) {
            return;
        }

        TriangleStrip allFill = null;
        for (TriangleStrip fill : wayFill.values()) {
            if (allFill == null) {
                allFill = fill;
            } else {
                allFill.extend(fill);
            }
        }

        TriangleStrip allBorders = null;
        for (TriangleStrip border : wayBorder.values()) {
            if (allBorders == null) {
                allBorders = border;
            } else {
                allBorders.extend(border);
            }
        }

        assert allBorders != null && allFill != null : "allBorders and allFill must not be null";

        wayVertexArray = new VertexArray(shaderProgram, allFill, 1, 1, 1, 1);

        wayBorderVertexArray = new VertexArray(shaderProgram, allBorders, 0.5f, 0.5f, 0.5f, 1);
    }

    private void drawHighway() {
        wayVertexArray.setDataFromVertexData();
        int pointCount = wayVertexArray.getVertexCount();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, pointCount);
    }

    private void drawBorder() {
        wayBorderVertexArray.setDataFromVertexData();
        int pointCount = wayBorderVertexArray.getVertexCount();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, pointCount);
    }

    public void draw() {
        if (wayFill.isEmpty() || wayBorder.isEmpty()) {
            return;
        }

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);

        shaderProgram.useProgram();
        drawHighway();
        drawBorder();
    }
}
