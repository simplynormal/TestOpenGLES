package com.hcmut.test.object;

import com.hcmut.test.programs.ShaderProgram;

public abstract class Drawable {
    protected ShaderProgram shaderProgram;

    public abstract void draw();
    public abstract void finalizeDrawer();

    public Drawable(ShaderProgram shaderProgram) {
        this.shaderProgram = shaderProgram;
    }
}
