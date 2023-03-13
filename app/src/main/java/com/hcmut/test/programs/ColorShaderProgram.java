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
    public static final String U_MATRIX = "u_Matrix";
    public static final String A_POSITION = "a_Position";
    public static final String A_COLOR = "a_Color";

    private final HashMap<String, Integer> mUniformLocations = new HashMap<String, Integer>();
    private final HashMap<String, Integer> mAttributeLocations = new HashMap<String, Integer>();

    public ColorShaderProgram(Context context) {
        super(context, R.raw.vert_shader,
                R.raw.frag_shader);

        // Retrieve uniform locations for the shader program.
        // Uniform locations
        int uMatrixLocation = glGetUniformLocation(program, U_MATRIX);

        mUniformLocations.put(U_MATRIX, uMatrixLocation);

        // Retrieve attribute locations for the shader program.
        // Attribute locations
        int aPositionLocation = glGetAttribLocation(program, A_POSITION);
        int aColorLocation = glGetAttribLocation(program, A_COLOR);

        mAttributeLocations.put(A_POSITION, aPositionLocation);
        mAttributeLocations.put(A_COLOR, aColorLocation);
    }

    public int getUniformLocation(String uniformName) {
        Integer location = mUniformLocations.get(uniformName);
        if (location == null) {
            location = glGetUniformLocation(program, uniformName);
            mUniformLocations.put(uniformName, location);
        }
        return location;
    }

    public int getAttributeLocation(String attributeName) {
        Integer location = mAttributeLocations.get(attributeName);
        if (location == null) {
            location = glGetAttribLocation(program, attributeName);
            mAttributeLocations.put(attributeName, location);
        }
        return location;
    }
}
