/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/
package com.hcmut.test.object;

import android.content.Context;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.data.Way;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.programs.TextShaderProgram;

public class ObjectBuilder {
    ColorShaderProgram colorProgram;
    TextShaderProgram textProgram;
    HighwayDrawer highwayDrawer;
    AreaDrawer areaDrawer;
    TextDrawer textDrawer;


    public ObjectBuilder(Context context, ColorShaderProgram colorProgram, TextShaderProgram textProgram) {
        this.colorProgram = colorProgram;
        this.textProgram = textProgram;
        areaDrawer = new AreaDrawer(colorProgram);
        highwayDrawer = new HighwayDrawer(colorProgram);
        textDrawer = new TextDrawer(context, textProgram);
    }

    public void addWay(String key, Way way, float originX, float originY, float scale) {
        areaDrawer.addWay(key, way, originX, originY, scale);
        highwayDrawer.addWay(key, way, originX, originY, scale);
        textDrawer.addWay(key, way, originX, originY, scale, 0, 0, 0, 1);
    }

    public void finalizeDrawer() {
        highwayDrawer.finalizeDrawer();
        areaDrawer.finalizeDrawer();
        textDrawer.finalizeDrawer();
    }

    public void draw() {
        areaDrawer.draw();
        highwayDrawer.draw();
        textDrawer.draw();
    }
}
