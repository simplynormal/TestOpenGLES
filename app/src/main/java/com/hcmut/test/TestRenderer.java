package com.hcmut.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import androidx.annotation.NonNull;

import com.hcmut.test.data.Framebuffer;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.Ray;
import com.hcmut.test.geometry.Vector;
import com.hcmut.test.mapnik.symbolizer.LineSymbolizer;
import com.hcmut.test.mapnik.StyleParser;
import com.hcmut.test.mapnik.symbolizer.PolygonSymbolizer;
import com.hcmut.test.mapnik.symbolizer.SymMeta;
import com.hcmut.test.mapnik.symbolizer.TextSymbolizer;
import com.hcmut.test.object.FullScreenQuad;
import com.hcmut.test.object.MapView;
import com.hcmut.test.programs.FrameShaderProgram;
import com.hcmut.test.programs.LineTextShaderProgram;
import com.hcmut.test.programs.PointTextShaderProgram;
import com.hcmut.test.reader.MapReader;
import com.hcmut.test.osm.Way;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.programs.TextShaderProgram;
import com.hcmut.test.remote.BaseResponse;
import com.hcmut.test.remote.LayerRequest;
import com.hcmut.test.remote.LayerResponse;
import com.hcmut.test.utils.Config;

import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TestRenderer implements GLSurfaceView.Renderer {
    private FrameShaderProgram frameShaderProgram;
    private MapView mapView;
    private final Config config;
    private Framebuffer framebuffer;
    private FullScreenQuad fullScreenQuad;
    private final float[] vertices = {
            0f, 0f, 0f,
            0.5f, 0f, 0f,
            0.5f, 0.5f, 0f,
            0f, 0.5f, 0f,
            0f, 0f, 0f,
    };
    private final float[] vertices1 = {
            0f, 0f, 0f,
            0.15f, 0.15f, 0f,
            0f, 0.5f, 0f,
    };
    private final float[] vertices2 = {
            0f, 0f, 0f,
            0f, 0.5f, 0f,
            -0.5f, 0.5f, 0f,
    };
    private final float[] projectionMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private Point oldPos;
    private List<Point> oldPosList;

    private float minLon = 0;
    private float maxLon = 0;
    private float minLat = 0;
    private float maxLat = 0;


    public TestRenderer(Context context) {
        config = new Config(context);
    }

    public void initOpenGL() {
        int width = config.context.getResources().getDisplayMetrics().widthPixels;
        int height = config.context.getResources().getDisplayMetrics().heightPixels;
        config.setWidthHeight(width, height);

        ColorShaderProgram colorProgram = new ColorShaderProgram(config, projectionMatrix, modelViewMatrix);
        PointTextShaderProgram pointTextProgram = new PointTextShaderProgram(config, projectionMatrix, modelViewMatrix);
        LineTextShaderProgram lineTextProgram = new LineTextShaderProgram(config, projectionMatrix, modelViewMatrix);
        TextShaderProgram textProgram = new TextShaderProgram(config, projectionMatrix, modelViewMatrix);
        frameShaderProgram = new FrameShaderProgram(config);

        config.setColorShaderProgram(colorProgram);
        config.setTextShaderProgram(textProgram);
        config.setPointTextShaderProgram(pointTextProgram);
        config.setLineTextShaderProgram(lineTextProgram);
        config.setFrameShaderProgram(frameShaderProgram);

//        minLon = 106.73603f;
//        maxLon = 106.74072f;
//        minLat = 10.73122f;
//        maxLat = 10.73465f;

//        minLon = 106.71410f;
//        maxLon = 106.72421f;
//        minLat = 10.72307f;
//        maxLat = 10.72860f;

        minLon = 106.7187f;
        maxLon = 106.7401f;
        minLat = 10.7237f;
        maxLat = 10.7350f;

        float originX = (minLon + maxLon) / 2;
        float originY = (minLat + maxLat) / 2;
        config.setOriginFromWGS84(originX, originY);
        config.setScaleDenominator(1000);
    }

    void read() {
//        MapReader mapReader;
        StyleParser styleParser;
        try {
//            mapReader = new MapReader(config.context, R.raw.map1);
//            mapReader.setBounds(minLon, maxLon, minLat, maxLat);
//            mapReader.read();
            styleParser = new StyleParser(config, R.raw.mapnik);
            styleParser.read();
        } catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        }

        mapView = new MapView();
        mapView.setLayers(styleParser.layers);
//        mapView.validateWays();
    }

    void request() {
        System.out.println("before postGetLayer");
        LayerRequest layerRequest = new LayerRequest(minLon, maxLon, minLat, maxLat);
        layerRequest.post(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<LayerResponse>> call, @NonNull Response<BaseResponse<LayerResponse>> response) {
                if (response.body() == null) {
                    System.err.println("response.body() == null");
                    return;
                }
                LayerResponse layerResponse = response.body().getData();
                System.out.println("layerResponse = " + layerResponse);
                mapView.validateResponse(layerResponse);
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<LayerResponse>> call, @NonNull Throwable t) {
                System.err.println("onFailure: " + t.getMessage());
            }
        });
        System.out.println("after postGetLayer");
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        GLES20.glClearColor(0.95f, 0.94f, 0.91f, 1f);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        initOpenGL();
        read();
        request();
        Test.test();
    }

    private void divideByW(float[] vector) {
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
    }

    private Ray convertNormalized2DPointToRay(
            float normalizedX, float normalizedY) {
        float[] transformMatrix = getTransformMatrix();
        // We'll convert these normalized device coordinates into world-space
        // coordinates. We'll pick a point on the near and far planes, and draw a
        // line between them. To do this transform, we need to first multiply by
        // the inverse matrix, and then we need to undo the perspective divide.
        final float[] nearPointNdc = {normalizedX, normalizedY, -1, 1};
        final float[] farPointNdc = {normalizedX, normalizedY, 1, 1};

        final float[] nearPointWorld = new float[4];
        final float[] farPointWorld = new float[4];

        float[] allMatrix = new float[16];
        float[] invertedAllMatrix = new float[16];
        Matrix.multiplyMM(allMatrix, 0, modelViewMatrix, 0, transformMatrix, 0);
        Matrix.multiplyMM(allMatrix, 0, projectionMatrix, 0, allMatrix, 0);
        Matrix.invertM(invertedAllMatrix, 0, allMatrix, 0);
        Matrix.multiplyMV(
                nearPointWorld, 0, invertedAllMatrix, 0, nearPointNdc, 0);
        Matrix.multiplyMV(
                farPointWorld, 0, invertedAllMatrix, 0, farPointNdc, 0);

        // Why are we dividing by W? We multiplied our vector by an inverse
        // matrix, so the W value that we end up is actually the *inverse* of
        // what the projection matrix would create. By dividing all 3 components
        // by W, we effectively undo the hardware perspective divide.
        divideByW(nearPointWorld);
        divideByW(farPointWorld);

        // We don't care about the W value anymore, because our points are now
        // in world coordinates.
        Point nearPointRay = new Point(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2]);

        Point farPointRay = new Point(farPointWorld[0], farPointWorld[1], farPointWorld[2]);

        return new Ray(nearPointRay, new Vector(nearPointRay, farPointRay));
    }

    private float[] getTransformMatrix() {
        float[] transformMatrix = new float[16];
        Matrix.setIdentityM(transformMatrix, 0);

        float scale = config.getScale();
        float rotation = config.getRotation();
        Vector translation = config.getTranslation();

        Matrix.scaleM(transformMatrix, 0, scale, scale, 1);
        Matrix.rotateM(transformMatrix, 0, rotation, 0, 0, 1);
        Matrix.translateM(transformMatrix, 0, translation.x, translation.y, 0);

        return transformMatrix;
    }

    private Point pixelScreenToProjScreen(float x, float y) {
        // scale to screen then scale to [-1, 1]
        return new Point(x / config.getWidth() * 2 - 1, 1 - y / config.getHeight() * 2);
    }

    private Point pixelScreenToCoord(float x, float y) {
        Point pos = pixelScreenToProjScreen(x, y);
        return fromProjScreenToCoord(pos);
    }

    private Point fromProjScreenToCoord(Point p) {
        float[] transformMatrix = getTransformMatrix();
        float[] inverseProjection = new float[16];
        Matrix.invertM(inverseProjection, 0, projectionMatrix, 0);
        float[] inverseModelView = new float[16];
        Matrix.invertM(inverseModelView, 0, modelViewMatrix, 0);
        float[] inverseTransform = new float[16];
        Matrix.invertM(inverseTransform, 0, transformMatrix, 0);

        float[] vector = new float[]{p.x, p.y, p.z, 1};
        float[] resultVec = new float[4];
        Matrix.multiplyMV(resultVec, 0, inverseProjection, 0, vector, 0);
        Matrix.multiplyMV(resultVec, 0, inverseModelView, 0, resultVec, 0);
        Matrix.multiplyMV(resultVec, 0, inverseTransform, 0, resultVec, 0);

        if (resultVec[3] != 0) {
            resultVec[0] /= resultVec[3];
            resultVec[1] /= resultVec[3];
            resultVec[2] /= resultVec[3];
        }

        return new Point(resultVec[0], resultVec[1], resultVec[2]);
    }

    private void translateMap(Point oldPos, Point newPos) {
        Vector translation = new Vector(oldPos, newPos);
        translation = translation.add(config.getTranslation());
        config.setTranslation(translation);
    }

    public void actionDown(float x, float y) {
        oldPos = pixelScreenToCoord(x, y);
        System.out.println("actionDown: " + oldPos);
    }

    public void actionMove(float x, float y) {
        Point newPos = pixelScreenToCoord(x, y);
        System.out.println("actionMove: " + newPos);
        translateMap(oldPos, newPos);
    }

    public void actionUp(float x, float y) {
    }

    @SuppressLint("NewApi")
    public void actionPointerDown(List<Float> x, List<Float> y) {
        if (x.size() != 2) return;
        oldPosList = new ArrayList<>() {{
            for (int i = 0; i < x.size(); i++) {
                add(pixelScreenToCoord(x.get(i), y.get(i)));
            }
        }};
        System.out.println("actionPointerDown: " + oldPosList);
    }

    @SuppressLint("NewApi")
    public void actionPointerMove(List<Float> x, List<Float> y) {
        if (x.size() != 2) return;
        List<Point> newPosList = new ArrayList<>() {{
            for (int i = 0; i < x.size(); i++) {
                add(pixelScreenToCoord(x.get(i), y.get(i)));
            }
        }};

        Point oldP1 = oldPosList.get(0);
        Point oldP2 = oldPosList.get(1);
        Point p1 = newPosList.get(0);
        Point p2 = newPosList.get(1);
        Point oldCenter = oldP1.midPoint(oldP2);
        Point center = p1.midPoint(p2);

        // Scale
        float oldDistance = oldP1.distance(oldP2);
        float newDistance = p1.distance(p2);
        float scale = newDistance / oldDistance;
        config.setScale(scale * config.getScale());

        // Rotate
        Vector vecX = new Vector(1, 0);
        float oldAngle = new Vector(oldP1, oldP2).signedAngle(vecX);
        float newAngle = new Vector(p1, p2).signedAngle(vecX);
        float angle = (float) Math.toDegrees(oldAngle - newAngle);
        config.setRotation((angle + config.getRotation() + 360) % 360);

        // Translate
        translateMap(oldCenter, center);

//        System.out.println("oldAngle: " + Math.toDegrees(oldAngle) + ", newAngle: " + Math.toDegrees(newAngle) + ", angle: " + angle);
    }

    public void actionPointerUp(List<Float> x, List<Float> y) {
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to fill the entire surface.
        GLES20.glViewport(0, 0, width, height);
        Matrix.frustumM(projectionMatrix, 0, -width / (float) height, width / (float) height, -1f, 1f, 1f, 10f);
        Matrix.setLookAtM(modelViewMatrix, 0, 0f, 0f, 2f, 0f, 0, 0f, 0f, 1f, 0f);

        framebuffer = new Framebuffer(width, height);
        fullScreenQuad = new FullScreenQuad(frameShaderProgram);
    }

    void drawLineSymbolizer(float[] chosenVertices, LineSymbolizer lineSymbolizer) {
        Way way = new Way(chosenVertices);
        SymMeta drawables = lineSymbolizer.toDrawable(way);
        drawables.draw(config);
    }

    void testLineSymbolizer() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        float[] chosenVertices = vertices1;

        float[] first = new float[]{
                chosenVertices[0], chosenVertices[1], chosenVertices[2],
        };

        for (int i = 0; i < chosenVertices.length; i += 3) {
            chosenVertices[i] -= first[0];
            chosenVertices[i + 1] -= first[1];
            chosenVertices[i + 2] -= first[2];
        }

        LineSymbolizer lineSymbolizer = new LineSymbolizer(
                config,
                "10",
                "#ff0000",
                null,
                "butt",
                null,
                null,
                null
        );

        // offset chosenVertices by 0
        float[] chosenVertices2 = new float[chosenVertices.length];
        System.arraycopy(chosenVertices, 0, chosenVertices2, 0, chosenVertices.length);
        for (int i = 0; i < chosenVertices2.length; i += 3) {
            chosenVertices2[i] += 0;
        }
        LineSymbolizer lineSymbolizer2 = new LineSymbolizer(
                config,
                "10",
                "#00ff00",
                null,
                "round",
                null,
                null,
                "-15"
        );

        // offset chosenVertices by 0.2
        float[] chosenVertices3 = new float[chosenVertices.length];
        System.arraycopy(chosenVertices, 0, chosenVertices3, 0, chosenVertices.length);
        for (int i = 0; i < chosenVertices3.length; i += 3) {
            chosenVertices3[i] += 0.2;
        }
        LineSymbolizer lineSymbolizer3 = new LineSymbolizer(
                config,
                "10",
                "#0000ff",
                null,
                "square",
                null,
                null,
                null
        );

        // offset chosenVertices by -0.5
        float[] chosenVertices4 = new float[vertices2.length];
        System.arraycopy(vertices2, 0, chosenVertices4, 0, vertices2.length);
        for (int i = 0; i < chosenVertices4.length; i += 3) {
            chosenVertices4[i] -= 0.5;
        }
        LineSymbolizer lineSymbolizer4 = new LineSymbolizer(
                config,
                "10",
                "#0000ff",
                "20, 15",
                "round",
                null,
                null,
                null
        );

        drawLineSymbolizer(chosenVertices, lineSymbolizer);
        drawLineSymbolizer(chosenVertices2, lineSymbolizer2);
        drawLineSymbolizer(chosenVertices3, lineSymbolizer3);
        drawLineSymbolizer(chosenVertices4, lineSymbolizer4);
    }

    //
    void drawTextSymbolizer(float[] chosenVertices, TextSymbolizer textSymbolizer) {
        Way way = new Way(chosenVertices);
        way.tags.put("daw", "replaced");
        SymMeta drawables = textSymbolizer.toDrawable(way);
        drawables.draw(config);
    }

    void drawPolygonSymbolizer(float[] chosenVertices, PolygonSymbolizer polygonSymbolizer) {
        Way way = new Way(chosenVertices);
        SymMeta drawables = polygonSymbolizer.toDrawable(way);
        drawables.draw(config);
    }

    void testTextSymbolizer() {
        float[] chosenVertices = vertices1;

        float[] first = new float[]{
                chosenVertices[0], chosenVertices[1], chosenVertices[2],
        };

        for (int i = 0; i < chosenVertices.length; i += 3) {
            chosenVertices[i] -= first[0];
            chosenVertices[i + 1] -= first[1];
            chosenVertices[i + 2] -= first[2];
        }

        TextSymbolizer textSymbolizer = new TextSymbolizer(
                config,
                "'abc'",
                "0",
                "0",
                "0",
                "12",
                "0",
                "70",
                null,
                null,
                "line",
                null,
                null,
                null,
                null,
                null,
                "15",
                "rgba(255, 255, 255, 0.6)",
                "1"
        );

        LineSymbolizer lineSymbolizer = new LineSymbolizer(
                config,
                "15",
                "#ffffff",
                null,
                "round",
                null,
                null,
                "0"
        );

        TextSymbolizer textSymbolizer2 = new TextSymbolizer(
                config,
                "'a b c'",
                "0",
                "0",
                "0",
                "12",
                "0",
                "70",
                null,
                null,
                "point",
                null,
                null,
                null,
                "0",
                null,
                "11",
                "rgba(255, 255, 255, 0.6)",
                "1"
        );

        PolygonSymbolizer polygonSymbolizer = new PolygonSymbolizer(config, "#00ff00", "1");

        drawLineSymbolizer(chosenVertices, lineSymbolizer);
        drawTextSymbolizer(chosenVertices, textSymbolizer);
//        drawPolygonSymbolizer(vertices, polygonSymbolizer);
//        drawTextSymbolizer(vertices, textSymbolizer2);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
//        mFramebuffer.bind();
//        GLES20.glViewport(0, 0, mFramebuffer.getWidth(), mFramebuffer.getHeight());
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);

//        testLineSymbolizer();
//        testTextSymbolizer();
        mapView.draw();

//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//        GLES20.glViewport(0, 0, config.getWidth(), config.getHeight());
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//        mFullScreenQuad.draw(mFramebuffer.getTextureId());
    }
}
