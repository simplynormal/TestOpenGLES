package com.hcmut.test.object;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glUniformMatrix4fv;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.data.Way;
import com.hcmut.test.geometry.Polygon;
import com.hcmut.test.geometry.Triangle;
import com.hcmut.test.programs.ColorShaderProgram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AreaDrawer extends Drawable {
    HashMap<String, Way> ways = new HashMap<>();
    List<Triangle> wayTriangles = new ArrayList<>();
    VertexArray wayVertexArray;
    float originX = 0;
    float originY = 0;
    float scale = 1;

    public AreaDrawer(ColorShaderProgram colorShaderProgram) {
        super(colorShaderProgram);
    }

    public void addWay(String key, Way way, float originX, float originY, float scale) {
        addWay(key, way);
        this.originX = originX;
        this.originY = originY;
        this.scale = scale;
    }

    public void addWay(String key, Way way) {
        ways.put(key, way);
    }

    public void finalizeDrawer() {
        if (ways.isEmpty()) {
            return;
        }

        for (Way way : ways.values()) {
            Polygon polygon = new Polygon(way.toPoints(originX, originY, scale));
            List<Triangle> curTriangulatedTriangles = polygon.triangulate();
            wayTriangles.addAll(curTriangulatedTriangles);
        }

        float[] triangleVertexData = Triangle.toVertexData(wayTriangles, 0.67f, 0.83f, 0.87f, 1f);
        wayVertexArray = new VertexArray(triangleVertexData);
    }

    public void draw() {
        if (ways.isEmpty()) {
            return;
        }

        shaderProgram.useProgram();
        wayVertexArray.setDataFromVertexData(shaderProgram);
        glDrawArrays(GL_TRIANGLES, 0, wayTriangles.size() * 3);
    }
}
