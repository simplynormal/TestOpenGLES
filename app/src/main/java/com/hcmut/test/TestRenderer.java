package com.hcmut.test;

import static android.opengl.GLES20.glClearColor;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.DisplayMetrics;

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
import com.hcmut.test.utils.StrokeGenerator;

import org.xmlpull.v1.XmlPullParserException;

import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TestRenderer implements GLSurfaceView.Renderer {
    private final Context context;
    private ColorShaderProgram colorProgram;
    private ObjectBuilder builder;
    MapReader mapReader;
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
    private final float[] vertices1 = {
            0f, 0.5f, 0f,
            0f, 0f, 0f,
            -0.5f, 0f, 0f,
            -0.5f, -0.5f, 0f,
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
    private float originX = 0;
    private float originY = 0;
    private float scale = 1f;

    public TestRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        glClearColor(0.95f, 0.94f, 0.91f, 1f);
        colorProgram = new ColorShaderProgram(context);
        try {
            mapReader = new MapReader(context, R.raw.map);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

//        mapReader.printObj();

        originX = mapReader.center.lon;
        originY = mapReader.center.lat;
        scale = 1f / mapReader.height;

//        originX = 0;
//        originY = 0;
//        scale = 1f;

//        System.out.println("Origin: " + originX + ", " + originY + ", scale: " + scale);

        builder = new ObjectBuilder();
//        Way way = new Way(vertices1);
//        builder.addWay(way, originX, originY, scale);

        List<String> keyBlacklist = List.of(
                "205341834",
                "224935486",
                "205957662",
                "205342060",
                "611427593"
        );

//        for (String key : mapReader.ways.keySet()) {
//            Way way = mapReader.ways.get(key);
//            if (way != null && !keyBlacklist.contains(key)) {
//                System.out.println("Drawing way " + key);
//                builder.addWay(way, originX, originY, scale);
//            } else if (way != null && keyBlacklist.contains(key)) {
//                System.out.println("Skipping way " + key + ": \n");
//                for (Point p : way.toPoints(originX, originY, scale)) {
//                    System.out.println("\t" + p.x + "f, " + p.y + "f, " + p.z + "f,");
//                }
//            }
//        }
    }

    public void handleTouchDrag(float eventX, float eventY) {
        // Get screen size
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels;

        float newOriginX = oldX - eventX / scale / width * 2;
        float newOriginY = oldY + eventY / scale / width * 2;
        originX = newOriginX;
        originY = newOriginY;
    }

    public void handleTouchPress(float eventX, float eventY) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels;

        oldX = originX + eventX / scale / width * 2;
        oldY = originY - eventY / scale / width * 2;
    }

    public void handleZoom(List<Float> eventXs, List<Float> eventYs) {
        if (eventXs.size() != 2 || eventYs.size() != 2) {
            return;
        }
        float newDistance = (float) Math.sqrt(Math.pow(eventXs.get(0) - eventXs.get(1), 2) + Math.pow(eventYs.get(0) - eventYs.get(1), 2));
        float delta = oldDistance > 0 ? newDistance - oldDistance : 0;
        oldDistance = newDistance;
        scale += delta;
//        System.out.println("Zooming delta " + delta);
    }

    public void handleResetZoom() {
        oldDistance = 0;
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
//            scale = height / mapReader.height;
        } else {
            // Portrait or square
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f);
//            scale = width / mapReader.height;
        }

//        Matrix.scaleM(projectionMatrix, 0, 3f, 3f, 1f);
//        Matrix.translateM(projectionMatrix, 0, 1f, 0f, 0f);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        // Clear the rendering surface.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_STENCIL_TEST);

//        builder.draw(colorProgram, projectionMatrix);

        VertexData.resetRandom();

        Polygon rv = StrokeGenerator.generateStroke(new LineStrip(vertices1), 10, 0.1f);
        TriangleStrip triangles = new TriangleStrip(rv.points);
        TriangleStrip single = new TriangleStrip(vertices1);
        triangles.points.addAll(single.points);

//        float[] waysVertexData = triangles.size() > 0 ? new float[triangles.get(0).toVertexData().length * triangles.size()] : new float[0];
//        for (int i = 0; i < triangles.size(); i++) {
//            System.arraycopy(triangles.get(i).toVertexData(), 0, waysVertexData, i * triangles.get(i).toVertexData().length, triangles.get(i).toVertexData().length);
//        }

        int uMVPLocation = colorProgram.getUniformLocation(ColorShaderProgram.U_MATRIX);

        VertexArray vertexArray = new VertexArray(triangles.toVertexData(0, 0, 0, 1));

        int strideInElements = VertexData.getTotalComponentCount();
        for (VertexData.VertexAttrib attrib : VertexData.getVertexAttribs()) {
            int location = colorProgram.getAttributeLocation(attrib.name);
            vertexArray.setVertexAttribPointer(attrib.offset, location, attrib.count, strideInElements);
        }
        colorProgram.useProgram();
        GLES20.glUniformMatrix4fv(uMVPLocation, 1, false, projectionMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, triangles.points.size() - single.points.size());
        GLES20.glDrawArrays(GLES20.GL_POINTS, triangles.points.size() - single.points.size(), single.points.size());
    }
}
