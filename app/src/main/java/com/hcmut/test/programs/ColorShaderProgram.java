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
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;

import android.content.Context;

import com.hcmut.test.R;

import java.util.HashMap;

public class ColorShaderProgram extends ShaderProgram {
    // Uniform locations
    private final int uMatrixLocation;
    private final int uColorLocation;

    // Attribute locations
    private final int aPositionLocation;
    private HashMap<String, Integer> mUniformLocations = new HashMap<String, Integer>();
    private HashMap<String, Integer> mAttributeLocations = new HashMap<String, Integer>();

    public ColorShaderProgram(Context context) {
        super(context, R.raw.vert_shader,
                R.raw.frag_shader);

        // Retrieve uniform locations for the shader program.
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
        uColorLocation = glGetUniformLocation(program, U_COLOR);

        mUniformLocations.put(U_MATRIX, uMatrixLocation);
        mUniformLocations.put(U_COLOR, uColorLocation);

        // Retrieve attribute locations for the shader program.
        aPositionLocation = glGetAttribLocation(program, A_POSITION);

        mAttributeLocations.put(A_POSITION, aPositionLocation);
    }

//    public void setUniformMVP(float[] mvp) {
//        glUniformMatrix4fv(uMatrixLocation, 1, false, mvp, 0);
//    }
//
//    public void setUniformColor(float r, float g, float b) {
//        glUniform4f(uColorLocation, r, g, b, 1f);
//    }
//
//    public int getPositionAttributeLocation() {
//        return aPositionLocation;
//    }

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
