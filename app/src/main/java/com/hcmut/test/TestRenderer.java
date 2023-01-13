package com.hcmut.test;

import static android.opengl.GLES20.glClearColor;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.programs.ColorShaderProgram;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TestRenderer implements GLSurfaceView.Renderer {
    private final Context context;
    private ColorShaderProgram colorProgram;
    private final float[] vertices = {
            -0.5f, -0.5f, 0f,
            0.5f, -0.5f, 0f,
            0.5f, 0.5f, 0f,
            -0.5f, 0.5f, 0f
            -0.5f, -0.5f, 0f,
    };

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
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        // Clear the rendering surface.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        colorProgram.useProgram();
        colorProgram.setUniforms(0f, 1f, 0f);

        VertexArray vertexArray = new VertexArray(vertices);
        vertexArray.setVertexAttribPointer(0, colorProgram.getPositionAttributeLocation(), 3, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 5);
    }
}
