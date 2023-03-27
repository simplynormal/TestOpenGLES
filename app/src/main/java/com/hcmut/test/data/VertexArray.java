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

import com.hcmut.test.programs.ShaderProgram;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class VertexArray {
    private static final int BYTES_PER_FLOAT = 4;
    private final FloatBuffer floatBuffer;
    private final int vertexCount;

    public VertexArray(float[] vertexData) {
        floatBuffer = ByteBuffer
                .allocateDirect(vertexData.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexCount = vertexData.length / VertexData.getTotalComponentCount();
    }

    public void setVertexAttribPointer(int dataOffset, int attributeLocation,
                                       int componentCount, int strideInElements) {
        floatBuffer.position(dataOffset);
        glVertexAttribPointer(attributeLocation, componentCount, GL_FLOAT,
                false, strideInElements * BYTES_PER_FLOAT, floatBuffer);
        glEnableVertexAttribArray(attributeLocation);

        floatBuffer.position(0);
    }

    public void setDataFromVertexData(ShaderProgram shaderProgram) {
        int strideInElements = VertexData.getTotalComponentCount();
        for (VertexData.VertexAttrib attrib : VertexData.getVertexAttribs()) {
            int location = shaderProgram.getAttributeLocation(attrib.name);
            setVertexAttribPointer(attrib.offset, location, attrib.count, strideInElements);
        }
    }

    public int getVertexCount() {
        return vertexCount;
    }
}
