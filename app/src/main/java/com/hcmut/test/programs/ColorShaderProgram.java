/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/
package com.hcmut.test.programs;

import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniformMatrix4fv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES20;

import com.hcmut.test.R;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.PointList;

import java.util.Arrays;
import java.util.List;

@SuppressLint("NewApi")
public class ColorShaderProgram extends ShaderProgram {
    public static final String U_PROJECTION_MATRIX = "u_ProjectionMatrix";
    public static final String U_MODEL_VIEW_MATRIX = "u_ModelViewMatrix";
    public static final String U_TRANSFORM_MATRIX = "u_TransformMatrix";
    public static final String U_COLOR = "u_Color";
    public static final String A_POSITION = "a_Position";
    public static final String A_COLOR = "a_Color";
    private static final int[] VERTEX_ATTRIBS = new int[]{
            3, 4
    };
    private static final String[] VERTEX_ATTRIB_NAMES = new String[]{
            A_POSITION, A_COLOR
    };
    public static final int TOTAL_VERTEX_ATTRIB_COUNT = Arrays.stream(VERTEX_ATTRIBS).reduce(0, Integer::sum);
    private static final List<VertexAttrib> VERTEX_ATTRIB_LIST = getVertexAttribs(VERTEX_ATTRIB_NAMES, VERTEX_ATTRIBS);
    private final float[] projectionMatrix;
    private final float[] modelViewMatrix;
    private final float[] transformMatrix;


    public ColorShaderProgram(Context context, float[] projectionMatrix, float[] modelViewMatrix, float[] transformMatrix) {
        super(context, R.raw.simple_vert,
                R.raw.simple_frag);
        this.projectionMatrix = projectionMatrix;
        this.modelViewMatrix = modelViewMatrix;
        this.transformMatrix = transformMatrix;
    }

    @Override
    public void useProgram() {
        super.useProgram();
        int uMVPLocation = getUniformLocation(U_PROJECTION_MATRIX);
        GLES20.glUniformMatrix4fv(uMVPLocation, 1, false, projectionMatrix, 0);

        int uModelViewMatrix = getUniformLocation(U_MODEL_VIEW_MATRIX);
        GLES20.glUniformMatrix4fv(uModelViewMatrix, 1, false, modelViewMatrix, 0);

        int uTransformMatrix = getUniformLocation(U_TRANSFORM_MATRIX);
        GLES20.glUniformMatrix4fv(uTransformMatrix, 1, false, transformMatrix, 0);
    }

    @Override
    public List<VertexAttrib> getVertexAttribs() {
        return VERTEX_ATTRIB_LIST;
    }

    @Override
    public int getTotalVertexAttribCount() {
        return TOTAL_VERTEX_ATTRIB_COUNT;
    }

    public static float[] toVertexData(Point p, float r, float g, float b, float a) {
        return new float[]{p.x, p.y, p.z, r, g, b, a};
    }

    public static float[] toVertexData(List<Point> points, float r, float g, float b, float a) {
        if (points == null) {
            return new float[0];
        }
        float[] result = new float[points.size() * TOTAL_VERTEX_ATTRIB_COUNT];
        for (int i = 0; i < points.size(); i++) {
            float[] vertexData = toVertexData(points.get(i), r, g, b, a);
            System.arraycopy(vertexData, 0, result, i * vertexData.length, vertexData.length);
        }
        return result;
    }

    public static float[] toVertexData(PointList p, float r, float g, float b, float a) {
        if (p == null) {
            return new float[0];
        }
        return toVertexData(p.points, r, g, b, a);
    }

    public static float[] toVertexData(PointList p, float[] color) {
        return toVertexData(p, color[0], color[1], color[2], color[3]);
    }

    public static float[] toVertexData(List<Point> points, float[] color) {
        return toVertexData(points, color[0], color[1], color[2], color[3]);
    }
}
