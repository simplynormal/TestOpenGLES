/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/
package com.hcmut.test.data;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glVertexAttribPointer;

import android.opengl.GLES20;

import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.geometry.Polygon;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.programs.ShaderProgram;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VertexArray {
    protected static final int BYTES_PER_FLOAT = 4;
    protected static final int seed = 69;
    protected static Random random = new Random(seed);
    protected final ShaderProgram shaderProgram;
    protected int vertexCount;
    protected int id = -1;

    private void genBuffer(float[] vertexData) {
        if (id != -1) {
            GLES20.glDeleteBuffers(1, new int[]{id}, 0);
        }
        FloatBuffer floatBuffer = ByteBuffer
                .allocateDirect(vertexData.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        floatBuffer.position(0);
        int[] vbo = new int[1];
        GLES20.glGenBuffers(1, vbo, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, floatBuffer.capacity() * BYTES_PER_FLOAT, floatBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        id = vbo[0];
    }

    public VertexArray(ShaderProgram shaderProgram, float[] vertexData) {
        this.shaderProgram = shaderProgram;
        this.vertexCount = vertexData.length / shaderProgram.getTotalVertexAttribCount();
        genBuffer(vertexData);
    }

    public void setVertexAttribPointer(int dataOffset, int attributeLocation,
                                       int componentCount, int strideInElements) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id);
        glVertexAttribPointer(attributeLocation, componentCount, GL_FLOAT,
                false, strideInElements * BYTES_PER_FLOAT, dataOffset * BYTES_PER_FLOAT);
        glEnableVertexAttribArray(attributeLocation);
    }

    public void setDataFromVertexData() {
        shaderProgram.useProgram();
        List<ShaderProgram.VertexAttrib> attribs = shaderProgram.getVertexAttribs();
        int totalComponents = shaderProgram.getTotalVertexAttribCount();
        List<String> attribNamesNotfound = new ArrayList<>(0);
        for (ShaderProgram.VertexAttrib attrib : attribs) {
            int location = shaderProgram.getAttributeLocation(attrib.name);
            if (location == -1) {
                attribNamesNotfound.add(attrib.name);
                continue;
            }
            setVertexAttribPointer(attrib.offset, location, attrib.count, totalComponents);
        }

        if (attribNamesNotfound.size() > 0) {
            throw new RuntimeException("Attribute(s) not found: " + attribNamesNotfound);
        }
    }

    public int getVertexCount() {
        return vertexCount;
    }

    @Override
    protected void finalize() throws Throwable {
        if (id != -1) {
            GLES20.glDeleteBuffers(1, new int[]{id}, 0);
        }
        super.finalize();
    }
}
