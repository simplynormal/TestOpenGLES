package com.hcmut.test.mapnik.symbolizer;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hcmut.test.algorithm.StrokeGenerator;
import com.hcmut.test.data.VertexArray;
import com.hcmut.test.geometry.LineStrip;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.geometry.Polygon;
import com.hcmut.test.geometry.Triangle;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.osm.Element;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.programs.ShaderProgram;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressLint("NewApi")
public class TextSymbolizer extends Symbolizer {
    private static final int NUM_POINTS_PER_TEXT_PATH = 40;
    private static class TextSymMeta extends SymMeta {
        private float[] triDrawable;
        private float[] triStripDrawable;
        private int firstHalfCount = 0;
        protected VertexArray vertexArray = null;

        public TextSymMeta() {
            this.triDrawable = new float[0];
            this.triStripDrawable = new float[0];
        }

        public TextSymMeta(float[] triDrawable, float[] triStripDrawable) {
            this.triDrawable = triDrawable;
            this.triStripDrawable = triStripDrawable;
        }

        @Override
        public boolean isEmpty() {
            return vertexArray == null && ((triDrawable == null || triDrawable.length == 0) && (triStripDrawable == null || triStripDrawable.length == 0));
        }

        @Override
        public SymMeta append(SymMeta other) {
            if (!(other instanceof TextSymMeta)) return this;
            TextSymMeta otherLineSymMeta = (TextSymMeta) other;
            float[] triDrawable = appendRegular(this.triDrawable, otherLineSymMeta.triDrawable);
            float[] triStripDrawable = appendTriangleStrip(this.triStripDrawable, otherLineSymMeta.triStripDrawable, ColorShaderProgram.TOTAL_VERTEX_ATTRIB_COUNT);
            return new TextSymMeta(triDrawable, triStripDrawable);
        }

