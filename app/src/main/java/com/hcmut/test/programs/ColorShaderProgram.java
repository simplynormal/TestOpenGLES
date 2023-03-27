/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/
package com.hcmut.test.programs;

import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniformMatrix4fv;

import android.content.Context;

import com.hcmut.test.R;

import java.util.HashMap;

public class ColorShaderProgram extends ShaderProgram {
    public static final String U_PROJECTION_MATRIX = "u_ProjectionMatrix";
    public static final String U_MODEL_VIEW_MATRIX = "u_ModelViewMatrix";
    public static final String A_POSITION = "a_Position";
    public static final String A_COLOR = "a_Color";

    public ColorShaderProgram(Context context) {
        super(context, R.raw.vert_shader,
                R.raw.frag_shader);
    }


    public int getUniformLocation(String uniformName) {
        return glGetUniformLocation(program, uniformName);
    }

    public int getAttributeLocation(String attributeName) {
        return glGetAttribLocation(program, attributeName);
    }
}
