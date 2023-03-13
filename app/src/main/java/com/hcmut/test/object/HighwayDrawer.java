package com.hcmut.test.object;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glUniformMatrix4fv;

import com.hcmut.test.algorithm.BorderGenerator;
import com.hcmut.test.algorithm.StrokeGenerator;
import com.hcmut.test.algorithm.UnionPolygons;
import com.hcmut.test.data.VertexArray;
import com.hcmut.test.data.VertexData;
import com.hcmut.test.data.Way;
import com.hcmut.test.geometry.LineStrip;
import com.hcmut.test.geometry.Polygon;
import com.hcmut.test.geometry.Triangle;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.programs.ColorShaderProgram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HighwayDrawer extends Drawable {
    float[] projectionMatrix;
    HashMap<String, Way> ways = new HashMap<>();
    List<Triangle> wayTriangles = new ArrayList<>();
    TriangleStrip wayBorderTriangleStrips = null;
    float originX = 0;
    float originY = 0;
    float scale = 1;

    public HighwayDrawer(ColorShaderProgram colorShaderProgram, float[] projectionMatrix) {
        super(colorShaderProgram);
        this.projectionMatrix = projectionMatrix;
    }

    public void addWay(String key, Way way, float originX, float originY, float scale) {
        ways.put(key, way);
        this.originX = originX;
        this.originY = originY;
        this.scale = scale;
    }

    public void removeWay(String key) {
        ways.remove(key);
        wayTriangles.remove(key);
    }

    public void finalizeDrawer() {
        List<Polygon> wayPolygons = new ArrayList<>();
        for (Way way : ways.values()) {
            LineStrip linePoints = new LineStrip(way.toPoints(originX, originY, scale));
            Polygon polygon = StrokeGenerator.generateStroke(linePoints, 8, 0.02f);
            wayPolygons.add(polygon);
        }

        List<Polygon> unionPolygons = UnionPolygons.union(wayPolygons);
        for (Polygon polygon : unionPolygons) {
            List<Triangle> curTriangulatedTriangles = polygon.triangulate();
            wayTriangles.addAll(curTriangulatedTriangles);
            TriangleStrip border = BorderGenerator.generateBorderFromPolygon(polygon, curTriangulatedTriangles, 0.002f);
            if (wayBorderTriangleStrips == null) {
                wayBorderTriangleStrips = border;
            } else {
                wayBorderTriangleStrips.extend(border);
            }
        }
    }

    private void drawHighway() {
        float[] triangleVertexData = Triangle.toVertexData(wayTriangles, 1, 1, 1, 1);

        VertexArray vertexArray = new VertexArray(triangleVertexData);
        vertexArray.setDataFromVertexData(shaderProgram);

        int strideInElements = VertexData.getTotalComponentCount();
        glDrawArrays(GL_TRIANGLES, 0, triangleVertexData.length / strideInElements);
    }

    private void drawBorder() {
        float[] borderVertexData = wayBorderTriangleStrips.toVertexData(0.5f, 0.5f, 0.5f, 1);

        VertexArray vertexArray = new VertexArray(borderVertexData);
        vertexArray.setDataFromVertexData(shaderProgram);

        int strideInElements = VertexData.getTotalComponentCount();
        glDrawArrays(GL_TRIANGLE_STRIP, 0, borderVertexData.length / strideInElements);
    }

    public void draw() {
        shaderProgram.useProgram();
        int uMVPLocation = shaderProgram.getUniformLocation(ColorShaderProgram.U_MATRIX);
        glUniformMatrix4fv(uMVPLocation, 1, false, projectionMatrix, 0);
        drawHighway();
        drawBorder();
    }
}
