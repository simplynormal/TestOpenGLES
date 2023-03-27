package com.hcmut.test;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glUniformMatrix4fv;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.DisplayMetrics;

import com.hcmut.test.algorithm.BorderGenerator;
import com.hcmut.test.algorithm.StrokeGenerator;
import com.hcmut.test.data.VertexArray;
import com.hcmut.test.data.VertexData;
import com.hcmut.test.geometry.LineStrip;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.Polygon;
import com.hcmut.test.geometry.Triangle;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.map.MapReader;
import com.hcmut.test.object.ObjectBuilder;
import com.hcmut.test.data.Way;
import com.hcmut.test.programs.ColorShaderProgram;

import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TestRenderer implements GLSurfaceView.Renderer {
    private final Context context;
    private ColorShaderProgram colorProgram;
    private ObjectBuilder builder;
    MapReader mapReader;
    private final float[] vertices = {
            -0.5f, 0.5f, 0f,
            -0.5f, 0f, 0f,
            -0.5f, -0.5f, 0f,
            0.5f, -0.5f, 0f,
            0.5f, -0f, 0f,
            0f, 0f, 0f,
            0f, 0.05f, 0f,
            0.3f, 0.1f, 0f,
            0f, 0.4f, 0f,
            0f, 0.5f, 0f,
            -0.5f, 0.5f, 0f,
    };
    private final float[] vertices1 = {
            -2.843159f, 3.132369f, 0.0f,
            -2.340378f, 3.058398f, 0.0f,
            -2.2914348f, 3.0511677f, 0.0f,
            -2.10901f, 3.0278084f, 0.0f,
            -2.0689654f, 2.7814236f, 0.0f,
            -2.0823135f, 2.7491655f, 0.0f,
            -2.10901f, 2.7202446f, 0.0f,
            -2.3759732f, 2.7513902f, 0.0f,
            -2.8476083f, 2.8120131f, 0.0f,
            -2.8921022f, 2.8515015f, 0.0f,
            -2.9187984f, 3.1056728f, 0.0f,
            -2.843159f, 3.132369f, 0.0f,
            -2.8342602f, 3.2002223f, 0.0f,
            -2.820912f, 3.2869854f, 0.0f,
    };
    private final float[] vertices2 = {
            -0.5f, 0.5f, 0f,
            -0.5f, -0.5f, 0f,
            0.5f, -0.5f, 0f,
            0.5f, 0.5f, 0f,
            -0.5f, 0.5f, 0f,
    };

    private float oldX = 0;
    private float oldY = 0;
    private float oldDistance = 0;

    private final float[] projectionMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private float oldOriginX = 0;
    private float originX = 0;
    private float oldOriginY = 0;
    private float originY = 0;
    private float oldScale = 1f;
    private float scale = 1f;

    public TestRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        glClearColor(0.95f, 0.94f, 0.91f, 1f);
        colorProgram = new ColorShaderProgram(context);

//        float minLon = 106.73603f;
//        float maxLon = 106.74072f;
//        float minLat = 10.73122f;
//        float maxLat = 10.73465f;

        float minLon = 106.7091f;
        float maxLon = 106.7477f;
        float minLat = 10.7190f;
        float maxLat = 10.7455f;

        try {
            mapReader = new MapReader(context, R.raw.map);
            mapReader.setBounds(minLon, maxLon, minLat, maxLat);
            mapReader.read();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

//        mapReader.printObj();

        originX = (minLon + maxLon) / 2;
        originY = (minLat + maxLat) / 2;
        scale = 583.1902f;

        oldOriginX = originX;
        oldOriginY = originY;
        oldScale = scale;

//        originX = 0;
//        originY = 0;
//        scale = 1f;

        System.out.println("Origin: " + originX + ", " + originY + ", scale: " + scale);


        List<String> keyBlacklist = new ArrayList<>(List.of("221442275"));


        builder = new ObjectBuilder(colorProgram, projectionMatrix, modelViewMatrix);
//        Way way = new Way(vertices);
//        builder.addWay("asd", way, 0, 0, 1);

        for (String key : mapReader.ways.keySet()) {
            Way way = mapReader.ways.get(key);
            if (way != null) {
                builder.addWay(key, way, originX, originY, scale);
            }
        }
        builder.finalizeDrawer();
    }

    public void handleTouchDrag(float eventX, float eventY) {
        // Get screen size
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels;

        float newOriginX = oldX - eventX / scale / width * 2;
        float newOriginY = oldY + eventY / scale / width * 2;
        float translateX = -(newOriginX - originX) * scale;
        float translateY = -(newOriginY - originY) * scale;
        originX = newOriginX;
        originY = newOriginY;

        Matrix.translateM(modelViewMatrix, 0, translateX, translateY, 0);
        System.out.println("Origin: " + originX + ", " + originY + ", scale: " + scale);
    }

    public void handleTouchPress(float eventX, float eventY) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels;

        oldX = originX + eventX / scale / width * 2;
        oldY = originY - eventY / scale / width * 2;

        // translate to old origin
//        Matrix.translateM(modelViewMatrix, 0, (originX - oldOriginX) * scale, (originY - oldOriginY) * scale, 0);
    }

    public void handleZoom(List<Float> eventXs, List<Float> eventYs) {
        if (eventXs.size() != 2 || eventYs.size() != 2) {
            return;
        }
        float newDistance = (float) Math.sqrt(Math.pow(eventXs.get(0) - eventXs.get(1), 2) + Math.pow(eventYs.get(0) - eventYs.get(1), 2));
        float delta = oldDistance > 0 ? newDistance - oldDistance : 0;
        oldDistance = newDistance;
        float onePlusDeltaDivScale = 1 + delta / scale;

//        System.out.println("Zooming delta " + delta);
        System.out.println("Origin: " + originX + ", " + originY + ", scale: " + scale);
        // translate to old origin
        Matrix.translateM(modelViewMatrix, 0, (originX - oldOriginX) * scale, (originY - oldOriginY) * scale, 0);
        // scale
        Matrix.scaleM(modelViewMatrix, 0, onePlusDeltaDivScale, onePlusDeltaDivScale, 1);
        // translate back to new origin
        scale += delta;
        Matrix.translateM(modelViewMatrix, 0, -(originX - oldOriginX) * scale, -(originY - oldOriginY) * scale, 0);
//        Matrix.scaleM(modelViewMatrix, 0, deltaDivScale, deltaDivScale, 1);
    }

    public void handleResetZoom() {
        oldDistance = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to fill the entire surface.
        GLES20.glViewport(0, 0, width, height);
        final float aspectRatio = width > height ? (float) width / (float) height : (float) height / (float) width;
        Matrix.frustumM(projectionMatrix, 0, -width / (float) height, width / (float) height, -1f, 1f, 1f, 10f);
        Matrix.setLookAtM(modelViewMatrix, 0, 0f, -1.5f, 2f, 0f, 0, 0f, 0f, 1f, 0f);

//        Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f);
//        Matrix.setIdentityM(modelViewMatrix, 0);
    }

    public void testStroke() {
        VertexData.resetRandom();

        float[] first = new float[]{
                vertices1[0], vertices1[1], vertices1[2],
        };

        for (int i = 0; i < vertices1.length; i += 3) {
            vertices1[i] -= first[0];
            vertices1[i + 1] -= first[1];
            vertices1[i + 2] -= first[2];
        }

        Polygon rv = StrokeGenerator.generateStroke(new LineStrip(vertices1), 8, 0.02f);
//        Polygon rv = new Polygon(vertices1);
        TriangleStrip triangles = new TriangleStrip(rv.points);
        TriangleStrip single = new TriangleStrip(vertices1);
        triangles.points.addAll(single.points);


        VertexArray vertexArray = new VertexArray(triangles.toVertexData(0, 0, 0, 1));

        int strideInElements = VertexData.getTotalComponentCount();
        for (VertexData.VertexAttrib attrib : VertexData.getVertexAttribs()) {
            int location = colorProgram.getAttributeLocation(attrib.name);
            vertexArray.setVertexAttribPointer(attrib.offset, location, attrib.count, strideInElements);
        }
        colorProgram.useProgram();
        int uProjectionMatrix = colorProgram.getUniformLocation(ColorShaderProgram.U_PROJECTION_MATRIX);
        glUniformMatrix4fv(uProjectionMatrix, 1, false, projectionMatrix, 0);

        int uModelViewMatrix = colorProgram.getUniformLocation(ColorShaderProgram.U_MODEL_VIEW_MATRIX);
        glUniformMatrix4fv(uModelViewMatrix, 1, false, modelViewMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, triangles.points.size() - single.points.size());
        GLES20.glDrawArrays(GLES20.GL_POINTS, triangles.points.size() - single.points.size(), single.points.size());
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        // Clear the rendering surface.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        builder.draw();
//        testStroke();
    }
}
