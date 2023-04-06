package com.hcmut.test.object;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.opengl.GLES20;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.programs.TextShaderProgram;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

public class TextDrawer {
    private static void drawText(Canvas canvas, Paint paint, String text, int textureWidth, int textureHeight, int fontHeight, List<Point> points) {
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

        float scaleFactorHeight = (textureHeight - fontHeight * 2) * (1 - 2f * fontHeight / textureHeight);
        float scaleFactorWidth = (textureWidth - fontHeight * 2) * (1 - 2f * fontHeight / textureWidth);
        Matrix matrix = new Matrix();
        matrix.postScale(scaleFactorWidth, scaleFactorHeight);
        matrix.postTranslate(fontHeight, fontHeight);
        path.transform(matrix);

        PathMeasure pathMeasure = new PathMeasure(path, false);
        float pathLength = pathMeasure.getLength();

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds); // calculate the height of the text

        float textWidth = paint.measureText(text);
        float hOffset = (pathLength - textWidth) / 2; // calculate the horizontal offset to center the text
        float vOffset = bounds.height() / 2f; // calculate the vertical offset to center the text

        canvas.drawTextOnPath(text, path, hOffset + 100, vOffset, paint);
        canvas.drawPath(path, paint);
    }

    public static void test(TextShaderProgram shaderProgram, Context context, List<Point> points) {
        Typeface tf = Typeface.createFromAsset(context.getAssets(), "Inter-Black.ttf");

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

        int textureHeight = 512;
        int fontHeight = 48; // height of the font in pixels

        minX -= (float) fontHeight / textureHeight;
        minY -= (float) fontHeight / textureHeight;
        maxX += (float) fontHeight / textureHeight;
        maxY += (float) fontHeight / textureHeight;

        float aspectRatio = (maxX - minX) / (maxY - minY);
        int textureWidth = (int) (textureHeight * aspectRatio);

//        Bitmap bitmap = Bitmap.createBitmap(textureSize, textureSize, Bitmap.Config.ARGB_8888);
        Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.GREEN);
        Paint paint = new Paint();
        paint.setTypeface(tf);
        paint.setTextSize(fontHeight);
        paint.setAntiAlias(true);
        String alphabet = "có cái lồn";

        Collections.reverse(points);

        paint.setColor(Color.TRANSPARENT);
        paint.setStyle(Paint.Style.FILL);
        drawText(canvas, paint, alphabet, textureWidth, textureHeight, fontHeight, points);

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        drawText(canvas, paint, alphabet, textureWidth, textureHeight, fontHeight, points);

        ByteBuffer buffer = ByteBuffer.allocateDirect(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(buffer);
        buffer.position(0);

        int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, textureHeight, textureHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        shaderProgram.useProgram();
        int textId = shaderProgram.loadTexture(bitmap);
        shaderProgram.setCurrentTexture(textId);

//        VertexArray vertexArray = new VertexArray(shaderProgram, new float[]{
//                0, 0, 0, 0, 1,
//                0, 2, 0, 0, 0,
//                2, 0, 0, 1, 1,
//                2, 2, 0, 1, 0,
//        });

        VertexArray vertexArray = new VertexArray(shaderProgram, new float[]{
                minX, minY, 0, 0, 1,
                minX, maxY, 0, 0, 0,
                maxX, minY, 0, 1, 1,
                maxX, maxY, 0, 1, 0,
        });

        vertexArray.setDataFromVertexData();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    public TextDrawer(TextShaderProgram shaderProgram, InputStream font) {
    }
}
