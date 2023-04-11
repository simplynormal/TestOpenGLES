package com.hcmut.test.object;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.util.Pair;

import com.hcmut.test.algorithm.StrokeGenerator;
import com.hcmut.test.data.VertexArray;
import com.hcmut.test.data.Way;
import com.hcmut.test.geometry.LineStrip;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.programs.TextShaderProgram;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class TextDrawer extends Drawable {
    private final TextShaderProgram textShaderProgram;
    private final Paint paint;
    float originX = 0;
    float originY = 0;
    float scale = 1;
    private static final int FONT_HEIGHT = 15;
    private static final int TEXTURE_SIZE_SCALE = 400;
    private final HashMap<String, VertexArray> textVertexArray = new HashMap<>();
    private final HashMap<String, Integer> textTextureIds = new HashMap<>();

    private static Path pointsToPath(List<Point> points, float textureWidth, float textureHeight, float fontHeight) {
        float maxX = 0;
        float maxY = 0;
        float minX = 99999;
        float minY = 99999;

        for (Point p : points) {
            if (p.x > maxX) maxX = p.x;
            if (p.y > maxY) maxY = p.y;
            if (p.x < minX) minX = p.x;
            if (p.y < minY) minY = p.y;
        }

        float lengthX = maxX - minX;
        float lengthY = maxY - minY;

        Path path = new Path();
        float scaledX = (points.get(0).x - minX) / lengthX;
        float scaledY = 1 - (points.get(0).y - minY) / lengthY;
        path.moveTo(scaledX, scaledY);
        for (int i = 1; i < points.size(); i++) {
            scaledX = (points.get(i).x - minX) / lengthX;
            scaledY = 1 - (points.get(i).y - minY) / lengthY;
            path.lineTo(scaledX, scaledY);
        }

        float scaleFactorWidth = (textureWidth - fontHeight * 2);
        float scaleFactorHeight = (textureHeight - fontHeight * 2);
//        scaleFactorHeight = textureHeight;
//        scaleFactorWidth = textureWidth;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleFactorWidth, scaleFactorHeight);
        matrix.postTranslate(fontHeight, fontHeight);
        path.transform(matrix);

        return path;
    }

    private static boolean drawText(Canvas canvas, Paint paint, String text, int textureWidth, int textureHeight, int fontHeight, List<Point> points) {
        Path path = pointsToPath(points, textureWidth, textureHeight, fontHeight);

        PathMeasure pathMeasure = new PathMeasure(path, false);
        float pathLength = pathMeasure.getLength();
        float textWidth = paint.measureText(text);

        if (textWidth > pathLength) {
            return false;
        }

        float hOffset = (pathLength - textWidth) / 2;
        float vOffset = (-paint.ascent() + paint.descent()) / 2 - paint.descent();

        // check if the path is upside down
        float[] pos = new float[2];
        float[] tan = new float[2];
        float angle;

        float threshold = 60; // adjust this value as needed
        float prevAngle = 0;
        boolean hasSharpTurn = false;
        for (float distance = 0; distance < pathLength; distance += 10) {
            pathMeasure.getPosTan(distance, pos, tan);
            angle = (float) Math.toDegrees(Math.atan2(tan[1], tan[0]));
            if (distance > 0 && Math.abs(angle - prevAngle) > threshold) {
                hasSharpTurn = true;
                break;
            }
            prevAngle = angle;
        }

        if (hasSharpTurn) {
            return false;
        }

        pathMeasure.getPosTan(pathLength / 2, pos, tan);
        angle = (float) Math.toDegrees(Math.atan2(tan[1], tan[0]));
        boolean isUpsideDown = angle > 90 || angle < -90;

        if (isUpsideDown) {
            List<Point> newPoints = new ArrayList<>(points);
            Collections.reverse(newPoints);
            path = pointsToPath(newPoints, textureWidth, textureHeight, fontHeight);
        }

        canvas.drawTextOnPath(text, path, hOffset, vOffset, paint);

//        canvas.drawPath(path, paint);
        return true;
    }

    private static void drawTextOrg(Canvas canvas, Paint paint, String text, int textureWidth, int textureHeight, int fontHeight, List<Point> points) {
        float maxX = 0;
        float maxY = 0;
        float minX = 99999;
        float minY = 99999;

        for (Point p : points) {
            if (p.x > maxX) maxX = p.x;
            if (p.y > maxY) maxY = p.y;
            if (p.x < minX) minX = p.x;
            if (p.y < minY) minY = p.y;
        }

        Path path = new Path();
        float scaledX = (points.get(0).x - minX) / (maxX - minX);
        float scaledY = 1 - (points.get(0).y - minY) / (maxY - minY);
        path.moveTo(scaledX, scaledY);
        for (int i = 1; i < points.size(); i++) {
            scaledX = (points.get(i).x - minX) / (maxX - minX);
            scaledY = 1 - (points.get(i).y - minY) / (maxY - minY);
            path.lineTo(scaledX, scaledY);
        }

        float scaleFactorHeight = textureHeight;
        float scaleFactorWidth = textureWidth;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleFactorWidth, scaleFactorHeight);
        path.transform(matrix);

        PathMeasure pathMeasure = new PathMeasure(path, false);
        float pathLength = pathMeasure.getLength();

        float textWidth = paint.measureText(text);
        float hOffset = (pathLength - textWidth) / 2;
        float vOffset = (-paint.ascent() + paint.descent()) / 2 - paint.descent();

        boolean isUpsideDown = false;
        float[] pos = new float[2];
        float[] tan = new float[2];
        pathMeasure.getPosTan(pathLength / 2, pos, tan);
        float angle = (float) Math.toDegrees(Math.atan2(tan[1], tan[0]));
        if (angle > 90 || angle < -90) {
            isUpsideDown = true;
        }

        if (isUpsideDown) {
            List<Point> newPoints = new ArrayList<>(points);
            Collections.reverse(newPoints);
            path = new Path();
            scaledX = (newPoints.get(0).x - minX) / (maxX - minX);
            scaledY = 1 - (newPoints.get(0).y - minY) / (maxY - minY);
            path.moveTo(scaledX, scaledY);
            for (int i = 1; i < newPoints.size(); i++) {
                scaledX = (newPoints.get(i).x - minX) / (maxX - minX);
                scaledY = 1 - (newPoints.get(i).y - minY) / (maxY - minY);
                path.lineTo(scaledX, scaledY);
            }

            scaleFactorHeight = textureHeight;
            scaleFactorWidth = textureWidth;
            matrix = new Matrix();
            matrix.postScale(scaleFactorWidth, scaleFactorHeight);
            path.transform(matrix);
        }

        canvas.drawTextOnPath(text, path, hOffset, vOffset, paint);
        canvas.drawPath(path, paint);
    }

    public static void test(TextShaderProgram shaderProgram, ColorShaderProgram colorProgram, Context context, List<Point> points) {
        innerTest(shaderProgram, colorProgram, context, points, 0, 0, false);
        innerTest(shaderProgram, colorProgram, context, points, 0.5f, 0, true);
        anotherTest(shaderProgram, context);
    }

    public static void innerTest(TextShaderProgram shaderProgram, ColorShaderProgram colorProgram, Context context, List<Point> points, float translateX, float translateY, boolean isOriginal) {
        List<Point> localPoints = new ArrayList<>(points);
        Typeface tf = Typeface.createFromAsset(context.getAssets(), "NotoSans-Regular.ttf");

        float maxX = 0;
        float maxY = 0;
        float minX = 99999;
        float minY = 99999;

        for (Point p : localPoints) {
            if (p.x > maxX) maxX = p.x;
            if (p.y > maxY) maxY = p.y;
            if (p.x < minX) minX = p.x;
            if (p.y < minY) minY = p.y;
        }

        int fontHeight = FONT_HEIGHT;
        float lengthX = maxX - minX;
        float lengthY = maxY - minY;
        int textureHeight = Math.round(TEXTURE_SIZE_SCALE * lengthY) + 2 * fontHeight;
        int textureWidth = Math.round(TEXTURE_SIZE_SCALE * lengthX) + 2 * fontHeight;

        if (!isOriginal) {
            minX -= ((float) textureWidth / (textureWidth - 2 * fontHeight) - 1) * lengthX / 2f;
            maxX += ((float) textureWidth / (textureWidth - 2 * fontHeight) - 1) * lengthX / 2f;
            minY -= ((float) textureHeight / (textureHeight - 2 * fontHeight) - 1) * lengthY / 2f;
            maxY += ((float) textureHeight / (textureHeight - 2 * fontHeight) - 1) * lengthY / 2f;
        }

        minX += translateX;
        maxX += translateX;
        minY += translateY;
        maxY += translateY;

        Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.GREEN);
        Paint paint = new Paint();
        paint.setTypeface(tf);
        paint.setTextSize(fontHeight);
        paint.setAntiAlias(true);
        String alphabet = "Hẻm 672 Đường Huỳnh Tấn Phát";

