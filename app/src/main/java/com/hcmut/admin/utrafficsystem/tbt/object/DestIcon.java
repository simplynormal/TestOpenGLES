package com.hcmut.admin.utrafficsystem.tbt.object;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Location;
import android.opengl.GLES20;

import androidx.core.content.res.ResourcesCompat;

import com.hcmut.admin.utrafficsystem.tbt.algorithm.CoordinateTransform;
import com.hcmut.admin.utrafficsystem.tbt.data.VertexArray;
import com.hcmut.admin.utrafficsystem.tbt.geometry.Point;
import com.hcmut.admin.utrafficsystem.tbt.programs.TextureShaderProgram;
import com.hcmut.admin.utrafficsystem.tbt.utils.Config;
import com.hcmut.test.R;

import org.osgeo.proj4j.ProjCoordinate;

import java.util.ArrayList;
import java.util.List;

public class DestIcon {
    private static final List<Point> QUAD_COORDS = new ArrayList<>(4) {{
        add(new Point(-1, -1));
        add(new Point(-1, 1));
        add(new Point(1, -1));
        add(new Point(1, 1));
    }};
    private static final List<Point> QUAD_TEX_COORDS = new ArrayList<>(4) {{
        add(new Point(0, 1));
        add(new Point(0, 0));
        add(new Point(1, 1));
        add(new Point(1, 0));
    }};
    private static final float RADIUS = 0.08f;
    private final Config config;
    private final Location destination;
    private int textureId;
    VertexArray vertexArray = null;

    public DestIcon(Config config, Location destination) {
        this.config = config;
        this.destination = destination;
    }

    private void setup() {
        float scaled = CoordinateTransform.getScalePixel(config.getScaleDenominator()) * config.getLengthPerPixel();
        ProjCoordinate p = CoordinateTransform.wgs84ToWebMercator(destination.getLatitude(), destination.getLongitude());
        Point loc = new Point((float) p.x, (float) p.y).transform(config.getOriginX(), config.getOriginY(), scaled);
        loc = new Point(0, 0);

        List<Point> points = new ArrayList<>(QUAD_COORDS.size());
        for (Point point : QUAD_COORDS) {
            points.add(point.scale(RADIUS).add(loc));
        }

        TextureShaderProgram textureShaderProgram = config.getTextureShaderProgram();
        VectorDrawable resDrawable = (VectorDrawable) ResourcesCompat.getDrawable(config.context.getResources(), R.drawable.ic_location_reb, null);
        assert resDrawable != null;
        Bitmap bitmap = Bitmap.createBitmap(resDrawable.getIntrinsicWidth(),
                resDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        resDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        resDrawable.draw(canvas);

        textureId = textureShaderProgram.loadTexture(bitmap);

        float[] drawable = TextureShaderProgram.toVertexData(points, QUAD_TEX_COORDS);
        vertexArray = new VertexArray(textureShaderProgram, drawable);
    }

    public void draw() {
        if (vertexArray == null) {
            setup();
        }
        vertexArray.setDataFromVertexData();
        TextureShaderProgram textureShaderProgram = config.getTextureShaderProgram();
        textureShaderProgram.setCurrentTexture(textureId);
        int pointCount = vertexArray.getVertexCount();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, pointCount);
    }
}
