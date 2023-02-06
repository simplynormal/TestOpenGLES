package com.hcmut.test;

import static android.opengl.GLES20.glClearColor;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.hcmut.test.data.Node;
import com.hcmut.test.data.Triangle;
import com.hcmut.test.data.VertexArray;
import com.hcmut.test.object.ObjectBuilder;
import com.hcmut.test.object.Way;
import com.hcmut.test.programs.ColorShaderProgram;

import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TestRenderer implements GLSurfaceView.Renderer {
    private final Context context;
    private ColorShaderProgram colorProgram;
    boolean isDraw = false;
    private final float[] vertices = {
            -0.5f, 0.5f, 0f,
            -0.5f, -0.5f, 0f,
            0.5f, -0.5f, 0f,
            0.5f, -0f, 0f,
            0f, 0f, 0f,
            0f, 0.05f, 0f,
            0.8f, 0.1f, 0f,
            0f, 0.4f, 0f,
            0f, 0.5f, 0f,
            -0.5f, 0.5f, 0f,
    };
    private final float[] projectionMatrix = new float[16];

    public TestRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        colorProgram = new ColorShaderProgram(context);
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to fill the entire surface.
        GLES20.glViewport(0, 0, width, height);
        final float aspectRatio =
                width > height ? (float) width / (float) height : (float) height / (float) width;
        if (width > height) {
            // Landscape
            Matrix.orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f);
        } else {
            // Portrait or square
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f);
        }
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        // Clear the rendering surface.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_STENCIL_TEST);

        Way way = new Way();
        for (int i = 0; i < vertices.length; i += 3) {
            way.addNode(new Node(vertices[i], vertices[i + 1]));
        }
        ObjectBuilder builder = new ObjectBuilder();
        builder.addWay(way, 0f, 0f, 1f);

        builder.draw(colorProgram, projectionMatrix);


//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 3, 6);

//        GLES20.glColorMask(false, false, false, false);
//        GLES20.glStencilMask(1);
//        GLES20.glStencilFunc(GLES20.GL_ALWAYS, 0, 1);
//        GLES20.glStencilOp(GLES20.GL_KEEP, GLES20.GL_KEEP, GLES20.GL_INVERT);
//
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
//
//        GLES20.glColorMask(true, true, true, true);
//        GLES20.glStencilFunc(GLES20.GL_EQUAL, 1, 1);
//        GLES20.glStencilOp(GLES20.GL_KEEP, GLES20.GL_KEEP, GLES20.GL_KEEP);

//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 3, 6);

//        GLES20.glColorMask(false, false, false, false);
//        GLES20.glStencilMask(1);
//        GLES20.glStencilFunc(GLES20.GL_ALWAYS, 0, 1);
//        GLES20.glStencilOp(GLES20.GL_KEEP, GLES20.GL_KEEP, GLES20.GL_INVERT);
//
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 9, 8);
//
//        GLES20.glColorMask(true, true, true, true);
//        GLES20.glStencilFunc(GLES20.GL_EQUAL, 1, 1);
//        GLES20.glStencilOp(GLES20.GL_KEEP, GLES20.GL_KEEP, GLES20.GL_KEEP);
//
//        colorProgram.setUniforms(projectionMatrix, 0f, 1f, 0f);
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 9, 8);
    }
}
