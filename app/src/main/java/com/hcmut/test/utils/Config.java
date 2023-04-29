package com.hcmut.test.utils;

import android.annotation.SuppressLint;
import android.content.Context;

import com.hcmut.test.algorithm.CoordinateTransform;
import com.hcmut.test.osm.Node;
import com.hcmut.test.programs.ColorShaderProgram;
import com.hcmut.test.programs.TextShaderProgram;

import org.osgeo.proj4j.ProjCoordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    private final List<BiConsumer<Config, Set<Property>>> listeners = new ArrayList<>();
    public final ColorShaderProgram colorShaderProgram;
    public final TextShaderProgram textShaderProgram;
    public final Context context;

    public enum Property {
        WIDTH,
        HEIGHT,
        PIXEL_PER_LENGTH,
        ORIGIN_X,
        ORIGIN_Y,
        SCALE_DENOMINATOR
    }

    public Config(Context context, ColorShaderProgram colorShaderProgram, TextShaderProgram textShaderProgram) {
        this.context = context;
        this.colorShaderProgram = colorShaderProgram;
        this.textShaderProgram = textShaderProgram;
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
        for (BiConsumer<Config, Set<Property>> listener : listeners) {
            listener.accept(this, changedProperties);
        }
    }

    public void setWidthHeight(int width, int height) {
        this.width = width;
        this.height = height;
        if (width > 0 && height > 0) {
            if (width > height) {
                this.lengthPerPixel = 2f / height;
            } else {
                this.lengthPerPixel = 2f / width;
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
}
