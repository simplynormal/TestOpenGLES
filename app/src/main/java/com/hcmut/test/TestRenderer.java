package com.hcmut.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.hcmut.test.algorithm.CoordinateTransform;
import com.hcmut.test.algorithm.StrokeGenerator;
import com.hcmut.test.data.VertexArray;
import com.hcmut.test.geometry.LineStrip;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.geometry.Ray;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.geometry.Vector;
import com.hcmut.test.mapnik.Layer;
import com.hcmut.test.mapnik.symbolizer.LineSymbolizer;
import com.hcmut.test.mapnik.StyleParser;
import com.hcmut.test.mapnik.symbolizer.TextSymbolizer;
import com.hcmut.test.reader.MapReader;
import com.hcmut.test.object.ObjectBuilder;
import com.hcmut.test.osm.Way;
import com.hcmut.test.object.TextDrawer;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.programs.TextShaderProgram;
import com.hcmut.test.utils.Config;

import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TestRenderer implements GLSurfaceView.Renderer {
    private final Context context;
    private ColorShaderProgram colorProgram;
    private TextShaderProgram textProgram;
    private ObjectBuilder builder;
    private MapReader mapReader;
    private StyleParser styleParser;
    private Config config;
    private final float[] vertices = {
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
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

    private float oldX = 0;
    private float oldY = 0;
    private float oldDistance = 0;
    private final float[] projectionMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] transformMatrix = new float[16];
    private float[] oldTransformMatrix;
    private float originX = 0;
    private float originY = 0;
    private float scale = 1f;
    private Point oldPos;
    private List<Point> oldPosList;


    public TestRenderer(Context context) {
        this.context = context;
    }

    public void initOpenGL() {
        colorProgram = new ColorShaderProgram(context, projectionMatrix, modelViewMatrix, transformMatrix);
        textProgram = new TextShaderProgram(context, projectionMatrix, modelViewMatrix, transformMatrix);
        config = new Config(context, colorProgram, textProgram);
        builder = new ObjectBuilder(context, colorProgram, textProgram);

        int width = context.getResources().getDisplayMetrics().widthPixels;
        int height = context.getResources().getDisplayMetrics().heightPixels;
        config.setWidthHeight(width, height);

        float minLon = 106.73603f;
        float maxLon = 106.74072f;
        float minLat = 10.73122f;
        float maxLat = 10.73465f;

//        float minLon = 106.71410f;
//        float maxLon = 106.72421f;
//        float minLat = 10.72307f;
//        float maxLat = 10.72860f;

//        float minLon = 106.7091f;
//        float maxLon = 106.7477f;
//        float minLat = 10.7190f;
//        float maxLat = 10.7455f;

        originX = (minLon + maxLon) / 2;
        originY = (minLat + maxLat) / 2;
        scale = 583.1902f;
        config.setOriginFromWGS84(originX, originY);
        config.setScaleDenominator(1000);

        try {
            mapReader = new MapReader(context, R.raw.map1);
//            mapReader.setBounds(minLon, maxLon, minLat, maxLat);
//            mapReader.read();
            styleParser = new StyleParser(context, R.raw.mapnik, config);
//            styleParser.read();
        } catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        }

        styleParser.validateWays(mapReader.ways);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        GLES20.glClearColor(0.95f, 0.94f, 0.91f, 1f);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        initOpenGL();

//        for (String key : mapReader.ways.keySet()) {
//            Way way = mapReader.ways.get(key);
//            if (way != null) {
//                builder.addWay(key, way, originX, originY, scale);
//            }
//        }
//        builder.finalizeDrawer();

//        Rule.test();
//        StyleParser.test(context, R.raw.mapnik);
        Test.test();
    }

    private void divideByW(float[] vector) {
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
    }

    private Ray convertNormalized2DPointToRay(
            float normalizedX, float normalizedY) {
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

    private void testPointCalc() {
        Point scaledCenter = new Point(1, 1);
        System.out.println("Org: " + scaledCenter);

        float[] mat = new float[16];
        Matrix.setIdentityM(mat, 0);
        Matrix.translateM(mat, 0, 0.5f, 0.5f, 0);

        Point calculatedCenter = testPointCalc(scaledCenter, mat);
        System.out.println("Calc center: " + calculatedCenter);

        Point calcBack = fromProjScreenToCoord(calculatedCenter, mat);
        System.out.println("Calc back: " + calcBack);
    }

    private Point testPointCalc(Point p, float[] transformMatrix) {
        float[] vec = new float[]{p.x, p.y, p.z, 1};

        float[] result = new float[4];
        Matrix.multiplyMV(result, 0, transformMatrix, 0, vec, 0);
        Matrix.multiplyMV(result, 0, modelViewMatrix, 0, result, 0);
        Matrix.multiplyMV(result, 0, projectionMatrix, 0, result, 0);

        if (result[3] != 0) {
            result[0] /= result[3];
            result[1] /= result[3];
            result[2] /= result[3];
        }

        return new Point(result[0], result[1], result[2]);
    }

    private Point pixelScreenToProjScreen(float x, float y) {
        // scale to screen then scale to [-1, 1]
        return new Point(x / config.getWidth() * 2 - 1, 1 - y / config.getHeight() * 2);
    }

    private Point pixelScreenToCoord(float x, float y, float[] transformMatrix) {
        Point pos = pixelScreenToProjScreen(x, y);
        return fromProjScreenToCoord(pos, transformMatrix);
    }

    private Point fromProjScreenToCoord(Point p, float[] transformMatrix) {
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

    public void actionDown(float x, float y) {
        oldTransformMatrix = Arrays.copyOf(transformMatrix, transformMatrix.length);
        oldPos = pixelScreenToCoord(x, y, oldTransformMatrix);
        System.out.println("actionDown: " + oldPos);
    }

    public void actionMove(float x, float y) {
        float[] copyOldTransformMatrix = Arrays.copyOf(oldTransformMatrix, oldTransformMatrix.length);
        Point newPos = pixelScreenToCoord(x, y, copyOldTransformMatrix);
        System.out.println("actionMove: " + newPos);
        float translateX = newPos.x - oldPos.x;
        float translateY = newPos.y - oldPos.y;
        Matrix.translateM(copyOldTransformMatrix, 0, translateX, translateY, 0);

        // copy to transformMatrix
        System.arraycopy(copyOldTransformMatrix, 0, transformMatrix, 0, transformMatrix.length);
    }

    public void actionUp(float x, float y) {
    }

    @SuppressLint("NewApi")
    public void actionPointerDown(List<Float> x, List<Float> y) {
        if (x.size() != 2) return;
        oldTransformMatrix = Arrays.copyOf(transformMatrix, transformMatrix.length);
        oldPosList = new ArrayList<>() {{
            for (int i = 0; i < x.size(); i++) {
                add(pixelScreenToCoord(x.get(i), y.get(i), oldTransformMatrix));
            }
        }};
        System.out.println("actionPointerDown: " + oldPosList);
    }

    @SuppressLint("NewApi")
    public void actionPointerMove(List<Float> x, List<Float> y) {
        if (x.size() != 2) return;
        float[] copyOldTransformMatrix = Arrays.copyOf(oldTransformMatrix, oldTransformMatrix.length);
        List<Point> newPosList = new ArrayList<>() {{
            for (int i = 0; i < x.size(); i++) {
                add(pixelScreenToCoord(x.get(i), y.get(i), oldTransformMatrix));
            }
        }};

        Point oldP1 = oldPosList.get(0);
        Point oldP2 = oldPosList.get(1);
        Point p1 = newPosList.get(0);
        Point p2 = newPosList.get(1);

        // Translate
        Point oldCenter = oldP1.midPoint(oldP2);
        Point center = p1.midPoint(p2);
        float translateX = center.x - oldCenter.x;
        float translateY = center.y - oldCenter.y;
        Matrix.translateM(copyOldTransformMatrix, 0, translateX, translateY, 0);

        // Scale
        float oldDistance = oldP1.distance(oldP2);
        float newDistance = p1.distance(p2);
        float scale = newDistance / oldDistance;

        Matrix.translateM(copyOldTransformMatrix, 0, center.x, center.y, 0);
        Matrix.scaleM(copyOldTransformMatrix, 0, scale, scale, 1);
        Matrix.translateM(copyOldTransformMatrix, 0, -center.x, -center.y, 0);

        // Rotate
        Vector vecX = new Vector(1, 0);
        float oldAngle = new Vector(oldP1, oldP2).signedAngle(vecX);
        float newAngle = new Vector(p1, p2).signedAngle(vecX);
        float angle = (float) Math.toDegrees(oldAngle - newAngle);

        System.out.println("oldAngle: " + Math.toDegrees(oldAngle) + ", newAngle: " + Math.toDegrees(newAngle) + ", angle: " + angle);

        Matrix.translateM(copyOldTransformMatrix, 0, center.x, center.y, 0);
        Matrix.rotateM(copyOldTransformMatrix, 0, angle, 0, 0, 1);
        Matrix.translateM(copyOldTransformMatrix, 0, -center.x, -center.y, 0);

        // copy to transformMatrix
        System.arraycopy(copyOldTransformMatrix, 0, transformMatrix, 0, transformMatrix.length);

        this.scale += scale;
    }

    public void actionPointerUp(List<Float> x, List<Float> y) {
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to fill the entire surface.
        GLES20.glViewport(0, 0, width, height);
        Matrix.frustumM(projectionMatrix, 0, -width / (float) height, width / (float) height, -1f, 1f, 1f, 10f);
        Matrix.setLookAtM(modelViewMatrix, 0, 0f, 0f, 2f, 0f, 0, 0f, 0f, 1f, 0f);
        Matrix.setIdentityM(transformMatrix, 0);
        float scaledToLength = CoordinateTransform.getScalePixel(config.getScaleDenominator()) * config.getLengthPerPixel();
        float translateX = -config.getOriginX() * scaledToLength;
        float translateY = -config.getOriginY() * scaledToLength;
//        Matrix.translateM(transformMatrix, 0, 1, 1, 0);

//        testPointCalc();

//        Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f);
//        Matrix.setIdentityM(modelViewMatrix, 0);
    }

    public void testClosedShape() {
        ObjectBuilder builder1 = new ObjectBuilder(context, colorProgram, textProgram);
        Way way = new Way(vertices);

        builder1.addWay("asd", way, 0, 0, 1);
        builder1.finalizeDrawer();

        builder1.draw();
    }

    public void testStroke() {
//        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        float[] chosenVertices = vertices1;

        float[] first = new float[]{
                chosenVertices[0], chosenVertices[1], chosenVertices[2],
        };

        for (int i = 0; i < chosenVertices.length; i += 3) {
            chosenVertices[i] -= first[0];
            chosenVertices[i + 1] -= first[1];
            chosenVertices[i + 2] -= first[2];
        }

        StrokeGenerator.Stroke rvStroke = StrokeGenerator.generateStroke(new LineStrip(chosenVertices), 8, 0.0012962963f);
        TriangleStrip rv = rvStroke.toTriangleStrip();
        TriangleStrip rvLine = new TriangleStrip(rvStroke.toOrderedPoints());
        TriangleStrip rvBorder = StrokeGenerator.generateBorderFromStroke(rvStroke, 10, 0.002f);
        List<Point> points = Point.toPoints(chosenVertices);

        VertexArray rvVertexArray = new VertexArray(colorProgram, rv, 1f, 1f, 1f, 1);
        VertexArray rvLineVertexArray = new VertexArray(colorProgram, rvLine, 0, 0, 0, 1);
        VertexArray rvBorderVertexArray = new VertexArray(colorProgram, rvBorder, 0, 0, 0, 0.7f);
        VertexArray singleVertexArray = new VertexArray(colorProgram, points, 0, 0, 1, 1);


        colorProgram.useProgram();

        rvVertexArray.setDataFromVertexData();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, rv.points.size());

        rvLineVertexArray.setDataFromVertexData();
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, rvLine.points.size());

        rvBorderVertexArray.setDataFromVertexData();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, rvBorder.points.size());
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, rvBorder.points.size());

        singleVertexArray.setDataFromVertexData();
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, points.size());
    }

    void testText() {
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

        List<Point> points = Point.toPoints(chosenVertices);
        TextDrawer.test(textProgram, colorProgram, context, points);
    }

    void drawLineSymbolizer(float[] chosenVertices, LineSymbolizer lineSymbolizer, boolean drawPoints) {
        List<Point> points = Point.toPoints(chosenVertices);
        PointList pointList = new PointList(points);

        float[] drawables = lineSymbolizer.toDrawable(null, pointList);
        VertexArray vertexArray = new VertexArray(colorProgram, drawables);
        config.colorShaderProgram.useProgram();
        vertexArray.setDataFromVertexData();
        int pointCount = vertexArray.getVertexCount();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, pointCount);

        if (!drawPoints) {
            return;
        }

        for (int i = 0; i < drawables.length; i += 7) {
            drawables[i + 3] = 0;
            drawables[i + 4] = 0;
            drawables[i + 5] = 0;
        }
        vertexArray = new VertexArray(colorProgram, drawables);
        config.colorShaderProgram.useProgram();
        vertexArray.setDataFromVertexData();
        pointCount = vertexArray.getVertexCount();
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, pointCount);
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

        drawLineSymbolizer(chosenVertices, lineSymbolizer, true);
        drawLineSymbolizer(chosenVertices2, lineSymbolizer2, true);
