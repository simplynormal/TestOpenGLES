package com.hcmut.test.object;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.programs.ShaderProgram;
import com.hcmut.test.programs.TextShaderProgram;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class TextDrawer {
    public static void test(TextShaderProgram shaderProgram, Context context) {
        Typeface tf = Typeface.createFromAsset(context.getAssets(), "Inter-Black.ttf");
        int fontHeight = 48; // height of the font in pixels
        int textureSize = 512; // size of the texture in pixels

        Bitmap bitmap = Bitmap.createBitmap(textureSize, textureSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setTypeface(tf);
        paint.setTextSize(fontHeight);
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        // set text border color
        paint.setStyle(Paint.Style.FILL);
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        String alphabet = "có cái lồn";
        int x = 0, y = 0;

        for (char c : alphabet.toCharArray()) {
            canvas.drawText(String.valueOf(c), x, y + fontHeight, paint);
            x += paint.measureText(String.valueOf(c));
            if (x > textureSize - fontHeight) {
                x = 0;
                y += fontHeight;
            }
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(buffer);
        buffer.position(0);

        int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, textureSize, textureSize, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        shaderProgram.useProgram();
        shaderProgram.setTextColor(0, 1, 0, 1);
        int textId = shaderProgram.loadTexture(bitmap);
        shaderProgram.setCurrentTexture(textId);

        VertexArray vertexArray = new VertexArray(shaderProgram, new float[] {
                -1, -1, 0, 0, 1,
                -1, 1, 0, 0, 0,
                1, -1, 0, 1, 1,
                1, 1, 0, 1, 0,
        });

        vertexArray.setDataFromVertexData();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    public TextDrawer(TextShaderProgram shaderProgram, InputStream font) {
    }
}
