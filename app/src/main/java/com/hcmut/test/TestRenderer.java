package com.hcmut.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.hcmut.test.algorithm.BezierCurveAnimation;
import com.hcmut.test.algorithm.CoordinateTransform;
import com.hcmut.test.data.VertexArray;
import com.hcmut.test.geometry.BoundBox;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.Vector;
import com.hcmut.test.geometry.equation.LineEquation3D;
import com.hcmut.test.geometry.equation.Plane;
import com.hcmut.test.mapnik.StyleParser;
import com.hcmut.test.object.MapView;
import com.hcmut.test.object.UserIcon;
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
    private final MapView mapView;
    private final Config config;
    private final float[] projectionMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private static final List<Point> SCREEN_QUAD = new ArrayList<>(4) {
        {
            add(new Point(-1, -1));
            add(new Point(1, -1));
            add(new Point(-1, 1));
            add(new Point(1, 1));
        }
    };
    private Point oldPos = null;
    private List<Point> oldPosList;
    private double curLon;
    private double curLat;
    private final double destLon = 106.65451236069202;
    private double destLat = 10.780349396189212;
    private final BezierCurveAnimation<TransformationAnimation> animation = new BezierCurveAnimation<>(TransformationAnimation::scale);
    private TransformationAnimation currentAnimation = null;
    private final UserIcon userIcon;
    private boolean userLocationChanged = false;
    private boolean isNavFocus = true;

    private static class TransformationAnimation {
        public final float scale;
        public final float rotation;
        public final Vector translation;

        public TransformationAnimation(float scale, float rotation, Vector translation) {
            this.scale = scale;
            this.rotation = rotation;
            this.translation = translation;
        }

        private static float calculateCurrentAngle(float minAngle, float maxAngle, float currentValue) {
            float angleRange = maxAngle - minAngle;
            if (angleRange > 180 || angleRange < -180) {
                float localMinAngle = minAngle < 180 ? minAngle + 360 : minAngle;
                float localMaxAngle = maxAngle < 180 ? maxAngle + 360 : maxAngle;
                return (localMinAngle + (currentValue * (localMaxAngle - localMinAngle))) % 360;
            }
            return minAngle + (currentValue * (maxAngle - minAngle));
        }

        public static TransformationAnimation scale(List<TransformationAnimation> minMax, float t) {
            float scale = minMax.get(0).scale + (minMax.get(1).scale - minMax.get(0).scale) * t;
            float rotation = calculateCurrentAngle(minMax.get(0).rotation, minMax.get(1).rotation, t);
            Vector translation = minMax.get(0).translation.add(minMax.get(1).translation.sub(minMax.get(0).translation).mul(t));
            return new TransformationAnimation(scale, rotation, translation);
        }
    }

    public TestRenderer(Context context) {
        config = new Config(context);
        mapView = new MapView(config, destLon, destLat);
        userIcon = new UserIcon(config, new Point(0, 0));
    }

    private void initOpenGL() {
        int width = config.context.getResources().getDisplayMetrics().widthPixels;
        int height = config.context.getResources().getDisplayMetrics().heightPixels;
        Matrix.frustumM(projectionMatrix, 0, -width / (float) height, width / (float) height, -1f, 1f, 1f, 20f);
        Matrix.setLookAtM(modelViewMatrix, 0, 0f, -1.7f, 2f, 0f, 0, 0f, 0f, 1f, 0f);
        Matrix.translateM(modelViewMatrix, 0, modelViewMatrix, 0, 0, -1, 0);
        config.setWidthHeight(width, height);

        config.setColorShaderProgram(new ColorShaderProgram(config, projectionMatrix, modelViewMatrix));
        config.setTextSymbShaderProgram(new TextSymbShaderProgram(config, projectionMatrix, modelViewMatrix));

//        curLon = 106.7265605;
//        curLat = 10.7294012;

//        curLon = 106.72315;
//        curLat = 10.7268;

        curLon = NavTest.coords[1];
        curLat = NavTest.coords[0];

        config.setScaleDenominator(1000);
        config.setOriginFromWGS84((float) curLon, (float) curLat);
        userLocationChanged = true;
    }

    void read() {
        StyleParser styleParser;
        try {
            styleParser = new StyleParser(config, R.raw.mapnik);
            styleParser.read();
        } catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        }

        mapView.setLayers(styleParser.layers);

