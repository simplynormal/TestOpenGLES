package com.hcmut.test.mapnik.symbolizer;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.geometry.Polygon;
import com.hcmut.test.geometry.Triangle;
import com.hcmut.test.osm.Way;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// PolygonSymbolizer keys: [fill, fill-opacity, gamma, clip]
public class PolygonSymbolizer extends Symbolizer {
    private final float[] fillColor;

    public PolygonSymbolizer(Config config, String fill, String fillOpacity) {
        super(config);
        this.fillColor = parseColorString(fill, fillOpacity == null ? 1 : Float.parseFloat(fillOpacity));
    }

    @Override
    public float[] toDrawable(Way way, PointList shape) {
        if (!shape.isClosed()) return new float[0];

        Polygon polygon;
        if (!(shape instanceof Polygon)) {
            polygon = new Polygon(shape);
        } else {
            polygon = (Polygon) shape;
        }
        List<Triangle> curTriangulatedTriangles = polygon.triangulate();

        return ColorShaderProgram.toVertexData(new ArrayList<>() {
            {
                for (Triangle triangle : curTriangulatedTriangles) {
                    add(triangle.p1);
                    add(triangle.p2);
                    add(triangle.p3);
                }
            }
        }, fillColor);
    }

    @Override
    public void draw(VertexArray vertexArray) {
        if (vertexArray == null) return;
        vertexArray.setDataFromVertexData();
        int pointCount = vertexArray.getVertexCount();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, pointCount);
    }

    @Override
    public float[] appendDrawable(float[] oldDrawable, float[] newDrawable) {
        float[] result = new float[oldDrawable.length + newDrawable.length];
        System.arraycopy(oldDrawable, 0, result, 0, oldDrawable.length);
        System.arraycopy(newDrawable, 0, result, oldDrawable.length, newDrawable.length);
        return result;
    }

    @Override
    public boolean isAppendable() {
        return true;
    }

    @Override
    public void draw(VertexArray vertexArray, float[] rawDrawable) {
    }

    @NonNull
    @Override
    public String toString() {
        return "<PolygonSymbolizer fill=\"" + Arrays.toString(fillColor);
    }
}
