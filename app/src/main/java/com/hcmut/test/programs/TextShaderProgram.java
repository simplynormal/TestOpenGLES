package com.hcmut.test.programs;

import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINEAR_MIPMAP_LINEAR;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGenerateMipmap;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLUtils.texImage2D;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.util.Log;

import androidx.annotation.NonNull;

import com.hcmut.test.R;

import java.util.List;

public class TextShaderProgram extends ShaderProgram {
    public static final String U_PROJECTION_MATRIX = "u_ProjectionMatrix";
    public static final String U_MODEL_VIEW_MATRIX = "u_ModelViewMatrix";
    public static final String U_TRANSFORM_MATRIX = "u_TransformMatrix";
    public static final String U_TEXTURE_UNIT = "u_TextureUnit";
    public static final String A_POSITION = "a_Position";
    public static final String A_TEX_COORD = "a_TexCoord";
    private static final int[] VERTEX_ATTRIBS = new int[]{
            3, 2
    };
    private static final int TOTAL_VERTEX_ATTRIB_COUNT = 5;
    private static final String[] VERTEX_ATTRIB_NAMES = new String[]{
            A_POSITION, A_TEX_COORD
    };
    private static final List<VertexAttrib> VERTEX_ATTRIB_LIST = getVertexAttribs(VERTEX_ATTRIB_NAMES, VERTEX_ATTRIBS);
    private final float[] projectionMatrix;
    private final float[] modelViewMatrix;
    private final float[] transformMatrix;

    public TextShaderProgram(Context context, float[] projectionMatrix, float[] modelViewMatrix, float[] transformMatrix) {
        super(context, R.raw.text_vert, R.raw.text_frag);
        this.projectionMatrix = projectionMatrix;
        this.modelViewMatrix = modelViewMatrix;
        this.transformMatrix = transformMatrix;
    }

    @Override
    public void useProgram() {
        super.useProgram();
        int uMVPLocation = getUniformLocation(U_PROJECTION_MATRIX);
        GLES20.glUniformMatrix4fv(uMVPLocation, 1, false, projectionMatrix, 0);

        int uModelViewMatrix = getUniformLocation(U_MODEL_VIEW_MATRIX);
        GLES20.glUniformMatrix4fv(uModelViewMatrix, 1, false, modelViewMatrix, 0);

        int uTransformMatrix = getUniformLocation(U_TRANSFORM_MATRIX);
        GLES20.glUniformMatrix4fv(uTransformMatrix, 1, false, transformMatrix, 0);
    }

    public int getUniformLocation(String uniformName) {
        return glGetUniformLocation(program, uniformName);
    }

    public int getAttributeLocation(String attributeName) {
        return glGetAttribLocation(program, attributeName);
    }

    @Override
    public List<VertexAttrib> getVertexAttribs() {
        return VERTEX_ATTRIB_LIST;
    }

    @Override
    public int getTotalVertexAttribCount() {
        return TOTAL_VERTEX_ATTRIB_COUNT;
    }

    public int loadTexture(@NonNull Bitmap bitmap) {
        super.useProgram();

        final int[] textureObjectIds = new int[1];
        glGenTextures(1, textureObjectIds, 0);

        if (textureObjectIds[0] == 0) {
            Log.w("TextureHelper", "Could not generate a new OpenGL texture object.");
            return 0;
        }

        // Bind to the texture in OpenGL
        glBindTexture(GL_TEXTURE_2D, textureObjectIds[0]);

        // Set filtering: a default must be set, or the texture will be
        // black.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        // Load the bitmap into the bound texture.
        texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

        // Note: Following code may cause an error to be reported in the
        // ADB log as follows: E/IMGSRV(20095): :0: HardwareMipGen:
        // Failed to generate texture mipmap levels (error=3)
        // No OpenGL error will be encountered (glGetError() will return
        // 0). If this happens, just squash the source image to be
        // square. It will look the same because of texture coordinates,
        // and mipmap generation will work.

        glGenerateMipmap(GL_TEXTURE_2D);

        // Recycle the bitmap, since its data has been loaded into
        // OpenGL.
        bitmap.recycle();

        // Unbind from the texture.
        glBindTexture(GL_TEXTURE_2D, 0);

        return textureObjectIds[0];
    }

    public void deleteTexture(int textureId) {
        glDeleteTextures(1, new int[]{textureId}, 0);
    }

    public void setCurrentTexture(int textureId) {
        int uTextureUnit = getUniformLocation(U_TEXTURE_UNIT);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureUnit, 0);
    }
}
