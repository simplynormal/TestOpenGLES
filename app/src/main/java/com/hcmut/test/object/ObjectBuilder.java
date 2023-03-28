/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/
package com.hcmut.test.object;

import com.hcmut.test.data.VertexData;
import com.hcmut.test.data.Way;
import com.hcmut.test.programs.ColorShaderProgram;

public class ObjectBuilder {
    ColorShaderProgram colorProgram;
    HighwayDrawer highwayDrawer;
    AreaDrawer areaDrawer;


    public ObjectBuilder(ColorShaderProgram colorProgram) {
        VertexData.resetRandom();
        this.colorProgram = colorProgram;
        areaDrawer = new AreaDrawer(colorProgram);
        highwayDrawer = new HighwayDrawer(colorProgram);
    }

    public void addWay(String key, Way way, float originX, float originY, float scale) {
        if (way.isClosed()) {
            areaDrawer.addWay(key, way, originX, originY, scale);
        } else {
            highwayDrawer.addWay(key, way, originX, originY, scale);
        }
    }

    public void finalizeDrawer() {
        highwayDrawer.finalizeDrawer();
        areaDrawer.finalizeDrawer();
    }

    public void draw() {
        VertexData.resetRandom();

        areaDrawer.draw();
        highwayDrawer.draw();
    }
}
