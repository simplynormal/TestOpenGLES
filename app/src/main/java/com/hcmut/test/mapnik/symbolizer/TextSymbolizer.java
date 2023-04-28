package com.hcmut.test.mapnik.symbolizer;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.osm.Way;
import com.hcmut.test.programs.ShaderProgram;
import com.hcmut.test.programs.TextShaderProgram;
import com.hcmut.test.utils.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private final HashMap<float[], Integer> textTextureIds = new HashMap<>();
    private static Typeface tf = null;
    private static boolean IS_SAVE = false;

    public TextSymbolizer(Config config, @Nullable String textExpr, @Nullable String dx, @Nullable String dy, @Nullable String spacing, @Nullable String repeatDistance, @Nullable String maxCharAngleDelta, @Nullable String fill, @Nullable String opacity, @Nullable String placement, @Nullable String verticalAlignment, @Nullable String horizontalAlignment, @Nullable String justifyAlignment, @Nullable String wrapWidth, @Nullable String size, @Nullable String haloFill, @Nullable String haloRadius) {
        super(config);
        if (tf == null) {
            tf = Typeface.createFromAsset(config.context.getAssets(), "NotoSans-Regular.ttf");
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
                if (placement.equals("line")) {
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
    public float[] toDrawable(Way way, PointList shape) {
        String name = textExprEvaluator.apply(way.tags);
        if (name == null) {
            return new float[0];
        }

        List<Point> points = shape.points;

        float maxX = 0;
        float maxY = 0;
        float minX = 99999;
        float minY = 99999;

        for (Point p : points) {
            if (p.x > maxX) maxX = p.x;
            if (p.y > maxY) maxY = p.y;
            if (p.x < minX) minX = p.x;
            if (p.y < minY) minY = p.y;
        }

        float fontHeight = size;
        float lengthX = maxX - minX;
        float lengthY = maxY - minY;
        float lengthPerPixel = config.getLengthPerPixel();
        int textureWidth = Math.round(lengthX / lengthPerPixel + 2 * fontHeight);
        int textureHeight = Math.round(lengthY / lengthPerPixel + 2 * fontHeight);

        minX -= ((float) textureWidth / (textureWidth - 2 * fontHeight) - 1) * lengthX / 2;
        maxX += ((float) textureWidth / (textureWidth - 2 * fontHeight) - 1) * lengthX / 2;
        minY -= ((float) textureHeight / (textureHeight - 2 * fontHeight) - 1) * lengthY / 2;
        maxY += ((float) textureHeight / (textureHeight - 2 * fontHeight) - 1) * lengthY / 2;

        Paint paint = new Paint();
        paint.setTypeface(tf);
        paint.setTextSize(size);
        paint.setAntiAlias(true);
        paint.setSubpixelText(false); // Disable sub-pixel text rendering
        paint.setDither(true); // Enable dithering for better color precision

        Bitmap bitmap = null;
        switch (placement) {
            case "line":
                bitmap = drawLinePlacement(textureWidth, textureHeight, name, points);
                break;
//            case "interior":
//                bitmap = drawInteriorPlacement(textureWidth, textureHeight, name, points);
//                break;
//            default:
//                bitmap = drawPointPlacement(textureWidth, textureHeight, name, points);
//                break;
        }

        if (bitmap == null) {
            return new float[0];
        }

        TextShaderProgram textShaderProgram = config.textShaderProgram;
        int textId = textShaderProgram.loadTexture(bitmap);

        float[] rv = new float[]{
                minX, minY, 0, 0, 1,
                minX, maxY, 0, 0, 0,
                maxX, minY, 0, 1, 1,
                maxX, maxY, 0, 1, 0,
        };

        textTextureIds.put(rv, textId);

        return rv;
    }

    private Bitmap drawLinePlacement(int textureWidth, int textureHeight, String name, List<Point> points) {
        Paint paint = new Paint();
        paint.setTypeface(tf);
        paint.setTextSize(size);
        paint.setAntiAlias(true);
        paint.setSubpixelText(false); // Disable sub-pixel text rendering
        paint.setDither(true); // Enable dithering for better color precision

        Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
//        canvas.drawColor(Color.BLUE);

        GetLinePathResult result = getLinePath(paint, name, points, textureWidth, textureHeight);
        if (result == null) {
            return null;
        }

        Path path = result.path;
        float hOffset = result.hOffset;
        float vOffset = result.vOffset;


//        if (haloRadius > 0) {
//            paint.setColor(Color.argb(haloFill[3], haloFill[0], haloFill[1], haloFill[2]));
//            paint.setStyle(Paint.Style.FILL);
//            paint.setStrokeWidth(2);
//            canvas.drawTextOnPath(name, path, hOffset, vOffset, paint);
//        }

        paint.setColor(Color.argb(fillColor[3], fillColor[0], fillColor[1], fillColor[2]));
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(0);
        canvas.drawTextOnPath(name, path, hOffset, vOffset, paint);

        paint.setColor(Color.RED);
        canvas.drawTextOnPath(name, path, hOffset, 0, paint);

        return bitmap;
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

    private GetLinePathResult getLinePath(Paint paint, String text, List<Point> points, int textureWidth, int textureHeight) {
        Path path = pointsToPath(points, textureWidth, textureHeight, size);

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
            path = pointsToPath(newPoints, textureWidth, textureHeight, size);
        }

        return new GetLinePathResult(path, hOffset, vOffset);
    }

    @Override
    public void draw(VertexArray vertexArray, float[] rawDrawable) {
        Integer textId = textTextureIds.get(rawDrawable);
        if (vertexArray == null || rawDrawable.length == 0 || textId == null) return;

        TextShaderProgram textShaderProgram = config.textShaderProgram;
        textShaderProgram.useProgram();

        textShaderProgram.setCurrentTexture(textId);
        vertexArray.setDataFromVertexData();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 4);

    }

    @Override
    public boolean isAppendable() {
        return false;
    }

    @Override
    public float[] appendDrawable(float[] oldDrawable, float[] newDrawable) {
        return new float[0];
    }

    @Override
    public void draw(VertexArray vertexArray) {
    }

    @Override
    public ShaderProgram getShaderProgram() {
        return config.textShaderProgram;
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

    private static Path pointsToPath(List<Point> points, float textureWidth, float textureHeight, float fontHeight) {
        float maxX = 0;
        float maxY = 0;
        float minX = 99999;
        float minY = 99999;

        for (Point p : points) {
            if (p.x > maxX) maxX = p.x;
            if (p.y > maxY) maxY = p.y;
            if (p.x < minX) minX = p.x;
            if (p.y < minY) minY = p.y;
        }

        float lengthX = maxX - minX;
        float lengthY = maxY - minY;

        Path path = new Path();
        float scaledX = (points.get(0).x - minX) / lengthX;
        float scaledY = 1 - (points.get(0).y - minY) / lengthY;
        path.moveTo(scaledX, scaledY);
        for (int i = 1; i < points.size(); i++) {
            scaledX = (points.get(i).x - minX) / lengthX;
            scaledY = 1 - (points.get(i).y - minY) / lengthY;
            path.lineTo(scaledX, scaledY);
        }

        float scaleFactorWidth = (textureWidth - fontHeight * 2);
        float scaleFactorHeight = (textureHeight - fontHeight * 2);
//        scaleFactorHeight = textureHeight;
//        scaleFactorWidth = textureWidth;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleFactorWidth, scaleFactorHeight);
        matrix.postTranslate(fontHeight, fontHeight);
        path.transform(matrix);

        return path;
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