//        mapView.setRoute(curLon, curLat, destLon, destLat);
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
        Matrix.frustumM(projectionMatrix, 0, -width / (float) height, width / (float) height, -1f, 1f, 1f, 20f);
        Matrix.setLookAtM(modelViewMatrix, 0, 0f, -1.7f, 2f, 0f, 0, 0f, 0f, 1f, 0f);
        Matrix.translateM(modelViewMatrix, 0, modelViewMatrix, 0, 0, -1, 0);

        config.addListener(this::onTransform);
        onTransform(config, Set.of(Config.Property.SCALE, Config.Property.ROTATION, Config.Property.TRANSLATION));
    }

    void transformOrigin(List<Point> worldQuad) {
        float scaled = CoordinateTransform.getScalePixel(config.getScaleDenominator()) * config.getLengthPerPixel();

        float bboxMinX = worldQuad.get(0).x / scaled + config.getOriginX();
        float bboxMinY = worldQuad.get(0).y / scaled + config.getOriginY();
        float bboxMaxX = worldQuad.get(3).x / scaled + config.getOriginX();
        float bboxMaxY = worldQuad.get(3).y / scaled + config.getOriginY();
        ProjCoordinate transformedPoint = CoordinateTransform.webMercatorToWgs84(bboxMinX, bboxMinY);
        Point bboxMin = new Point((float) transformedPoint.x, (float) transformedPoint.y);
        transformedPoint = CoordinateTransform.webMercatorToWgs84(bboxMaxX, bboxMaxY);
        Point bboxMax = new Point((float) transformedPoint.x, (float) transformedPoint.y);
        float radius = bboxMin.distance(bboxMax) / 2;
//        Log.d(TAG, "transformOrigin: " + curLon + ", " + curLat + ", " + bboxMin + ", " + bboxMax + ", " + radius);

        mapView.setCurLocation((float) this.curLon, (float) this.curLat, radius);
    }

    void onTransform(Config config, Set<Config.Property> properties) {
        if (!properties.contains(Config.Property.SCALE) && !properties.contains(Config.Property.ROTATION) && !properties.contains(Config.Property.TRANSLATION)) {
            return;
        }

        List<Point> worldQuad = screenToWorld(SCREEN_QUAD);
        transformOrigin(worldQuad);
    }

    private Vector getTranslation() {
        if (animation.isRunning()) {
            return animation.getMaxValue().translation;
        }
        return config.getTranslation();
    }

    private float getRotation() {
        if (animation.isRunning()) {
            return animation.getMaxValue().rotation;
        }
        return config.getRotation();
    }

    private float getScale() {
        if (animation.isRunning()) {
            return animation.getMaxValue().scale;
        }
        return config.getScale();
    }

    private Vector moveTo(double lon, double lat) {
        float scaled = CoordinateTransform.getScalePixel(config.getScaleDenominator()) * config.getLengthPerPixel();
        Point origin = new Point(0, 0);
        ProjCoordinate p = CoordinateTransform.wgs84ToWebMercator(lat, lon);
        Point dest = new Point((float) p.x, (float) p.y).transform(config.getOriginX(), config.getOriginY(), scaled);
        return new Vector(dest, origin);
    }

    public void onLocationChange(Location location) {
        float rotation = location.hasBearing() ? location.getBearing() : getRotation();
//        float rotation = getRotation();
        Vector translation = moveTo(location.getLongitude(), location.getLatitude());
        Log.v(TAG, "onLocationChange: " + translation);

        currentAnimation = new TransformationAnimation(1, rotation, translation);
        if (isNavFocus) {
            animation.start(
                    new TransformationAnimation(
                            getScale(),
                            getRotation(),
                            getTranslation()
                    ),
                    currentAnimation
            );
        }

        curLon = location.getLongitude();
        curLat = location.getLatitude();

        float scaled = CoordinateTransform.getScalePixel(config.getScaleDenominator()) * config.getLengthPerPixel();
        List<Point> worldQuad = screenToWorld(SCREEN_QUAD);
        float bboxMinX = worldQuad.get(0).x / scaled + config.getOriginX();
        float bboxMinY = worldQuad.get(0).y / scaled + config.getOriginY();
        float bboxMaxX = worldQuad.get(3).x / scaled + config.getOriginX();
        float bboxMaxY = worldQuad.get(3).y / scaled + config.getOriginY();
        ProjCoordinate transformedPoint = CoordinateTransform.webMercatorToWgs84(bboxMinX, bboxMinY);
        Point bboxMin = new Point((float) transformedPoint.x, (float) transformedPoint.y);
        transformedPoint = CoordinateTransform.webMercatorToWgs84(bboxMaxX, bboxMaxY);
        Point bboxMax = new Point((float) transformedPoint.x, (float) transformedPoint.y);
        float radius = bboxMin.distance(bboxMax) / 2;

        mapView.setCurLocation((float) curLon, (float) curLat, radius);

        userLocationChanged = true;
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

        float scale = getScale();
        float rotation = getRotation();
        Vector translation = getTranslation();

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
        isNavFocus = false;
        Point newPos = pixelScreenToCoord(x, y);
        Log.d(TAG, "actionMove: " + newPos);
        Vector translation = new Vector(oldPos, newPos);
        translation = translation.add(getTranslation());
        config.setTranslation(translation);
    }

    private double lastActionUpTime = 0;
    private Point lastActionUpPos = null;

    public void actionUp(float x, float y) {
        double curTime = System.currentTimeMillis();
        Point curPos = new Point(x, y);

        boolean isSamePos = lastActionUpPos != null && lastActionUpPos.distance(curPos) < 100;
        if (isSamePos && curTime - lastActionUpTime < 200) {
            isNavFocus = true;
            Log.d(TAG, "actionUp currentAnimation: " + currentAnimation);
            if (currentAnimation != null) {
                animation.start(
                        new TransformationAnimation(
                                getScale(),
                                getRotation(),
                                getTranslation()
                        ),
                        currentAnimation
                );
            }
        }
        lastActionUpTime = curTime;
        lastActionUpPos = curPos;
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

        config.setTransform(scale * getScale(), (angle + getRotation() + 360) % 360, translation.add(getTranslation()));
    }

    public void actionPointerUp(List<Float> x, List<Float> y) {
    }

    private void drawUser() {
        if (userLocationChanged) {
            float scaled = CoordinateTransform.getScalePixel(config.getScaleDenominator()) * config.getLengthPerPixel();
            ProjCoordinate p = CoordinateTransform.wgs84ToWebMercator(curLat, curLon);
            Point userLocation = new Point((float) p.x, (float) p.y).transform(config.getOriginX(), config.getOriginY(), scaled);
            userIcon.relocate(userLocation);
            userLocationChanged = false;
        }
        userIcon.draw();
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);

        if (animation.isRunning()) {
            TransformationAnimation cur = animation.getCurrent();
            config.setTransform(cur.scale, cur.rotation, cur.translation);
        }
        mapView.draw();
        drawUser();
    }
}
