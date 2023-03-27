package com.hcmut.test.object;

import android.opengl.GLES20;

import com.hcmut.test.algorithm.BorderGenerator;
import com.hcmut.test.algorithm.StrokeGenerator;
import com.hcmut.test.data.VertexArray;
import com.hcmut.test.data.Way;
import com.hcmut.test.geometry.LineStrip;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.Polygon;
import com.hcmut.test.geometry.Triangle;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.programs.ColorShaderProgram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HighwayDrawer extends Drawable {
    float[] projectionMatrix;
    float[] modelViewMatrix;
    HashMap<String, List<Triangle>> wayTriangles = new HashMap<>();
    HashMap<String, TriangleStrip> wayBorderTriangleStrips = new HashMap<>();
    VertexArray wayVertexArray;
    VertexArray wayBorderVertexArray;
    float originX = 0;
    float originY = 0;
    float scale = 1;

    public HighwayDrawer(ColorShaderProgram colorShaderProgram, float[] projectionMatrix, float[] modelViewMatrix) {
        super(colorShaderProgram);
        this.projectionMatrix = projectionMatrix;
        this.modelViewMatrix = modelViewMatrix;
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
        Polygon polygon = StrokeGenerator.generateStroke(linePoints, 8, 0.02f);
        List<Triangle> curTriangulatedTriangles = polygon.triangulate();
        TriangleStrip border = BorderGenerator.generateBorderFromPolygon(polygon, curTriangulatedTriangles, 0.002f);

        wayTriangles.put(key, curTriangulatedTriangles);
        wayBorderTriangleStrips.put(key, border);
    }

    public void removeWay(String key) {
        wayTriangles.remove(key);
        wayBorderTriangleStrips.remove(key);
    }

    public void finalizeDrawer() {
        if (wayTriangles.isEmpty() || wayBorderTriangleStrips.isEmpty()) {
            return;
        }

        List<Triangle> allTriangles = new ArrayList<>();
        for (List<Triangle> triangles : wayTriangles.values()) {
            allTriangles.addAll(triangles);
        }

        TriangleStrip allBorders = null;
        for (TriangleStrip border : wayBorderTriangleStrips.values()) {
            if (allBorders == null) {
                allBorders = border;
            } else {
                allBorders.extend(border);
            }
        }

        assert allBorders != null : "allBorders must not be null";

        float[] triangleVertexData = Triangle.toVertexData(allTriangles, 1, 1, 1, 1);
        wayVertexArray = new VertexArray(triangleVertexData);

        float[] borderVertexData = allBorders.toVertexData(0.5f, 0.5f, 0.5f, 1);
        wayBorderVertexArray = new VertexArray(borderVertexData);
    }

    private void drawHighway() {
        wayVertexArray.setDataFromVertexData(shaderProgram);
        int pointCount = wayVertexArray.getVertexCount();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, pointCount);
    }

    private void drawBorder() {
        wayBorderVertexArray.setDataFromVertexData(shaderProgram);
        int pointCount = wayBorderVertexArray.getVertexCount();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, pointCount);
    }

    public void draw() {
        if (wayTriangles.isEmpty() || wayBorderTriangleStrips.isEmpty()) {
            return;
        }

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);

        shaderProgram.useProgram();
        int uMVPLocation = shaderProgram.getUniformLocation(ColorShaderProgram.U_PROJECTION_MATRIX);
        GLES20.glUniformMatrix4fv(uMVPLocation, 1, false, projectionMatrix, 0);

        int uModelViewMatrix = shaderProgram.getUniformLocation(ColorShaderProgram.U_MODEL_VIEW_MATRIX);
        GLES20.glUniformMatrix4fv(uModelViewMatrix, 1, false, modelViewMatrix, 0);

        drawHighway();
        drawBorder();
    }
}
