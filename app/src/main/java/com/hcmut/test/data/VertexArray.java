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

import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.geometry.Polygon;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.programs.ShaderProgram;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VertexArray {
    private static final int BYTES_PER_FLOAT = 4;
    private static final int seed = 69;
    private static Random random = new Random(seed);
    private final FloatBuffer floatBuffer;
    private final ShaderProgram shaderProgram;
    private final int vertexCount;
    public static void resetRandom() {
        random = new Random(seed);
    }

    public VertexArray(ShaderProgram shaderProgram, float[] vertexData) {
        floatBuffer = ByteBuffer
                .allocateDirect(vertexData.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        this.shaderProgram = shaderProgram;
        this.vertexCount = vertexData.length / shaderProgram.getTotalVertexAttribCount();
    }

    public VertexArray(ShaderProgram shaderProgram, Point p, float r, float g, float b, float a) {
        this(shaderProgram, toVertexData(p, r, g, b, a));
    }

    public VertexArray(ShaderProgram shaderProgram, List<Point> points, float r, float g, float b, float a) {
        this(shaderProgram, toVertexData(shaderProgram, points, r, g, b, a));
    }

    public VertexArray(ShaderProgram shaderProgram, List<Point> points) {
        this(shaderProgram, toVertexData(shaderProgram, points));
    }

    public VertexArray(ShaderProgram shaderProgram, PointList p, float r, float g, float b, float a) {
        this(shaderProgram, p.points, r, g, b, a);
    }

    public VertexArray(ShaderProgram shaderProgram, PointList p) {
        this(shaderProgram, p.points);
    }


    public void setVertexAttribPointer(int dataOffset, int attributeLocation,
                                       int componentCount, int strideInElements) {
        floatBuffer.position(dataOffset);
        glVertexAttribPointer(attributeLocation, componentCount, GL_FLOAT,
                false, strideInElements * BYTES_PER_FLOAT, floatBuffer);
        glEnableVertexAttribArray(attributeLocation);

        floatBuffer.position(0);
    }

    public void setDataFromVertexData() {
        List<ShaderProgram.VertexAttrib> attribs = shaderProgram.getVertexAttribs();
        int totalComponents = shaderProgram.getTotalVertexAttribCount();
        for (ShaderProgram.VertexAttrib attrib : attribs) {
            int location = shaderProgram.getAttributeLocation(attrib.name);
            setVertexAttribPointer(attrib.offset, location, attrib.count, totalComponents);
        }
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public static float[] toVertexData(Point p, float r, float g, float b, float a) {
        return new float[]{p.x, p.y, p.z, r, g, b, a};
    }

    public static float[] toVertexData(ShaderProgram shaderProgram, List<Point> points, float r, float g, float b, float a) {
        float[] result = new float[points.size() * shaderProgram.getTotalVertexAttribCount()];
        for (int i = 0; i < points.size(); i++) {
            float[] vertexData = toVertexData(points.get(i), r, g, b, a);
            System.arraycopy(vertexData, 0, result, i * vertexData.length, vertexData.length);
        }
        return result;
    }

    public static float[] toVertexData(ShaderProgram shaderProgram, List<Point> points) {
        float r = random.nextFloat();
        float g = random.nextFloat();
        float b = random.nextFloat();
        float a = 0.5f;
        float[] result = new float[points.size() * shaderProgram.getTotalVertexAttribCount()];
        for (int i = 0; i < points.size(); i++) {
            float[] vertexData = toVertexData(points.get(i), r, g, b, a);
            System.arraycopy(vertexData, 0, result, i * vertexData.length, vertexData.length);
        }
        return result;
    }
}
