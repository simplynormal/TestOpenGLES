package com.hcmut.test.mapnik.symbolizer;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Typeface;
import android.opengl.GLES20;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.osm.Way;
import com.hcmut.test.programs.TextShaderProgram;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
    private final String textName;
    private final List<String> textNameVars = new ArrayList<>();
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

    public TextSymbolizer(Config config, @Nullable String textName, @Nullable String dx, @Nullable String dy, @Nullable String spacing, @Nullable String repeatDistance, @Nullable String maxCharAngleDelta, @Nullable String fill, @Nullable String opacity, @Nullable String placement, @Nullable String verticalAlignment, @Nullable String horizontalAlignment, @Nullable String justifyAlignment, @Nullable String wrapWidth, @Nullable String size, @Nullable String haloFill, @Nullable String haloRadius) {
        super(config);
        if (tf == null) {
            tf = Typeface.createFromAsset(config.context.getAssets(), "NotoSans-Regular.ttf");
        }
        this.textName = parseTextNameVars(textName);
        this.dx = dx == null ? 0 : Float.parseFloat(dx);
        this.dy = dy == null ? 0 : Float.parseFloat(dy);
        this.spacing = spacing == null ? 0 : Float.parseFloat(spacing);
        this.repeatDistance = repeatDistance == null ? 0 : Float.parseFloat(repeatDistance);
        this.maxCharAngleDelta = maxCharAngleDelta == null ? 0 : Float.parseFloat(maxCharAngleDelta);
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

    public String parseTextNameVars(String template) {
        Pattern pattern = Pattern.compile("\\[([a-zA-Z\\d]+)]");
        Matcher matcher = pattern.matcher(template);
        while (matcher.find()) {
            String variable = matcher.group(1);
            textNameVars.add(variable);
        }

        return template;
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
        String name = textName;
        for (String var : textNameVars) {
            String value = way.tags.get(var);
            if (value == null) {
                return new float[0];
            }
            name = name.replace("[" + var + "]", value);
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

        Paint paint = new Paint();
        paint.setTypeface(tf);
        paint.setTextSize(size);
        paint.setAntiAlias(true);

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

        Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        paint.setColor(Color.argb(fillColor[3], fillColor[0], fillColor[1], fillColor[2]));
        paint.setStyle(Paint.Style.FILL);
        boolean isDrawn = drawText(canvas, paint, name, textureWidth, textureHeight, fontHeight, points);

        if (!isDrawn) {
            return new float[0];
        }

        TextShaderProgram textShaderProgram = config.textShaderProgram;
        textShaderProgram.useProgram();
        int textId = textShaderProgram.loadTexture(bitmap);

        float[] rv = new float[]{
                minX, minY, 0.01f, 0, 1,
                minX, maxY, 0.01f, 0, 0,
                maxX, minY, 0.01f, 1, 1,
                maxX, maxY, 0.01f, 1, 0,
        };

        textTextureIds.put(rv, textId);

        return rv;
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

    @NonNull
    @Override
    public String toString() {
        return "TextDrawable{" +
                "textName='" + textName + '\'' +
                ", textNameVars=" + textNameVars +
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

    private static boolean drawText(Canvas canvas, Paint paint, String text, int textureWidth, int textureHeight, float fontHeight, List<Point> points) {
        Path path = pointsToPath(points, textureWidth, textureHeight, fontHeight);

        PathMeasure pathMeasure = new PathMeasure(path, false);
        float pathLength = pathMeasure.getLength();
        float textWidth = paint.measureText(text);

        if (textWidth > pathLength) {
            return false;
        }

        float hOffset = (pathLength - textWidth) / 2;
        float vOffset = (-paint.ascent() + paint.descent()) / 2 - paint.descent();

        // check if the path is upside down
        float[] pos = new float[2];
        float[] tan = new float[2];
        float angle;

        float threshold = 60; // adjust this value as needed
        float prevAngle = 0;
        boolean hasSharpTurn = false;
        for (float distance = 0; distance < pathLength; distance += 10) {
            pathMeasure.getPosTan(distance, pos, tan);
            angle = (float) Math.toDegrees(Math.atan2(tan[1], tan[0]));
            if (distance > 0 && Math.abs(angle - prevAngle) > threshold) {
                hasSharpTurn = true;
                break;
            }
            prevAngle = angle;
        }

        if (hasSharpTurn) {
            return false;
        }

        pathMeasure.getPosTan(pathLength / 2, pos, tan);
        angle = (float) Math.toDegrees(Math.atan2(tan[1], tan[0]));
        boolean isUpsideDown = angle > 90 || angle < -90;

        if (isUpsideDown) {
            List<Point> newPoints = new ArrayList<>(points);
            Collections.reverse(newPoints);
            path = pointsToPath(newPoints, textureWidth, textureHeight, fontHeight);
        }

        canvas.drawTextOnPath(text, path, hOffset, vOffset, paint);

//        canvas.drawPath(path, paint);
        return true;
    }
}
