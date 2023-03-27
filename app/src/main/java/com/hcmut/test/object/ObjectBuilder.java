/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/
package com.hcmut.test.object;

import static android.opengl.GLES20.GL_LINE_STRIP;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;

import android.opengl.GLES20;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.data.VertexData;
import com.hcmut.test.data.Way;
import com.hcmut.test.geometry.LineStrip;
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
    AreaDrawer areaDrawer;
    float[] projectionMatrix;
    float[] modelViewMatrix;


    public ObjectBuilder(ColorShaderProgram colorProgram, float[] projectionMatrix, float[] modelViewMatrix) {
        VertexData.resetRandom();
        this.colorProgram = colorProgram;
        this.projectionMatrix = projectionMatrix;
        this.modelViewMatrix = modelViewMatrix;
        areaDrawer = new AreaDrawer(colorProgram, projectionMatrix, modelViewMatrix);
        highwayDrawer = new HighwayDrawer(colorProgram, projectionMatrix, modelViewMatrix);
    }

    public void addWay(String key, Way way, float originX, float originY, float scale) {
        if (way.isClosed()) {
            areaDrawer.addWay(key, way, originX, originY, scale);
        } else {
            highwayDrawer.addWay(key, way, originX, originY, scale);
        }
    }

    public void finalizeDrawer() {
        highwayDrawer.finalizeDrawer();
        areaDrawer.finalizeDrawer();
    }

    public void draw() {
        VertexData.resetRandom();

        areaDrawer.draw();
        highwayDrawer.draw();
    }
}
