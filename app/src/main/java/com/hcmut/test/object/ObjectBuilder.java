/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/
package com.hcmut.test.object;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;

import android.opengl.GLES20;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.data.VertexData;
import com.hcmut.test.data.Way;
import com.hcmut.test.geometry.Triangle;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.programs.ColorShaderProgram;

import com.hcmut.test.geometry.Polygon;

import java.util.ArrayList;
import java.util.List;

public class ObjectBuilder {
    //    private float[] waysVertexData = new float[0];
//    private float[] linesVertexData = new float[0];
//    private float[] borderVertexData = new float[0];
    private final float[] testVertexData = new float[]{
            -0f, 1.5f, 0f,
            -1f, 1f, 0f,
            -2f, 0f, 0f,
            -3f, 1f, 0f,
            -4f, 0f, 0f,
            -5f, -5f, 0f,

    };

    List<Triangle> triangles = new ArrayList<>();
    TriangleStrip borderTriangleStrip = null;
    ColorShaderProgram colorProgram;
    HighwayDrawer highwayDrawer;
    float[] projectionMatrix;

    private static boolean isDebug = false;

    public ObjectBuilder(ColorShaderProgram colorProgram, float[] projectionMatrix) {
        VertexData.resetRandom();
        this.colorProgram = colorProgram;
        this.projectionMatrix = projectionMatrix;
        highwayDrawer = new HighwayDrawer(colorProgram, projectionMatrix);
    }

    public void addWay(String key, Way way, float originX, float originY, float scale) {
//        System.out.println("add way" + way);
        if (way.isClosed()) {
            addClosedWay(way, originX, originY, scale);
        } else {
//            addOpenWay(way, originX, originY, scale);
            highwayDrawer.addWay(key, way, originX, originY, scale);
        }

    }

    private void addClosedWay(Way way, float originX, float originY, float scale) {
        Polygon polygon = new Polygon(way.toPoints(originX, originY, scale));
        List<Triangle> curTriangulatedTriangles = polygon.triangulate();
        triangles.addAll(curTriangulatedTriangles);
//        TriangleStrip newWay = BorderGenerator.generateBorderFromPolygon(polygon, curTriangulatedTriangles, 0.002f);
//        if (borderTriangleStrip == null)
//            borderTriangleStrip = newWay;
//        else
//            borderTriangleStrip.extend(newWay);
    }

//    public void addOpenWay(Way way, float originX, float originY, float scale) {
//        LineStrip linePoints = new LineStrip(way.toPoints(originX, originY, scale));
//        Polygon polygon = StrokeGenerator.generateStroke(linePoints, 8, 0.02f);
//        List<Triangle> curTriangulatedTriangles = polygon.triangulate();
//        triangles1.addAll(curTriangulatedTriangles);
//        TriangleStrip newWay = BorderGenerator.generateBorderFromPolygon(polygon, curTriangulatedTriangles, 0.002f);
//        if (borderTriangleStrip == null)
//            borderTriangleStrip = newWay;
//        else
//            borderTriangleStrip.extend(newWay);
//    }

    public void finalizeDrawer() {
        highwayDrawer.finalizeDrawer();
    }

    public void draw() {
        VertexData.resetRandom();
        float[] waysVertexData = triangles.size() > 0 ? new float[triangles.get(0).toVertexData().length * (triangles.size())] : new float[0];
        for (int i = 0; i < triangles.size(); i++) {
            System.arraycopy(triangles.get(i).toVertexData(0.67f, 0.83f, 0.87f, 1f), 0, waysVertexData, i * triangles.get(i).toVertexData().length, triangles.get(i).toVertexData().length);
        }
        float[] borderVertexData = borderTriangleStrip != null ? borderTriangleStrip.toVertexData(0, 0, 0, 1) : new float[0];

        float[] vertexData = new float[borderVertexData.length + waysVertexData.length + testVertexData.length];
        System.arraycopy(borderVertexData, 0, vertexData, 0, borderVertexData.length);
        System.arraycopy(waysVertexData, 0, vertexData, borderVertexData.length, waysVertexData.length);
        System.arraycopy(testVertexData, 0, vertexData, borderVertexData.length + waysVertexData.length, testVertexData.length);

        int uMVPLocation = colorProgram.getUniformLocation(ColorShaderProgram.U_MATRIX);

        VertexArray vertexArray = new VertexArray(vertexData);

        int strideInElements = VertexData.getTotalComponentCount();
        vertexArray.setDataFromVertexData(colorProgram);

        colorProgram.useProgram();
        GLES20.glUniformMatrix4fv(uMVPLocation, 1, false, projectionMatrix, 0);
//        GLES20.glUniform4f(uColorLocation, 0.57f, 0.76f, 0.88f, 1f);
        glDrawArrays(GL_TRIANGLES, borderVertexData.length / strideInElements, waysVertexData.length / strideInElements);


//        GLES20.glUniform4f(uColorLocation, 0f, 0f, 0f, 1f);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, borderVertexData.length / strideInElements);


//        colorProgram.setUniformColor(0.8f, 0.8f, 0.8f);
//        glDrawArrays(GL_TRIANGLE_STRIP, (borderVertexData.length + waysVertexData.length) / 3, testVertexData.length / 3);

//        colorProgram.setUniformColor(0f, 0f, 1f);
//        glDrawArrays(GL_LINES, 0, borderVertexData.length / 3);

//        colorProgram.setUniformColor(1f, 1f, 1f);
//        int test = 38;
//        glDrawArrays(GL_TRIANGLE_STRIP, test, 4);

        highwayDrawer.draw();
        isDebug = false;
    }
}
