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
import com.hcmut.test.osm.Way;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// LineSymbolizer keys: [stroke-opacity, offset, stroke-linejoin, stroke-dasharray, stroke-width, stroke, clip, stroke-linecap]
public class LineSymbolizer extends Symbolizer {
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

//    private LineStrip convertToOffsetLineStrip(LineStrip lineStrip) {
//        if (this.offset == 0) return lineStrip;
//        float offset = this.offset * config.getLengthPerPixel();
//
//        List<Point> points = lineStrip.points;
//        List<Point> offsetPoints = new ArrayList<>();
//        Point oldOffsetP2 = null;
//        for (int i = 0; i < points.size() - 1; i++) {
//            Point p1 = points.get(i);
//            Point p2 = points.get(i + 1);
//            Vector v12 = new Vector(p1, p2);
//            Vector v12Perp = v12.orthogonal2d();
//            v12Perp = v12Perp.normalize().mul(offset);
//            Point offsetP1 = p1.add(v12Perp);
//            Point offsetP2 = p2.add(v12Perp);
//            if (oldOffsetP2 != null) {
//                Vector vOld = new Vector(p1, oldOffsetP2);
//                Vector vNew = new Vector(p1, offsetP1);
//            }
//            oldOffsetP2 = offsetP2;
//            offsetPoints.add(offsetP1);
//        }
//        offsetPoints.add(oldOffsetP2);
//        return new LineStrip(offsetPoints);
//    }

    public LineStrip convertToOffsetLineStrip(LineStrip lineStrip) {
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
    public float[] toDrawable(Way way, PointList shape) {
        LineStrip lineStrip;
        if (!(shape instanceof LineStrip)) {
            lineStrip = new LineStrip(shape);
        } else {
            lineStrip = (LineStrip) shape;
        }

        lineStrip = convertToOffsetLineStrip(lineStrip);

        float[] rv = new float[0];

        if (strokeDashArray != null) {
            List<LineStrip> dashedLineStrips = convertToDashedLineStrips(lineStrip, strokeDashArray);
            for (LineStrip dashedLineStrip : dashedLineStrips) {
                rv = appendDrawable(rv, drawLineStrip(dashedLineStrip));
            }
            return rv;
        } else {
            rv = drawLineStrip(lineStrip);
        }

        if (shape instanceof Polygon) {
            Polygon polygon = (Polygon) shape;
            for (Polygon hole : polygon.holes) {
                rv = appendDrawable(rv, drawLineStrip(new LineStrip(hole)));
            }
        }

        return rv;
    }

    @Override
    public void draw(VertexArray vertexArray) {
        if (vertexArray == null) return;
        config.colorShaderProgram.useProgram();
        vertexArray.setDataFromVertexData();
        int pointCount = vertexArray.getVertexCount();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, pointCount);
//        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pointCount);
    }

    @Override
    public float[] appendDrawable(float[] oldDrawable, float[] newDrawable) {
        if (newDrawable.length == 0) return oldDrawable;

        boolean oldDrawableEmpty = oldDrawable.length == 0;
        float[] result = new float[oldDrawable.length + newDrawable.length + (oldDrawableEmpty ? 0 : ColorShaderProgram.TOTAL_VERTEX_ATTRIB_COUNT * 2)];
        System.arraycopy(oldDrawable, 0, result, 0, oldDrawable.length);
        if (!oldDrawableEmpty) {
            System.arraycopy(oldDrawable, oldDrawable.length - ColorShaderProgram.TOTAL_VERTEX_ATTRIB_COUNT, result, oldDrawable.length, ColorShaderProgram.TOTAL_VERTEX_ATTRIB_COUNT);
            System.arraycopy(newDrawable, 0, result, oldDrawable.length + ColorShaderProgram.TOTAL_VERTEX_ATTRIB_COUNT, ColorShaderProgram.TOTAL_VERTEX_ATTRIB_COUNT);
        }
        System.arraycopy(newDrawable, 0, result, oldDrawable.length + (oldDrawableEmpty ? 0 : ColorShaderProgram.TOTAL_VERTEX_ATTRIB_COUNT * 2), newDrawable.length);
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
        return "<LineSymbolizer stroke-width=\"" + strokeWidth + "\" stroke=\"" + Arrays.toString(strokeColor) + "\" stroke-dasharray=\"" + strokeDashArray + "\" stroke-linecap=\"" + strokeLineCap + "\" stroke-linejoin=\"" + strokeLineJoin + "\" offset=\"" + offset + "\" />";
    }
}
