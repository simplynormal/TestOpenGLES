package com.hcmut.test.mapnik.symbolizer;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.geometry.Polygon;
import com.hcmut.test.geometry.Triangle;
import com.hcmut.test.osm.Element;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.programs.ShaderProgram;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// PolygonSymbolizer keys: [fill, fill-opacity, gamma, clip]
public class PolygonSymbolizer extends Symbolizer {
    private static class PolygonSymMeta extends SymMeta {
        private float[] drawable;
        protected VertexArray vertexArray = null;

        public PolygonSymMeta(float[] drawable) {
            this.drawable = drawable;
        }

        @Override
        public boolean isEmpty() {
            return vertexArray == null && (drawable == null || drawable.length == 0);
        }

        @Override
        public SymMeta append(SymMeta other) {
            if (!(other instanceof PolygonSymMeta)) return this;
            PolygonSymMeta otherLineSymMeta = (PolygonSymMeta) other;
            float[] result = appendRegular(drawable, otherLineSymMeta.drawable);
            return new PolygonSymMeta(result);
        }

        private void draw(ShaderProgram shaderProgram) {
            if (isEmpty()) return;
            if (vertexArray == null) {
                vertexArray = new VertexArray(shaderProgram, drawable);
                drawable = null;
            }
            shaderProgram.useProgram();
            vertexArray.setDataFromVertexData();
            int pointCount = vertexArray.getVertexCount();
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, pointCount);
        }
    }

    private final float[] fillColor;

    public PolygonSymbolizer(Config config, String fill, String fillOpacity) {
        super(config);
        this.fillColor = parseColorString(fill, fillOpacity == null ? 1 : Float.parseFloat(fillOpacity));
    }

    @Override
    public SymMeta toDrawable(Element element, PointList shape) {
        if (!shape.isClosed()) return new PolygonSymMeta(new float[0]);

        Polygon polygon;
        if (!(shape instanceof Polygon)) {
            polygon = new Polygon(shape);
        } else {
            polygon = (Polygon) shape;
        }
        List<Triangle> curTriangulatedTriangles = polygon.triangulate();

        return new PolygonSymMeta(ColorShaderProgram.toVertexData(new ArrayList<>() {
            {
                for (Triangle triangle : curTriangulatedTriangles) {
                    add(triangle.p1);
                    add(triangle.p2);
                    add(triangle.p3);
                }
            }
        }, fillColor));
    }

    @Override
    public void draw(SymMeta symMeta) {
        if (!(symMeta instanceof PolygonSymMeta)) return;
        PolygonSymMeta polygonSymMeta = (PolygonSymMeta) symMeta;
        polygonSymMeta.draw(config.colorShaderProgram);
    }

    @NonNull
    @Override
    public String toString() {
        return "<PolygonSymbolizer fill=\"" + Arrays.toString(fillColor);
    }
}
