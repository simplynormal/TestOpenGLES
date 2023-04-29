package com.hcmut.test.mapnik.symbolizer;

import android.opengl.GLES20;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hcmut.test.algorithm.StrokeGenerator;
import com.hcmut.test.data.VertexArray;
import com.hcmut.test.geometry.LineStrip;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.geometry.Polygon;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.geometry.Vector;
import com.hcmut.test.osm.Element;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.programs.ShaderProgram;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// LineSymbolizer keys: [stroke-opacity, offset, stroke-linejoin, stroke-dasharray, stroke-width, stroke, clip, stroke-linecap]
public class LineSymbolizer extends Symbolizer {
    private static class LineSymMeta extends SymMeta {
        private float[] drawable;
        protected VertexArray vertexArray = null;

        public LineSymMeta(float[] drawable) {
            this.drawable = drawable;
        }

        @Override
        public boolean isEmpty() {
            return vertexArray == null && (drawable == null || drawable.length == 0);
        }

        @Override
        public SymMeta append(SymMeta other) {
            if (!(other instanceof LineSymMeta)) return this;
            LineSymMeta otherLineSymMeta = (LineSymMeta) other;
            float[] result = appendTriangleStrip(drawable, otherLineSymMeta.drawable, ColorShaderProgram.TOTAL_VERTEX_ATTRIB_COUNT);
            return new LineSymMeta(result);
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
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, pointCount);
        }
    }

    private final float strokeWidth;
    private final float[] strokeColor;
    private final List<Float> strokeDashArray;
    private final StrokeGenerator.StrokeLineCap strokeLineCap;
    private final StrokeGenerator.StrokeLineJoin strokeLineJoin;
    private final float offset;

    public LineSymbolizer(Config config, @Nullable String strokeWidth, @Nullable String strokeColor, @Nullable String strokeDashArray, @Nullable String strokeLineCap, @Nullable String strokeLineJoin, @Nullable String strokeOpacity, String offset) {
        super(config);
        this.strokeWidth = strokeWidth == null ? 0 : Float.parseFloat(strokeWidth);
        this.strokeColor = parseColorString(strokeColor, strokeOpacity == null ? 1 : Float.parseFloat(strokeOpacity));
        this.strokeDashArray = parseStrokeDashArray(strokeDashArray);
        this.strokeLineCap = parseStrokeLineCap(strokeLineCap);
        this.strokeLineJoin = parseStrokeLineJoin(strokeLineJoin);
        this.offset = offset == null ? 0 : Float.parseFloat(offset);
    }

    private static List<Float> parseStrokeDashArray(String dashArray) {
        if (dashArray == null) return null;
        List<Float> dashArrayList = new ArrayList<>();
        String[] dashArraySplit = dashArray.split(",");
        if (dashArraySplit.length == 0) return null;
        if (dashArraySplit.length % 2 != 0) {
            // Duplicate the list
            String[] dashArraySplitNew = new String[dashArraySplit.length * 2];
            System.arraycopy(dashArraySplit, 0, dashArraySplitNew, 0, dashArraySplit.length);
            System.arraycopy(dashArraySplit, 0, dashArraySplitNew, dashArraySplit.length, dashArraySplit.length);
            dashArraySplit = dashArraySplitNew;
        }

        for (int i = 0; i < dashArraySplit.length; i += 2) {
            float length = Float.parseFloat(dashArraySplit[i]);
            float gap = Float.parseFloat(dashArraySplit[i + 1]);
            dashArrayList.add(length);
            dashArrayList.add(gap);
        }
        return dashArrayList;
    }

    private static StrokeGenerator.StrokeLineCap parseStrokeLineCap(String strokeLineCap) {
        if (strokeLineCap == null) return StrokeGenerator.StrokeLineCap.BUTT;
        switch (strokeLineCap) {
            case "round":
                return StrokeGenerator.StrokeLineCap.ROUND;
            case "square":
                return StrokeGenerator.StrokeLineCap.SQUARE;
            default:
                return StrokeGenerator.StrokeLineCap.BUTT;
        }
    }

    private static StrokeGenerator.StrokeLineJoin parseStrokeLineJoin(String strokeLineJoin) {
        if (strokeLineJoin == null) return StrokeGenerator.StrokeLineJoin.MITER;
        switch (strokeLineJoin) {
            case "round":
                return StrokeGenerator.StrokeLineJoin.ROUND;
            case "bevel":
                return StrokeGenerator.StrokeLineJoin.BEVEL;
            default:
                return StrokeGenerator.StrokeLineJoin.MITER;
        }
    }

    private List<Float> dashArrayToLength(List<Float> strokeDashArray) {
        List<Float> lengths = new ArrayList<>();
        for (int i = 0; i < strokeDashArray.size(); i += 2) {
            float lengthPerPixel = config.getLengthPerPixel();
            float length = strokeDashArray.get(i) * lengthPerPixel;
            float gap = strokeDashArray.get(i + 1) * lengthPerPixel;
            lengths.add(length);
            lengths.add(gap);
        }
        return lengths;
    }

    private LineStrip convertToOffsetLineStrip(LineStrip lineStrip) {
        return StrokeGenerator.offsetLineStrip(lineStrip, offset * config.getLengthPerPixel(), strokeLineJoin);
    }

    private List<LineStrip> convertToDashedLineStrips(LineStrip linestrip, List<Float> strokeDashArray) {
        List<LineStrip> dashedLineStrips = new ArrayList<>();
        boolean isDash = true;
        int dashIndex = 0;

        List<Point> currentSegment = new ArrayList<>();
        List<Float> convertedDashArray = dashArrayToLength(strokeDashArray);
        float remainingDashLength = convertedDashArray.get(dashIndex);

        List<Point> lineStripPoints = linestrip.points;
        for (int i = 0; i < lineStripPoints.size() - 1; i++) {
            Point p1 = lineStripPoints.get(i);
            Point p2 = lineStripPoints.get(i + 1);
            Vector v12 = new Vector(p1, p2);
            Point curP = p1;

            float segmentLength = curP.distance(p2);
            float remainingSegmentLength = segmentLength;

            while (remainingSegmentLength > 0) {
                float splitLength = Math.min(remainingDashLength, remainingSegmentLength);

                float ratio = splitLength / segmentLength;
                Point splitPoint = curP.add(v12.mul(ratio));

                if (isDash) {
                    currentSegment.add(curP);
                    currentSegment.add(splitPoint);
                }

                curP = splitPoint;
                remainingSegmentLength -= splitLength;
                remainingDashLength -= splitLength;

                if (remainingDashLength <= 0) {
                    isDash = !isDash;
                    dashIndex = (dashIndex + 1) % convertedDashArray.size();
                    remainingDashLength = convertedDashArray.get(dashIndex);
                    if (currentSegment.size() > 0) {
                        dashedLineStrips.add(new LineStrip(currentSegment));
                    }
                    currentSegment = new ArrayList<>();
                }
            }
        }

        return dashedLineStrips;
    }

    private float[] drawLineStrip(LineStrip lineStrip) {
        float strokeWidth = this.strokeWidth * config.getLengthPerPixel();
//        System.out.println("Rendering line with stroke width: " + strokeWidth);
        StrokeGenerator.Stroke stroke = StrokeGenerator.generateStroke(lineStrip
                , 8, strokeWidth / 2, strokeLineCap);
        TriangleStrip triangleStrip = stroke.toTriangleStrip();
        return ColorShaderProgram.toVertexData(triangleStrip, strokeColor);
    }

    @Override
    public SymMeta toDrawable(Element element, PointList shape) {
        LineStrip lineStrip;
        if (!(shape instanceof LineStrip)) {
            lineStrip = new LineStrip(shape);
        } else {
            lineStrip = (LineStrip) shape;
        }

        lineStrip = convertToOffsetLineStrip(lineStrip);

        SymMeta rv = new LineSymMeta(new float[0]);

        if (strokeDashArray != null) {
            List<LineStrip> dashedLineStrips = convertToDashedLineStrips(lineStrip, strokeDashArray);
            for (LineStrip dashedLineStrip : dashedLineStrips) {
                rv = rv.append(new LineSymMeta(drawLineStrip(dashedLineStrip)));
            }
            return rv;
        } else {
            rv = new LineSymMeta(drawLineStrip(lineStrip));
        }

        if (shape instanceof Polygon) {
            Polygon polygon = (Polygon) shape;
            for (Polygon hole : polygon.holes) {
                rv = rv.append(new LineSymMeta(drawLineStrip(new LineStrip(hole))));
            }
        }

        return rv;
    }

    @Override
    public void draw(SymMeta symMeta) {
        if (!(symMeta instanceof LineSymMeta)) return;
        LineSymMeta lineSymMeta = (LineSymMeta) symMeta;
        lineSymMeta.draw(config.colorShaderProgram);
    }

    @NonNull
    @Override
    public String toString() {
        return "<LineSymbolizer stroke-width=\"" + strokeWidth + "\" stroke=\"" + Arrays.toString(strokeColor) + "\" stroke-dasharray=\"" + strokeDashArray + "\" stroke-linecap=\"" + strokeLineCap + "\" stroke-linejoin=\"" + strokeLineJoin + "\" offset=\"" + offset + "\" />";
    }
}
