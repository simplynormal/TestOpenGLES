package com.hcmut.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import androidx.annotation.NonNull;

import com.hcmut.test.data.Framebuffer;
import com.hcmut.test.geometry.BoundBox;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.Vector;
import com.hcmut.test.geometry.equation.LineEquation;
import com.hcmut.test.geometry.equation.LineEquation3D;
import com.hcmut.test.geometry.equation.Plane;
import com.hcmut.test.mapnik.StyleParser;
import com.hcmut.test.object.FullScreenQuad;
import com.hcmut.test.object.MapView;
import com.hcmut.test.programs.FrameShaderProgram;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.programs.TextShaderProgram;
import com.hcmut.test.programs.TextSymbShaderProgram;
import com.hcmut.test.remote.BaseResponse;
import com.hcmut.test.remote.LayerRequest;
import com.hcmut.test.remote.LayerResponse;
import com.hcmut.test.remote.RetrofitClient;
import com.hcmut.test.utils.Config;

import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    private static final List<Point> SCREEN_QUAD = new ArrayList<>(4) {
        {
            add(new Point(-0.8f, -0.95f));
            add(new Point(0.8f, -0.95f));
            add(new Point(0.8f, 0.75f));
            add(new Point(-0.8f, 0.75f));
        }
    };
    private Point oldPos = null;
    private List<Point> oldPosList;
    private static boolean TEST_DRAWN = false;

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

        frameShaderProgram = new FrameShaderProgram(config);

        config.setColorShaderProgram(new ColorShaderProgram(config, projectionMatrix, modelViewMatrix));
        config.setTextShaderProgram(new TextShaderProgram(config, projectionMatrix, modelViewMatrix));
//        config.setPointTextShaderProgram(new PointTextShaderProgram(config, projectionMatrix, modelViewMatrix));
//        config.setLineTextShaderProgram(new LineTextShaderProgram(config, projectionMatrix, modelViewMatrix));
        config.setTextSymbShaderProgram(new TextSymbShaderProgram(config, projectionMatrix, modelViewMatrix));
        config.setFrameShaderProgram(frameShaderProgram);

//        minLon = 106.73101f;
//        maxLon = 106.73298f;
//        minLat = 10.73037f;
//        maxLat = 10.73137f;

//        minLon = 106.73603f;
//        maxLon = 106.74072f;
//        minLat = 10.73122f;
//        maxLat = 10.73465f;

//        minLon = 106.71410f;
//        maxLon = 106.72421f;
//        minLat = 10.72307f;
//        maxLat = 10.72860f;

        minLon = 106.7000f;
        maxLon = 106.7202f;
        minLat = 10.7382f;
        maxLat = 10.7492f;

//        minLon = 106.7187f;
//        maxLon = 106.7401f;
//        minLat = 10.7237f;
//        maxLat = 10.7350f;


        float originX = (minLon + maxLon) / 2;
        float originY = (minLat + maxLat) / 2;
        config.setOriginFromWGS84(originX, originY);
        config.setScaleDenominator(1000);
    }

    void read() {
        StyleParser styleParser;
        try {
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
        RetrofitClient.THREAD_POOL_EXECUTOR.execute(this::request);
//        request();
        Test.test();
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to fill the entire surface.
        GLES20.glViewport(0, 0, width, height);
        Matrix.frustumM(projectionMatrix, 0, -width / (float) height, width / (float) height, -1f, 1f, 1f, 10f);
        Matrix.setLookAtM(modelViewMatrix, 0, 0f, -2f, 2f, 0f, 0, 0f, 0f, 1f, 0f);

        config.addListener(this::onTransform);
        onTransform(config, Set.of(Config.Property.SCALE, Config.Property.ROTATION, Config.Property.TRANSLATION));

        framebuffer = new Framebuffer(width, height);
        fullScreenQuad = new FullScreenQuad(frameShaderProgram);
    }

    void onTransform(Config config, Set<Config.Property> properties) {
        if (!properties.contains(Config.Property.SCALE) && !properties.contains(Config.Property.ROTATION) && !properties.contains(Config.Property.TRANSLATION)) {
            return;
        }

        List<Point> worldQuad = screenToWorld(SCREEN_QUAD);
        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;

        for (Point point : worldQuad) {
            minX = Math.min(minX, point.x);
            maxX = Math.max(maxX, point.x);
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }

        config.setWorldBoundBox(new BoundBox(minX, minY, maxX, maxY));
//        System.out.println(config.getWorldBoundBox());
    }

    private void divideByW(float[] vector) {
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
    }

    private float[] getInverseAllMatrix() {
        float[] transformMatrix = getTransformMatrix();
        float[] allMatrix = new float[16];
        float[] inverseAllMatrix = new float[16];
        Matrix.multiplyMM(allMatrix, 0, modelViewMatrix, 0, transformMatrix, 0);
        Matrix.multiplyMM(allMatrix, 0, projectionMatrix, 0, allMatrix, 0);
        Matrix.invertM(inverseAllMatrix, 0, allMatrix, 0);
        return inverseAllMatrix;
    }

    private List<Point> screenToWorld(List<Point> points) {
        float[] inverseAllMatrix = getInverseAllMatrix();

        List<Point> result = new ArrayList<>();
        Plane plane = new Plane(new Vector(0, 0, 1), new Point(0, 0));
        for (Point p : points) {
            final float[] nearPointNdc = {p.x, p.y, -1, 1};
            final float[] farPointNdc = {p.x, p.y, 1, 1};

            final float[] nearPointWorld = new float[4];
            final float[] farPointWorld = new float[4];

            Matrix.multiplyMV(
                    nearPointWorld, 0, inverseAllMatrix, 0, nearPointNdc, 0);
            Matrix.multiplyMV(
                    farPointWorld, 0, inverseAllMatrix, 0, farPointNdc, 0);
            divideByW(nearPointWorld);
            divideByW(farPointWorld);

            Point nearPointRay = new Point(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2]);

            Point farPointRay = new Point(farPointWorld[0], farPointWorld[1], farPointWorld[2]);

            LineEquation3D lineEquation = new LineEquation3D(nearPointRay, farPointRay);

            Point intersectionPoint = lineEquation.intersectPlane(plane);
            result.add(intersectionPoint);
        }

        return result;
    }

    private LineEquation convertNormalized2DPointToLine(
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

        return new LineEquation(nearPointRay, farPointRay);
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
        return screenToWorld(Collections.singletonList(p)).get(0);
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
        if (oldPos == null) return;
        Point newPos = pixelScreenToCoord(x, y);
        System.out.println("actionMove: " + newPos);
        translateMap(oldPos, newPos);
    }

    public void actionUp(float x, float y) {
        oldPos = null;
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
        oldPos = null;
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

        // Rotate
        Vector vecX = new Vector(1, 0);
        float oldAngle = new Vector(oldP1, oldP2).signedAngle(vecX);
        float newAngle = new Vector(p1, p2).signedAngle(vecX);
        float angle = (float) Math.toDegrees(oldAngle - newAngle);

        // Translate
        Vector translation = new Vector(oldCenter, center);

        config.setTransform(scale * config.getScale(), (angle + config.getRotation() + 360) % 360, translation.add(config.getTranslation()));
    }

    public void actionPointerUp(List<Float> x, List<Float> y) {
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
//        framebuffer.bind();
//        GLES20.glViewport(0, 0, framebuffer.getWidth(), framebuffer.getHeight());
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);

        mapView.draw();

//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//        GLES20.glViewport(0, 0, config.getWidth(), config.getHeight());
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//        fullScreenQuad.draw(framebuffer.getTextureId());
    }
}