//        Collections.reverse(localPoints);

        paint.setColor(Color.TRANSPARENT);
        paint.setStyle(Paint.Style.FILL);
        if (isOriginal) {
            drawTextOrg(canvas, paint, alphabet, textureWidth, textureHeight, fontHeight, localPoints);
        } else {
            drawText(canvas, paint, alphabet, textureWidth, textureHeight, fontHeight, localPoints);
        }

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        if (isOriginal) {
            drawTextOrg(canvas, paint, alphabet, textureWidth, textureHeight, fontHeight, localPoints);
        } else {
            drawText(canvas, paint, alphabet, textureWidth, textureHeight, fontHeight, localPoints);
        }

        shaderProgram.useProgram();
        int textId = shaderProgram.loadTexture(bitmap);
        shaderProgram.setCurrentTexture(textId);

        VertexArray vertexArray = new VertexArray(shaderProgram, new float[]{
                minX, minY, 0.01f, 0, 1,
                minX, maxY, 0.01f, 0, 0,
                maxX, minY, 0.01f, 1, 1,
                maxX, maxY, 0.01f, 1, 0,
        });

        vertexArray.setDataFromVertexData();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        shaderProgram.deleteTexture(textId);

        colorProgram.useProgram();
        List<Point> transformedPoints = new ArrayList<>();
        for (Point p : localPoints) {
            float x = p.x + translateX;
            float y = p.y + translateY;
            transformedPoints.add(new Point(x, y));
        }
        VertexArray singleVertexArray = new VertexArray(colorProgram, transformedPoints, 0, 0, 1, 1);
        singleVertexArray.setDataFromVertexData();
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, transformedPoints.size());
    }

    public static void anotherTest(TextShaderProgram shaderProgram, Context context) {
        Typeface tf = Typeface.createFromAsset(context.getAssets(), "NotoSans-Regular.ttf");

        int textureSize = 512;
        Bitmap bitmap = Bitmap.createBitmap(textureSize, textureSize, Bitmap.Config.ARGB_8888);
        Bitmap bitmap2 = Bitmap.createBitmap(textureSize, textureSize, Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(bitmap2);

        Paint textPaint = new Paint();
        textPaint.setTypeface(tf);
        textPaint.setTextSize(48);
        textPaint.setColor(Color.BLACK);
        textPaint.setStyle(Paint.Style.STROKE);

        Path textPath = new Path();
        textPath.addArc(100, 100, 500, 500, -180, 180);

        canvas2.drawPath(textPath, textPaint);

// Extract the alpha channel of the bitmap as a new bitmap object
        Bitmap alphaBitmap = bitmap2.extractAlpha();

// Create a new Path object from the alpha channel bitmap
        Path textOutlinePath = new Path();
        Canvas alphaCanvas = new Canvas(alphaBitmap);
        alphaCanvas.drawColor(Color.BLACK, PorterDuff.Mode.SRC_IN);
        textOutlinePath.reset();
        alphaCanvas.drawPath(textPath, textPaint);
        alphaCanvas.drawBitmap(alphaBitmap, 0, 0, textPaint);
        textOutlinePath.addPath(textPath);
        textOutlinePath.op(textPath, Path.Op.DIFFERENCE);

// Draw the text outline on the canvas
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLUE);
        canvas.drawPath(textOutlinePath, textPaint);

        shaderProgram.useProgram();
        int textId = shaderProgram.loadTexture(bitmap);
        shaderProgram.setCurrentTexture(textId);

        float minX = 1;
        float minY = 1;
        float maxX = 2;
        float maxY = 2;

        VertexArray vertexArray = new VertexArray(shaderProgram, new float[]{
                minX, minY, 0.01f, 0, 1,
                minX, maxY, 0.01f, 0, 0,
                maxX, minY, 0.01f, 1, 1,
                maxX, maxY, 0.01f, 1, 0,
        });

        vertexArray.setDataFromVertexData();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        shaderProgram.deleteTexture(textId);
    }

    public TextDrawer(Context context, TextShaderProgram shaderProgram) {
        super(shaderProgram);
        textShaderProgram = shaderProgram;
        Typeface tf = Typeface.createFromAsset(context.getAssets(), "NotoSans-Regular.ttf");
        paint = new Paint();
        paint.setTypeface(tf);
        paint.setTextSize(FONT_HEIGHT);
        paint.setAntiAlias(true);
    }

    public void addWay(String key, Way way, float originX, float originY, float scale, float r, float g, float b, float a) {
        if (!way.tags.containsKey("highway") || !way.tags.containsKey("name")) return;

        this.originX = originX;
        this.originY = originY;
        this.scale = scale;
        try {
            addWay(key, way, r, g, b, a);
        } catch (Exception e) {
            System.err.println("Error drawing way " + key);
            for (Point point : way.toPoints(originX, originY, scale)) {
                System.err.println(point.x + "f, " + point.y + "f, " + point.z + "f,");
            }
            e.printStackTrace();
        }

//        if (key.equals("205957639")) {
//            System.err.println("Test drawing way " + key);
//            for (Point point : way.toPoints(originX, originY, scale)) {
//                System.err.println(point.x + "f, " + point.y + "f, " + point.z + "f,");
//            }
//        }
    }

    @SuppressLint("NewApi")
    public void addWay(String key, Way way, float r, float g, float b, float a) {
        String name = way.tags.get("name");
        List<Point> points = way.toPoints(originX, originY, scale);
        if (points.size() < 2) return;

        float maxX = 0;
        float maxY = 0;
        float minX = 99999;
        float minY = 99999;

        for (Point p : points) {
            if (p.x > maxX) maxX = p.x;
            if (p.y > maxY) maxY = p.y;
            if (p.x < minX) minX = p.x;
            if (p.y < minY) minY = p.y;
        }

        int fontHeight = FONT_HEIGHT;
        float lengthX = maxX - minX;
        float lengthY = maxY - minY;
        int textureHeight = Math.round(TEXTURE_SIZE_SCALE * lengthY) + 2 * fontHeight;
        int textureWidth = Math.round(TEXTURE_SIZE_SCALE * lengthX) + 2 * fontHeight;

        minX -= ((float) textureWidth / (textureWidth - 2 * fontHeight) - 1) * lengthX / 2;
        maxX += ((float) textureWidth / (textureWidth - 2 * fontHeight) - 1) * lengthX / 2;
        minY -= ((float) textureHeight / (textureHeight - 2 * fontHeight) - 1) * lengthY / 2;
        maxY += ((float) textureHeight / (textureHeight - 2 * fontHeight) - 1) * lengthY / 2;

        Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        paint.setColor(Color.argb(a, r, g, b));
        paint.setStyle(Paint.Style.FILL);
        boolean isDrawn = drawText(canvas, paint, name, textureWidth, textureHeight, fontHeight, points);

        if (!isDrawn) {
            return;
        }

        textShaderProgram.useProgram();
        int textId = textShaderProgram.loadTexture(bitmap);

        VertexArray vertexArray = new VertexArray(shaderProgram, new float[]{
                minX, minY, 0.01f, 0, 1,
                minX, maxY, 0.01f, 0, 0,
                maxX, minY, 0.01f, 1, 1,
                maxX, maxY, 0.01f, 1, 0,
        });

        textTextureIds.put(key, textId);
        textVertexArray.put(key, vertexArray);
    }

    public void removeWay(String key) {
        Integer textId = textTextureIds.get(key);
        assert textId != null;
        textShaderProgram.deleteTexture(textId);
        textTextureIds.remove(key);
        textVertexArray.remove(key);
    }

    @Override
    public void finalizeDrawer() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    }

    @Override
    public void draw() {
        if (textVertexArray.isEmpty() || textTextureIds.isEmpty()) return;

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        textShaderProgram.useProgram();
        for (String key : textVertexArray.keySet()) {
            Integer textId = textTextureIds.get(key);
            VertexArray vertexArray = textVertexArray.get(key);
            assert textId != null && vertexArray != null;

            textShaderProgram.setCurrentTexture(textId);
            vertexArray.setDataFromVertexData();
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }
}
