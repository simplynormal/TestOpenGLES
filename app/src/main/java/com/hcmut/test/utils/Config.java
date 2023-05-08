package com.hcmut.test.utils;

import android.annotation.SuppressLint;
import android.content.Context;

import com.hcmut.test.algorithm.CoordinateTransform;
import com.hcmut.test.geometry.Vector;
import com.hcmut.test.osm.Node;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.programs.FrameShaderProgram;
import com.hcmut.test.programs.LineTextShaderProgram;
import com.hcmut.test.programs.PointTextShaderProgram;
import com.hcmut.test.programs.TextShaderProgram;

import org.osgeo.proj4j.ProjCoordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.lang.Runnable;

@SuppressLint("NewApi")
public class Config {
    private int width;
    private int height;
    private float lengthPerPixel;
    private float originX;
    private float originY;
    private float scaleDenominator;
    private float scale = 1;
    private float rotation = 0;
    private Vector translation = new Vector(0, 0);
    private final List<BiConsumer<Config, Set<Property>>> listeners = new ArrayList<>();
    private ColorShaderProgram colorShaderProgram;
    private TextShaderProgram textShaderProgram;
    private PointTextShaderProgram pointTextShaderProgram;
    private LineTextShaderProgram lineTextShaderProgram;
    private FrameShaderProgram frameShaderProgram;
    public final Context context;

    public enum Property {
        WIDTH,
        HEIGHT,
        PIXEL_PER_LENGTH,
        ORIGIN_X,
        ORIGIN_Y,
        SCALE_DENOMINATOR,
        SCALE,
        ROTATION,
        TRANSLATION
    }

    public Config(Context context) {
        this.context = context;
        this.width = 0;
        this.height = 0;
        this.lengthPerPixel = 0;
        this.scaleDenominator = 0;
    }

    public Runnable addListener(BiConsumer<Config, Set<Property>> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void notifyListeners(Set<Property> changedProperties) {
        if (listeners.isEmpty()) {
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            for (BiConsumer<Config, Set<Property>> listener : listeners) {
                listener.accept(this, changedProperties);
            }
        });
    }

    public void setColorShaderProgram(ColorShaderProgram colorShaderProgram) {
        this.colorShaderProgram = colorShaderProgram;
    }

    public ColorShaderProgram getColorShaderProgram() {
        return colorShaderProgram;
    }

    public void setTextShaderProgram(TextShaderProgram textShaderProgram) {
        this.textShaderProgram = textShaderProgram;
    }

    public void setPointTextShaderProgram(PointTextShaderProgram pointTextShaderProgram) {
        this.pointTextShaderProgram = pointTextShaderProgram;
    }

    public PointTextShaderProgram getPointTextShaderProgram() {
        return pointTextShaderProgram;
    }

    public TextShaderProgram getTextShaderProgram() {
        return textShaderProgram;
    }

    public void setFrameShaderProgram(FrameShaderProgram frameShaderProgram) {
        this.frameShaderProgram = frameShaderProgram;
    }

    public void setLineTextShaderProgram(LineTextShaderProgram lineTextShaderProgram) {
        this.lineTextShaderProgram = lineTextShaderProgram;
    }

    public LineTextShaderProgram getLineTextShaderProgram() {
        return lineTextShaderProgram;
    }

    public FrameShaderProgram getFrameShaderProgram() {
        return frameShaderProgram;
    }

    public void setWidthHeight(int width, int height) {
        this.width = width;
        this.height = height;
        if (width > 0 && height > 0) {
            if (width > height) {
                this.lengthPerPixel = 4f / height / 0.95f;
            } else {
                this.lengthPerPixel = 4f / width / 0.95f;
            }
        }
        notifyListeners(Set.of(Property.WIDTH, Property.HEIGHT, Property.PIXEL_PER_LENGTH));
    }

    public void setOrigin(float originX, float originY) {
        this.originX = originX;
        this.originY = originY;
        notifyListeners(Set.of(Property.ORIGIN_X, Property.ORIGIN_Y));
    }

    public void setOriginFromWGS84(float originX, float originY) {
        ProjCoordinate origin = CoordinateTransform.wgs84ToWebMercator(originY, originX);
        Node transformedOrigin = new Node((float) origin.x, (float) origin.y, true);
        this.originX = transformedOrigin.lon;
        this.originY = transformedOrigin.lat;
        notifyListeners(Set.of(Property.ORIGIN_X, Property.ORIGIN_Y));
    }

    public void setScaleDenominator(float scaleDenominator) {
        this.scaleDenominator = scaleDenominator;
        notifyListeners(Set.of(Property.SCALE_DENOMINATOR));
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getLengthPerPixel() {
        return lengthPerPixel;
    }

    public float getScaleDenominator() {
        return scaleDenominator;
    }

    public float getOriginX() {
        return originX;
    }

    public float getOriginY() {
        return originY;
    }

    public float getScale() {
        return scale;
    }

    public float getRotation() {
        return rotation;
    }

    public Vector getTranslation() {
        return translation;
    }

    public void setScale(float scale) {
        this.scale = scale;
        notifyListeners(Set.of(Property.SCALE));
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
        notifyListeners(Set.of(Property.ROTATION));
    }

    public void setTranslation(Vector translation) {
        this.translation = translation;
        notifyListeners(Set.of(Property.TRANSLATION));
    }
}