        private void draw(ShaderProgram shaderProgram) {
            if (isEmpty()) return;
            if (vertexArray == null) {
                float[] drawable = appendRegular(triStripDrawable, triDrawable);
                firstHalfCount = triStripDrawable.length / ColorShaderProgram.TOTAL_VERTEX_ATTRIB_COUNT;
                vertexArray = new VertexArray(shaderProgram, drawable);
                triDrawable = null;
                triStripDrawable = null;
            }
            shaderProgram.useProgram();
            vertexArray.setDataFromVertexData();
            int pointCount = vertexArray.getVertexCount();
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, firstHalfCount);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, firstHalfCount, pointCount - firstHalfCount);
        }
    }

    /*
     * textName
     * dx
     * dy
     * spacing
     * repeatDistance
     * maxCharAngleDelta
     * fill
     * opacity
     * placement
     * verticalAlignment
     * horizontalAlignment
     * justifyAlignment
     * wrapWidth
     * size
     * haloFill
     * haloRadius
     * */
    private final Function<HashMap<String, String>, String> textExprEvaluator;
    private final String textExprString;
    private final float dx;
    private final float dy;
    private final float spacing;
    private final float repeatDistance;
    private final float maxCharAngleDelta;
    private final float[] fillColor;
    private final String placement;
    private final String verticalAlignment;
    private final String horizontalAlignment;
    private final String justifyAlignment;
    private final float wrapWidth;
    private final float size;
    private final float[] haloFill;
    private final float haloRadius;
    private static Typeface tf = null;
    private static Typeface tf1 = null;

    public TextSymbolizer(Config config, @Nullable String textExpr, @Nullable String dx, @Nullable String dy, @Nullable String spacing, @Nullable String repeatDistance, @Nullable String maxCharAngleDelta, @Nullable String fill, @Nullable String opacity, @Nullable String placement, @Nullable String verticalAlignment, @Nullable String horizontalAlignment, @Nullable String justifyAlignment, @Nullable String wrapWidth, @Nullable String size, @Nullable String haloFill, @Nullable String haloRadius) {
        super(config);
        if (tf == null && tf1 == null) {
            tf = Typeface.createFromAsset(config.context.getAssets(), "NotoSans-Regular.ttf");
            tf1 = Typeface.createFromAsset(config.context.getAssets(), "NotoSans-Italic.ttf");
        }
        this.textExprString = textExpr;
        this.textExprEvaluator = createTextExprEvaluator(textExpr);
        this.dx = dx == null ? 0 : Float.parseFloat(dx);
        this.dy = dy == null ? 0 : Float.parseFloat(dy);
        this.spacing = spacing == null ? 0 : Float.parseFloat(spacing);
        this.repeatDistance = repeatDistance == null ? 0 : Float.parseFloat(repeatDistance);
        this.maxCharAngleDelta = maxCharAngleDelta == null ? 22.5f : Float.parseFloat(maxCharAngleDelta);
        this.fillColor = parseColorString(fill, opacity == null ? 1 : Float.parseFloat(opacity));
        this.placement = placement == null ? "point" : placement;
        this.verticalAlignment = parseVerticalAlignment(verticalAlignment);
        this.horizontalAlignment = parseHorizontalAlignment(horizontalAlignment);
        this.justifyAlignment = parseJustifyAlignment(justifyAlignment);
        this.wrapWidth = wrapWidth == null ? 0 : Float.parseFloat(wrapWidth);
        this.size = size == null ? 10 : Float.parseFloat(size);
        this.haloFill = parseColorString(haloFill, 1);
        this.haloRadius = haloRadius == null ? 0 : Float.parseFloat(haloRadius);
    }

    public static Function<HashMap<String, String>, String> createTextExprEvaluator(String template) {
        if (template == null || template.isEmpty()) {
            return tags -> null;
        }

        Pattern pattern = Pattern.compile("(\\[.+?])|('.+?')");
        Matcher matcher = pattern.matcher(template);
        List<Pair<String, Boolean>> expressions = new ArrayList<>(); // <tag, isLiteral>

        while (matcher.find()) {
            String tag = matcher.group(1);
            String literal = matcher.group(2);

            if (tag != null) {
                expressions.add(new Pair<>(tag.substring(1, tag.length() - 1), false));
            } else if (literal != null) {
                expressions.add(new Pair<>(literal.substring(1, literal.length() - 1), true));
            }
        }

        return tags -> {
            StringBuilder sb = new StringBuilder();
            for (Pair<String, Boolean> expression : expressions) {
                String expr = expression.first;
                Boolean isLiteral = expression.second;
                if (isLiteral) {
                    sb.append(expr);
                } else {
                    String replacement = tags.get(expr);
                    if (replacement == null) {
                        return null;
                    }
                    sb.append(replacement);
                }
            }
            return sb.toString();
        };
    }

    private String parseVerticalAlignment(String verticalAlignment) {
        switch (verticalAlignment == null ? "auto" : verticalAlignment) {
            case "top":
                return "top";
            case "middle":
                return "middle";
            case "bottom":
                return "bottom";
            default:
                if (dy > 0) {
                    return "bottom";
                } else if (dy < 0) {
                    return "top";
                } else {
                    return "middle";
                }
        }
    }

    private String parseHorizontalAlignment(String horizontalAlignment) {
        switch (horizontalAlignment == null ? "auto" : horizontalAlignment) {
            case "left":
                return "left";
            case "middle":
                return "middle";
            case "right":
                return "right";
            default:
                if (placement.equals("line") || placement.equals("point")) {
                    return "middle";
                } else {
                    if (dx > 0) {
                        return "left";
                    } else if (dx < 0) {
                        return "right";
                    } else {
                        return "middle";
                    }
                }
        }
    }

    private String parseJustifyAlignment(String justifyAlignment) {
        switch (justifyAlignment == null ? "auto" : justifyAlignment) {
            case "left":
                return "left";
            case "center":
                return "center";
            case "right":
                return "right";
            default:
                switch (horizontalAlignment) {
                    case "left":
                        return "left";
                    case "right":
                        return "right";
                    default:
                        return "center";
                }
        }
    }

    @Override
    public SymMeta toDrawable(Element element, PointList shape) {
        String name = textExprEvaluator.apply(element.tags);
        if (name == null) {
            return new TextSymMeta();
        }
        Paint paint = new Paint();
        paint.setTypeface(tf);
        paint.setTextSize(size);
        List<Point> points = shape.points;

        switch (placement) {
            case "line":
                return drawLinePlacement(name, points, paint);
        }

        return new TextSymMeta();
    }

    private float[] drawLineStrip(LineStrip lineStrip, float strokeWidth, float[] strokeColor) {
        if (lineStrip == null || strokeWidth <= 0) {
            return new float[0];
        }
        StrokeGenerator.Stroke stroke = StrokeGenerator.generateStroke(lineStrip
                , 8, strokeWidth / 2, StrokeGenerator.StrokeLineCap.ROUND);
        TriangleStrip triangleStrip = stroke.toTriangleStrip();
        return ColorShaderProgram.toVertexData(triangleStrip, strokeColor);
    }

    private SymMeta drawLinePlacement(String name, List<Point> points, Paint paint) {
        TextSymMeta rv = new TextSymMeta();
        GetLinePathResult result = getLinePath(paint, name, points);
        if (result == null) {
            return rv;
        }

        List<Polygon> polygons = createTextPathOnPath(result.path, name, result.hOffset, result.vOffset, paint);

        float lengthPerPixel = config.getLengthPerPixel();
        for (Polygon polygon : polygons) {
            if (polygon != null) {
                Polygon transformedPolygon = polygon.transform(0, 0, lengthPerPixel);
                List<Triangle> curTriangulatedTriangles = transformedPolygon.triangulate();

                float[] curTri = ColorShaderProgram.toVertexData(new ArrayList<>() {
                    {
                        for (Triangle triangle : curTriangulatedTriangles) {
                            add(triangle.p1);
                            add(triangle.p2);
                            add(triangle.p3);
                        }
                    }
                }, fillColor);
                float strokeWidth = haloRadius * lengthPerPixel;
                float[] curTriStrip = drawLineStrip(new LineStrip(transformedPolygon), strokeWidth, haloFill);
                for (Polygon hole : transformedPolygon.holes) {
                    curTriStrip = SymMeta.appendTriangleStrip(curTriStrip, drawLineStrip(new LineStrip(hole), strokeWidth, haloFill), ColorShaderProgram.TOTAL_VERTEX_ATTRIB_COUNT);
                }

                rv = (TextSymMeta) rv.append(new TextSymMeta(curTri, curTriStrip));
            }
        }

        return rv;
    }


    private Path pointsToPath(List<Point> points) {
        Path path = new Path();
        float lengthPerPixel = config.getLengthPerPixel();
        path.moveTo(points.get(0).x / lengthPerPixel, points.get(0).y / lengthPerPixel);
        for (int i = 1; i < points.size(); i++) {
            Point p = points.get(i);
            path.lineTo(p.x / lengthPerPixel, p.y / lengthPerPixel);
        }
        return path;
    }

    private GetLinePathResult getLinePath(Paint paint, String text, List<Point> points) {
        Path path = pointsToPath(points);

        PathMeasure pathMeasure = new PathMeasure(path, false);
        float pathLength = pathMeasure.getLength();
        float textWidth = paint.measureText(text);

        if (textWidth > pathLength) {
            return null;
        }

        float hOffset = (pathLength - textWidth) / 2;
        float vOffset = (-paint.ascent() + paint.descent()) / 2 - paint.descent();

        boolean isOverMaxCharAngleDelta = checkOverMaxCharAngleDelta(text, pathMeasure, paint, hOffset, maxCharAngleDelta);

        if (isOverMaxCharAngleDelta) {
            return null;
        }

        boolean isUpsideDown = checkUpsideDown(pathMeasure, pathLength);

        if (isUpsideDown) {
            List<Point> newPoints = new ArrayList<>(points);
            Collections.reverse(newPoints);
            path = pointsToPath(newPoints);
        }

        return new GetLinePathResult(path, hOffset, vOffset);
    }

    private List<Polygon> createTextPathOnPath(Path originalPath, String text, float hOffset, float vOffset, Paint paint) {
        List<Polygon> polygons = new ArrayList<>();
        PathMeasure pathMeasure = new PathMeasure(originalPath, false);
        float[] pos = new float[2];
        float[] tan = new float[2];
        float[] nextPos = new float[2];
        float[] nextTan = new float[2];
        float distance = hOffset;

        for (int i = 0; i < text.length(); i++) {
            String c = text.substring(i, i + 1);
            float charWidth = paint.measureText(c);
            Path charPath = new Path();
            paint.getTextPath(c, 0, 1, 0, 0, charPath);

            pathMeasure.getPosTan(distance, pos, tan);
            pathMeasure.getPosTan(distance + charWidth, nextPos, nextTan);

            float angle = (float) (Math.atan2(tan[1], tan[0]) * 180 / Math.PI);
            Matrix matrix = new Matrix();
            matrix.setScale(1, -1);
            matrix.postRotate(angle);
            matrix.postTranslate(pos[0], pos[1]);
            matrix.postTranslate(tan[1] * vOffset, -tan[0] * vOffset);
            charPath.transform(matrix);

            List<Polygon> p = textPathToPolygons(charPath, NUM_POINTS_PER_TEXT_PATH);
            polygons.addAll(p);

            distance += charWidth;
        }

        return polygons;
    }

    private static List<Polygon> textPathToPolygons(Path path, float numPoints) {
        List<Polygon> polygons = new ArrayList<>();
        PathMeasure pathMeasure = new PathMeasure(path, false);

        float[] pos = new float[2];
        float[] tan = new float[2];

        Path prevPath = null;
        Path curPath;

        boolean done = false;
        while (!done) {
            List<Point> points = new ArrayList<>();
            float length = pathMeasure.getLength();
            curPath = new Path();
            pathMeasure.getSegment(0, length, curPath, true);
            float dist = length / numPoints;
            for (float i = 0; i < length; i += dist) {
                pathMeasure.getPosTan(i, pos, tan);
                points.add(new Point(pos[0], pos[1]));
            }

            if (points.size() < 3) {
                done = !pathMeasure.nextContour();
                continue;
            }

            if (points.get(points.size() - 1) != points.get(0)) {
                points.add(points.get(0));
            }

            if (prevPath != null) {
                // check if prevPath is inside curPath
                RectF prevRect = new RectF();
                prevPath.computeBounds(prevRect, true);
                RectF curRect = new RectF();
                curPath.computeBounds(curRect, true);
                if (prevRect.contains(curRect)) {
                    polygons.get(polygons.size() - 1).addHole(points);
                } else {
                    polygons.add(new Polygon(points));
                    prevPath = curPath;
                }
            } else {
                polygons.add(new Polygon(points));
                prevPath = curPath;
            }

            done = !pathMeasure.nextContour();
        }


        return polygons;
    }

    private static class GetLinePathResult {
        public Path path;
        public float hOffset;
        public float vOffset;

        public GetLinePathResult(Path path, float hOffset, float vOffset) {
            this.path = path;
            this.hOffset = hOffset;
            this.vOffset = vOffset;
        }
    }

    @Override
    public void draw(SymMeta symMeta) {
        if (!(symMeta instanceof TextSymMeta)) return;
        TextSymMeta textSymMeta = (TextSymMeta) symMeta;
        textSymMeta.draw(config.colorShaderProgram);
    }

    @NonNull
    @Override
    public String toString() {
        return "TextDrawable{" +
                "textName='" + textExprString + '\'' +
                ", size=" + size +
                ", placement='" + placement + '\'' +
                ", dx=" + dx +
                ", dy=" + dy +
                ", fillColor=" + Arrays.toString(fillColor) +
                ", verticalAlignment='" + verticalAlignment + '\'' +
                ", horizontalAlignment='" + horizontalAlignment + '\'' +
                '}';
    }

    public static boolean checkOverMaxCharAngleDelta(String text, PathMeasure pathMeasure, Paint paint, float hOffset, float maxCharAngleDelta) {
        if (maxCharAngleDelta <= 0) return false;

        float distance = hOffset;
        float prevAngle = 0;

        float[] pos = new float[2];
        float[] tan = new float[2];

        for (int i = 0; i < text.length(); i++) {
            String character = text.substring(i, i + 1);
            float currentWidth = paint.measureText(character);
            pathMeasure.getPosTan(distance + currentWidth / 2, pos, tan);
            float angle = (float) Math.atan2(tan[1], tan[0]) * 180 / (float) Math.PI;
            if (i > 0 && Math.abs(angle - prevAngle) > maxCharAngleDelta) {
                return true;
            }
            prevAngle = angle;
            distance += currentWidth;
        }

        return false;
    }

    private static boolean checkUpsideDown(PathMeasure pathMeasure, float pathLength) {
        float[] pos = new float[2];
        float[] tan = new float[2];

        pathMeasure.getPosTan(pathLength / 2, pos, tan);
        float angle = (float) Math.toDegrees(Math.atan2(tan[1], tan[0]));

        return angle > 90 || angle < -90;
    }
}