//        drawLineSymbolizer(chosenVertices3, lineSymbolizer3, true);
//        drawLineSymbolizer(chosenVertices4, lineSymbolizer4, false);
    }

    void drawTextSymbolizer(float[] chosenVertices, TextSymbolizer textSymbolizer, boolean drawPoints) {
        List<Point> points = Point.toPoints(chosenVertices);
        PointList pointList = new PointList(points);
        Way way = new Way();
        way.tags.put("daw", "replaced");
        float[] drawables = textSymbolizer.toDrawable(way, pointList);
        VertexArray vertexArray = new VertexArray(textProgram, drawables);
        textSymbolizer.draw(vertexArray, drawables);
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
                "[daw] + ' dawdawd'",
                "0",
                "0",
                "0",
                "0",
                "0",
                null,
                null,
                "line",
                null,
                null,
                null,
                null,
                "15",
                "#ffffff",
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

        drawLineSymbolizer(chosenVertices, lineSymbolizer, false);
        drawTextSymbolizer(chosenVertices, textSymbolizer, true);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
//        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);

//        builder.draw();
//        testStroke();
//        testText();
//        testClosedShape();
//        testLineSymbolizer();
        testTextSymbolizer();
//        for (Layer layer : styleParser.layers) {
//            layer.draw();
//        }
    }
}
