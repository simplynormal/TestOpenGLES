package com.hcmut.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.hcmut.test.algorithm.CoordinateTransform;
import com.hcmut.test.data.VertexArray;
import com.hcmut.test.geometry.BoundBox;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.Vector;
import com.hcmut.test.geometry.equation.LineEquation3D;
import com.hcmut.test.geometry.equation.Plane;
import com.hcmut.test.mapnik.StyleParser;
import com.hcmut.test.object.MapView;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.programs.TextSymbShaderProgram;
import com.hcmut.test.utils.Config;

import org.osgeo.proj4j.ProjCoordinate;
import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TestRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = TestRenderer.class.getSimpleName();
    private MapView mapView;
    private final Config config;
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
            add(new Point(-0.95f, -0.95f));
            add(new Point(0.95f, -0.95f));
            add(new Point(-0.95f, 0.85f));
            add(new Point(0.95f, 0.85f));
        }
    };
    private Point oldPos = null;
    private List<Point> oldPosList;
    private double curLon;
    private double curLat;
    private double destLon;
    private double destLat;
    private Point bboxMin;
    private Point bboxMax;

    public TestRenderer(Context context) {
        config = new Config(context);
    }

    private void initOpenGL() {
        int width = config.context.getResources().getDisplayMetrics().widthPixels;
        int height = config.context.getResources().getDisplayMetrics().heightPixels;
        config.setWidthHeight(width, height);

        config.setColorShaderProgram(new ColorShaderProgram(config, projectionMatrix, modelViewMatrix));
        config.setTextSymbShaderProgram(new TextSymbShaderProgram(config, projectionMatrix, modelViewMatrix));

        curLon = 106.70668;
        curLat = 10.729151;

//        float curLon = 106.65609;
//        float curLat = 10.780013;

        destLat = 10.780349396189212;
        destLon = 106.65451236069202;

        config.setOriginFromWGS84((float) curLon, (float) curLat);
        config.setScaleDenominator(1066);

        float scaled = CoordinateTransform.getScalePixel(config.getScaleDenominator()) * config.getLengthPerPixel();
        ProjCoordinate p = CoordinateTransform.wgs84ToWebMercator(curLat, curLon);
        Point origin = new Point((float) p.x, (float) p.y).transform(config.getOriginX(), config.getOriginY(), scaled);
        Log.d(TAG, "initOpenGL: origin: " + origin);
    }

    void read() {
        StyleParser styleParser;
        try {
            styleParser = new StyleParser(config, R.raw.mapnik);
            styleParser.read();
        } catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        }

        mapView = new MapView(config);
        mapView.setLayers(styleParser.layers);

        mapView.setRoute(curLon, curLat, destLon, destLat);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        GLES20.glClearColor(0.95f, 0.94f, 0.91f, 1f);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        initOpenGL();
        read();
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to fill the entire surface.
        GLES20.glViewport(0, 0, width, height);
        Matrix.frustumM(projectionMatrix, 0, -width / (float) height, width / (float) height, -1f, 1f, 1f, 10f);
        Matrix.setLookAtM(modelViewMatrix, 0, 0f, -1.7f, 2f, 0f, 0, 0f, 0f, 1f, 0f);

        config.addListener(this::onTransform);
        onTransform(config, Set.of(Config.Property.SCALE, Config.Property.ROTATION, Config.Property.TRANSLATION));
    }

    void transformOrigin(List<Point> worldQuad) {
        float scaled = CoordinateTransform.getScalePixel(config.getScaleDenominator()) * config.getLengthPerPixel();
        Point user = screenToWorld(List.of(new Point(0, -0.3f))).get(0);

        float x = user.x / scaled + config.getOriginX();
        float y = user.y / scaled + config.getOriginY();

        ProjCoordinate transformedPoint = CoordinateTransform.webMercatorToWgs84(x, y);
        float curLon = (float) transformedPoint.x;
        float curLat = (float) transformedPoint.y;

        float bboxMinX = worldQuad.get(0).x / scaled + config.getOriginX();
        float bboxMinY = worldQuad.get(0).y / scaled + config.getOriginY();
        float bboxMaxX = worldQuad.get(3).x / scaled + config.getOriginX();
        float bboxMaxY = worldQuad.get(3).y / scaled + config.getOriginY();
        transformedPoint = CoordinateTransform.webMercatorToWgs84(bboxMinX, bboxMinY);
        bboxMin = new Point((float) transformedPoint.x, (float) transformedPoint.y);
        transformedPoint = CoordinateTransform.webMercatorToWgs84(bboxMaxX, bboxMaxY);
        bboxMax = new Point((float) transformedPoint.x, (float) transformedPoint.y);
        float radius = bboxMin.distance(bboxMax) / 2;
        Log.d(TAG, "transformOrigin: " + curLon + ", " + curLat + ", " + bboxMin + ", " + bboxMax + ", " + radius);

        mapView.setCurLocation(curLon, curLat, radius);
    }

    void onTransform(Config config, Set<Config.Property> properties) {
        if (!properties.contains(Config.Property.SCALE) && !properties.contains(Config.Property.ROTATION) && !properties.contains(Config.Property.TRANSLATION)) {
            return;
        }

        List<Point> worldQuad = screenToWorld(SCREEN_QUAD);
        transformOrigin(worldQuad);
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

    public void actionDown(float x, float y) {
        oldPos = pixelScreenToCoord(x, y);
        Log.d(TAG, "actionDown: " + oldPos);
    }

    public void actionMove(float x, float y) {
        if (oldPos == null) return;
        Point newPos = pixelScreenToCoord(x, y);
        Log.d(TAG, "actionMove: " + newPos);
        Vector translation = new Vector(oldPos, newPos);
        translation = translation.add(config.getTranslation());
        config.setTranslation(translation);
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
        Log.d(TAG, "actionPointerDown: " + oldPosList);
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
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);

        mapView.draw();
    }
}
