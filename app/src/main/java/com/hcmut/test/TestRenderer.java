package com.hcmut.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;

import com.hcmut.test.algorithm.CoordinateTransform;
import com.hcmut.test.algorithm.TileSystem;
import com.hcmut.test.geometry.BoundBox;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.Vector;
import com.hcmut.test.geometry.equation.LineEquation;
import com.hcmut.test.geometry.equation.LineEquation3D;
import com.hcmut.test.geometry.equation.Plane;
import com.hcmut.test.mapnik.StyleParser;
import com.hcmut.test.object.MapView;
import com.hcmut.test.object.UserIcon;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.programs.TextSymbShaderProgram;
import com.hcmut.test.remote.BaseResponse;
import com.hcmut.test.remote.LayerRequest;
import com.hcmut.test.remote.LayerResponse;
import com.hcmut.test.utils.Config;

import org.osgeo.proj4j.ProjCoordinate;
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
            add(new Point(0.95f, 0.75f));
            add(new Point(-0.95f, 0.75f));
        }
    };
    private Point oldPos = null;
    private List<Point> oldPosList;
    private float curLon = 0;
    private float curLat = 0;
    private UserIcon userIcon;

    public TestRenderer(Context context) {
        config = new Config(context);
    }

    private void initOpenGL() {
        int width = config.context.getResources().getDisplayMetrics().widthPixels;
        int height = config.context.getResources().getDisplayMetrics().heightPixels;
        config.setWidthHeight(width, height);

        config.setColorShaderProgram(new ColorShaderProgram(config, projectionMatrix, modelViewMatrix));
        config.setTextSymbShaderProgram(new TextSymbShaderProgram(config, projectionMatrix, modelViewMatrix));

//        float minLon = 106.73101f;
//        float maxLon = 106.73298f;
//        float minLat = 10.73037f;
//        float maxLat = 10.73137f;

//        float minLon = 106.73603f;
//        float maxLon = 106.74072f;
//        float minLat = 10.73122f;
//        float maxLat = 10.73465f;

//        float minLon = 106.71410f;
//        float maxLon = 106.72421f;
//        float minLat = 10.72307f;
//        float maxLat = 10.72860f;

        float minLon = 106.7000f;
        float maxLon = 106.7202f;
        float minLat = 10.7382f;
        float maxLat = 10.7492f;

//        float minLon = 106.7187f;
//        float maxLon = 106.7401f;
//        float minLat = 10.7237f;
//        float maxLat = 10.7350f;

//        originX = (minLon + maxLon) / 2;
//        originY = (minLat + maxLat) / 2;
//        curLon = 106.65902182459831f;
//        curLat = 10.771236883015353f;
        // 106.65609, 10.780013
        curLon = 106.65609f;
        curLat = 10.780013f;
        config.setOriginFromWGS84(curLon, curLat);
        config.setScaleDenominator(1066);

        float scaled = CoordinateTransform.getScalePixel(config.getScaleDenominator()) * config.getLengthPerPixel();
        ProjCoordinate p = CoordinateTransform.wgs84ToWebMercator(curLat, curLon);
        Point origin = new Point((float) p.x, (float) p.y).transform(config.getOriginX(), config.getOriginY(), scaled);
        Log.d(TAG, "initOpenGL: origin: " + origin);
        userIcon = new UserIcon(config, origin);
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
        Matrix.setLookAtM(modelViewMatrix, 0, 0f, -2f, 2f, 0f, 0, 0f, 0f, 1f, 0f);

        config.addListener(this::onTransform);
        onTransform(config, Set.of(Config.Property.SCALE, Config.Property.ROTATION, Config.Property.TRANSLATION));
    }

    void transformOrigin(BoundBox userBbox) {
        float scaled = CoordinateTransform.getScalePixel(config.getScaleDenominator()) * config.getLengthPerPixel();
        float[] transformMatrix = new float[16];
        Matrix.setIdentityM(transformMatrix, 0);

        float scale = 1 / config.getScale();
        float rotation = 360 - config.getRotation();
        Vector translation = config.getTranslation().negate();

        Matrix.translateM(transformMatrix, 0, translation.x, translation.y, 0);
        Matrix.rotateM(transformMatrix, 0, rotation, 0, 0, 1);
        Matrix.scaleM(transformMatrix, 0, scale, scale, 1);

        float[] originTransformed = new float[4];
        Point user = screenToWorld(List.of(new Point(0, -0.3f))).get(0);
        Matrix.multiplyMV(originTransformed, 0, transformMatrix, 0, new float[]{user.x, user.y, 0, 1}, 0);

        float x = user.x / scaled + config.getOriginX();
        float y = user.y / scaled + config.getOriginY();
        float bboxMinX = userBbox.minX / scaled + config.getOriginX();
        float bboxMinY = userBbox.minY / scaled + config.getOriginY();
        float bboxMaxX = userBbox.maxX / scaled + config.getOriginX();
        float bboxMaxY = userBbox.maxY / scaled + config.getOriginY();

        ProjCoordinate transformedPoint = CoordinateTransform.webMercatorToWgs84(x, y);
        curLon = (float) transformedPoint.x;
        curLat = (float) transformedPoint.y;
        transformedPoint = CoordinateTransform.webMercatorToWgs84(bboxMinX, bboxMinY);
        Point bboxMin = new Point((float) transformedPoint.x, (float) transformedPoint.y);
        transformedPoint = CoordinateTransform.webMercatorToWgs84(bboxMaxX, bboxMaxY);
        Point bboxMax = new Point((float) transformedPoint.x, (float) transformedPoint.y);
        float radius = bboxMin.distance(bboxMax) / 2;
        Log.d(TAG, "transformOrigin: " + curLon + ", " + curLat);

//        userIcon.relocate(new Point(user.x, user.y));
        mapView.setCurLocation(curLon, curLat, radius);
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

        BoundBox boundBox = new BoundBox(minX, minY, maxX, maxY);
        config.setWorldBoundBox(boundBox);

        transformOrigin(boundBox);
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
        System.out.println("actionDown: " + oldPos);
    }

    public void actionMove(float x, float y) {
        if (oldPos == null) return;
        Point newPos = pixelScreenToCoord(x, y);
        System.out.println("actionMove: " + newPos);
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
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);

        mapView.draw();
        float scaled = CoordinateTransform.getScalePixel(config.getScaleDenominator()) * config.getLengthPerPixel();
        ProjCoordinate p = CoordinateTransform.wgs84ToWebMercator(curLat, curLon);
        Point userLocation = new Point((float) p.x, (float) p.y).transform(config.getOriginX(), config.getOriginY(), scaled);
        userIcon.relocate(userLocation);
        userIcon.draw();
    }
}
