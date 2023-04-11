package com.hcmut.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.DisplayMetrics;

import com._2gis.cartoshka.CartoParser;
import com._2gis.cartoshka.tree.Block;
import com._2gis.cartoshka.tree.expression.Literal;
import com._2gis.cartoshka.visitor.ConstantFoldVisitor;
import com._2gis.cartoshka.visitor.EvaluateVisitor;
import com._2gis.cartoshka.visitor.PrintVisitor;
import com._2gis.cartoshka.visitor.VolatilityCheckVisitor;
import com.hcmut.test.algorithm.StrokeGenerator;
import com.hcmut.test.data.VertexArray;
import com.hcmut.test.geometry.LineStrip;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.Ray;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.geometry.Vector;
import com.hcmut.test.map.MapReader;
import com.hcmut.test.object.ObjectBuilder;
import com.hcmut.test.data.Way;
import com.hcmut.test.object.TextDrawer;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.programs.TextShaderProgram;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TestRenderer implements GLSurfaceView.Renderer {
    private static final float NEAR = 1f;
    private static final float FAR = 10f;
    private final Context context;
    private ColorShaderProgram colorProgram;
    private TextShaderProgram textProgram;
    private ObjectBuilder builder;
    private MapReader mapReader;
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
            -4.8587317f, -1.1496106f, 0.0f,
            -4.849833f, -1.1368186f, 0.0f,
            -4.8320355f, -1.057842f, 0.0f,
            -4.836485f, -1.0094548f, 0.0f,
            -4.8453836f, -0.96718574f, 0.0f,
            -4.8587317f, -0.9321468f, 0.0f,
            -4.876529f, -0.90155727f, 0.0f,
            -4.8943267f, -0.8776418f, 0.0f,
            -4.9254727f, -0.81368184f, 0.0f,
            -4.929922f, -0.80478305f, 0.0f,
            -4.9254727f, -0.7997775f, 0.0f,
            -4.9210234f, -0.7975528f, 0.0f,
            -4.876529f, -0.77586204f, 0.0f,
            -4.863181f, -0.7636262f, 0.0f,
            -4.8676305f, -0.74360394f, 0.0f,
            -4.876529f, -0.7080089f, 0.0f,
            -4.876529f, -0.6968854f, 0.0f,
            -4.87208f, -0.69132364f, 0.0f,
            -4.8587317f, -0.6885428f, 0.0f,
            -4.6273637f, -0.6401557f, 0.0f,
    };
    private final float[] vertices2 = {
            -0.15175f, 0.12061f, 0,
            0.14604f, 0.22661f, 0,
            -0.07352f, 0.21651f, 0,
            0.44637f, 0.22661f, 0
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
    private int width;
    private int height;
    private Point oldPos;
    private List<Point> oldPosList;


    public TestRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        GLES20.glClearColor(0.95f, 0.94f, 0.91f, 1f);
        colorProgram = new ColorShaderProgram(context, projectionMatrix, modelViewMatrix, transformMatrix);
        textProgram = new TextShaderProgram(context, projectionMatrix, modelViewMatrix, transformMatrix);

//        float minLon = 106.73603f;
//        float maxLon = 106.74072f;
//        float minLat = 10.73122f;
//        float maxLat = 10.73465f;

        float minLon = 106.7091f;
        float maxLon = 106.7477f;
        float minLat = 10.7190f;
        float maxLat = 10.7455f;

//        float minLon = 106.7301f;
//        float maxLon = 106.7477f;
//        float minLat = 10.7290f;
//        float maxLat = 10.7405f;

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

//        originX = 0;
//        originY = 0;
//        scale = 1f;

        System.out.println("Origin: " + originX + ", " + originY + ", scale: " + scale);


        builder = new ObjectBuilder(context, colorProgram, textProgram);

        for (String key : mapReader.ways.keySet()) {
            Way way = mapReader.ways.get(key);
            if (way != null) {
                builder.addWay(key, way, originX, originY, scale);
            }
        }
        builder.finalizeDrawer();

//        testFoo();
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
        return new Point(x / width * 2 - 1, 1 - y / height * 2);
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
        this.width = width;
        this.height = height;
        // Set the OpenGL viewport to fill the entire surface.
        GLES20.glViewport(0, 0, width, height);
        final float aspectRatio = width > height ? (float) width / (float) height : (float) height / (float) width;
        Matrix.frustumM(projectionMatrix, 0, -width / (float) height, width / (float) height, -1f, 1f, NEAR, FAR);
        Matrix.setLookAtM(modelViewMatrix, 0, 0f, 0f, 2f, 0f, 0, 0f, 0f, 1f, 0f);
        Matrix.setIdentityM(transformMatrix, 0);

        testPointCalc();

//        Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f);
//        Matrix.setIdentityM(modelViewMatrix, 0);
    }

    public void testFoo() {
        // Resource to file
        List<String> files;
        InputStream[] inputStreams;
        try {
            files = new ArrayList<>(List.of(context.getAssets().list("style/carto")));
            inputStreams = new InputStream[files.size()];
            for (int i = 0; i < files.size(); i++) {
                inputStreams[i] = context.getAssets().open("style/carto/" + files.get(i));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        CartoParser parser = new CartoParser();
        Block[] styles = new Block[inputStreams.length];
        for (int i = 0; i < inputStreams.length; i++) {
            InputStream inputStream = inputStreams[i];
            String file = files.get(i);
            try (InputStreamReader reader = new InputStreamReader(inputStream)) {
                // parsing the file
                styles[i] = parser.parse(file, reader);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // constant folding
        for (Block style : styles) {
            style.accept(new ConstantFoldVisitor(), null);
            Literal p = style.accept(new EvaluateVisitor(), null);
            Boolean b = style.accept(new VolatilityCheckVisitor(), null);

//            System.out.println("Literal: " + p);
//            System.out.println("Volatility: " + b);

            // pretty print
            String pretty = style.accept(new PrintVisitor(), null);
            System.out.println(pretty);
        }
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

        StrokeGenerator.Stroke rvStroke = StrokeGenerator.generateStrokeT(new LineStrip(chosenVertices), 10, 0.02f);
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

    @Override
    public void onDrawFrame(GL10 glUnused) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);

        builder.draw();
//        testStroke();
//        testText();
    }
}
