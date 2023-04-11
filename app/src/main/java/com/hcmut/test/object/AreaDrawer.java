package com.hcmut.test.object;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glUniformMatrix4fv;

import android.opengl.GLES20;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.data.Way;
import com.hcmut.test.geometry.Polygon;
import com.hcmut.test.geometry.Triangle;
import com.hcmut.test.programs.ColorShaderProgram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AreaDrawer extends Drawable {
    private static final float Z_FOR_BELOW = -8e-4f;
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
        if (!way.isClosed() || way.tags.containsKey("highway")) return;

        ways.put(key, way);
    }

    public void finalizeDrawer() {
        if (ways.isEmpty()) {
            return;
        }

        float[] wayVert = new float[0];
        for (Way way : ways.values()) {
            Polygon polygon = new Polygon(way.toPoints(originX, originY, scale, Z_FOR_BELOW));
            List<Triangle> curTriangulatedTriangles = polygon.triangulate();
            wayTriangles.addAll(curTriangulatedTriangles);
            float[] curWayVert = VertexArray.toVertexData(shaderProgram, new ArrayList<>() {
                {
                    for (Triangle triangle : curTriangulatedTriangles) {
                        add(triangle.p1);
                        add(triangle.p2);
                        add(triangle.p3);
                    }
                }
            });

            float[] newWayVert = new float[wayVert.length + curWayVert.length];
            System.arraycopy(wayVert, 0, newWayVert, 0, wayVert.length);
            System.arraycopy(curWayVert, 0, newWayVert, wayVert.length, curWayVert.length);
            wayVert = newWayVert;
        }

        wayVertexArray = new VertexArray(shaderProgram, wayVert);
    }

    public void draw() {
        if (ways.isEmpty()) {
            return;
        }

        shaderProgram.useProgram();
        wayVertexArray.setDataFromVertexData();
        glDrawArrays(GL_TRIANGLES, 0, wayTriangles.size() * 3);
    }
}
