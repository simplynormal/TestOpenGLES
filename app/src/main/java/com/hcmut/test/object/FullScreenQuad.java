package com.hcmut.test.object;

import android.opengl.GLES20;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.programs.FrameShaderProgram;

public class FullScreenQuad {
    private final FrameShaderProgram shaderProgram;
    private final VertexArray vertexArray;

    private static final float[] DATA = {
            -1.0f, -1.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f
    };

    public FullScreenQuad(FrameShaderProgram shaderProgram) {
        this.shaderProgram = shaderProgram;
        vertexArray = new VertexArray(shaderProgram, DATA);
    }

    public void draw(int textureId) {
        shaderProgram.useProgram();
        shaderProgram.setCurrentTexture(textureId);

        vertexArray.setDataFromVertexData();

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }
}

