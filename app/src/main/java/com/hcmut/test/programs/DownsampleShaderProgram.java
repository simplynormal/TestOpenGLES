package com.hcmut.test.programs;

import static android.opengl.GLES20.glDeleteTextures;

import android.annotation.SuppressLint;
import android.opengl.GLES20;

import com.hcmut.test.R;
import com.hcmut.test.utils.Config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@SuppressLint("NewApi")
public class DownsampleShaderProgram extends ShaderProgram {
    public static final String A_POSITION = "a_Position";
    public static final String A_TEXCOORD = "a_TexCoord";
    public static final String U_TEXTURE_UNIT = "u_TextureUnit";
    public static final String U_TEXEL_STEP = "u_TexelStep";
    public static final String U_RESOLUTION = "u_Resolution";
    public static final String U_SHOW_EDGES = "u_ShowEdges";
    public static final String U_FXAA_ON = "u_FxaaOn";
    public static final String U_LUMA_THRESHOLD = "u_LumaThreshold";
    public static final String U_MUL_REDUCE = "u_MulReduce";
    public static final String U_MIN_REDUCE = "u_MinReduce";
    public static final String U_MAX_SPAN = "u_MaxSpan";
    private static final int SHOW_EDGES = 1;
    private static final int FXAA_ON = 1;
    private static final float LUMA_THRESHOLD = 0.5f;
    private static final float MUL_REDUCE = 1f / 8f;
    private static final float MIN_REDUCE = 0.1f / 128f;
    private static final float MAX_SPAN = 8f;
    private static final int[] VERTEX_ATTRIBS = new int[]{
            2, 2
    };
    private static final String[] VERTEX_ATTRIB_NAMES = new String[]{
            A_POSITION, A_TEXCOORD
    };
    public static final int TOTAL_VERTEX_ATTRIB_COUNT = Arrays.stream(VERTEX_ATTRIBS).reduce(0, Integer::sum);
    private static final List<VertexAttrib> VERTEX_ATTRIB_LIST = getVertexAttribs(VERTEX_ATTRIB_NAMES, VERTEX_ATTRIBS);
    private final Config config;

    public DownsampleShaderProgram(Config config) {
        super(config.context, R.raw.down_sampler_vert, R.raw.down_sampler_frag2);
        this.config = config;
    }

    @Override
    public void useProgram() {
        super.useProgram();
        int uInverseScreenSize = getUniformLocation(U_TEXEL_STEP);
        GLES20.glUniform2f(uInverseScreenSize, 1.0f / config.getWidth(), 1.0f / config.getHeight());
        int uResolution = getUniformLocation(U_RESOLUTION);
        GLES20.glUniform2f(uResolution, config.getWidth(), config.getHeight());
        int uShowEdges = getUniformLocation(U_SHOW_EDGES);
        GLES20.glUniform1i(uShowEdges, SHOW_EDGES);
        int uFxaaOn = getUniformLocation(U_FXAA_ON);
        GLES20.glUniform1i(uFxaaOn, FXAA_ON);
        int uLumaThreshold = getUniformLocation(U_LUMA_THRESHOLD);
        GLES20.glUniform1f(uLumaThreshold, LUMA_THRESHOLD);
        int uMulReduce = getUniformLocation(U_MUL_REDUCE);
        GLES20.glUniform1f(uMulReduce, MUL_REDUCE);
        int uMinReduce = getUniformLocation(U_MIN_REDUCE);
        GLES20.glUniform1f(uMinReduce, MIN_REDUCE);
        int uMaxSpan = getUniformLocation(U_MAX_SPAN);
        GLES20.glUniform1f(uMaxSpan, MAX_SPAN);
    }

    public void deleteTexture(int textureId) {
        glDeleteTextures(1, new int[]{textureId}, 0);
    }

    public void setCurrentTexture(int textureId) {
        int uTextureUnit = getUniformLocation(U_TEXTURE_UNIT);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureUnit, 0);
    }

    @Override
    public List<VertexAttrib> getVertexAttribs() {
        return VERTEX_ATTRIB_LIST;
    }

    @Override
    public int getTotalVertexAttribCount() {
        return TOTAL_VERTEX_ATTRIB_COUNT;
    }
}
