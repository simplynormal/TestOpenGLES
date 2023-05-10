package com.hcmut.test.mapnik.symbolizer;

import androidx.annotation.NonNull;

import com.hcmut.test.algorithm.CoordinateTransform;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.osm.Way;
import com.hcmut.test.utils.Config;

import java.util.HashMap;

public abstract class Symbolizer {
    protected Config config;
    abstract public SymMeta toDrawable(Way way, String layerName);
    @NonNull
    public abstract String toString();

    protected Symbolizer(Config config) {
        this.config = config;
    }

    protected static float[] parseColorString(String colorString, float optionalOpacity) {
        if (colorString == null) return new float[]{0, 0, 0, optionalOpacity};

        if (colorString.startsWith("rgba")) {
            return parseColorRGBA(colorString);
        } else if (colorString.startsWith("rgb")) {
            return parseColorRGB(colorString, optionalOpacity);
        } else if (colorString.startsWith("#") && colorString.length() == 7) {
            return parseColorHex(colorString, optionalOpacity);
        } else if (colorString.startsWith("#") && colorString.length() == 9) {
            return parseColorHex(colorString);
        } else {
            throw new IllegalArgumentException("Invalid color string: " + colorString);
        }
    }

    protected static float[] parseColorRGBA(String colorString) {
        // color string format: rgba(r, g, b, a)
        String[] colorStringArray = colorString.substring(5, colorString.length() - 1).split(",");
        float[] colorVector = new float[4];
        colorVector[0] = Integer.parseInt(colorStringArray[0].trim()) / 255f;
        colorVector[1] = Integer.parseInt(colorStringArray[1].trim()) / 255f;
        colorVector[2] = Integer.parseInt(colorStringArray[2].trim()) / 255f;
        colorVector[3] = Float.parseFloat(colorStringArray[3].trim());
        return colorVector;
    }

    protected static float[] parseColorRGB(String colorString, float opacity) {
        // color string format: rgb(r, g, b)
        String[] colorStringArray = colorString.substring(4, colorString.length() - 1).split(",");
        float[] colorVector = new float[4];
        colorVector[0] = Integer.parseInt(colorStringArray[0].trim()) / 255f;
        colorVector[1] = Integer.parseInt(colorStringArray[1].trim()) / 255f;
        colorVector[2] = Integer.parseInt(colorStringArray[2].trim()) / 255f;
        colorVector[3] = opacity;
        return colorVector;
    }

    protected static float[] parseColorHex(String colorString, float opacity) {
        float[] colorVector = new float[4];
        colorVector[0] = Integer.parseInt(colorString.substring(1, 3), 16) / 255f;
        colorVector[1] = Integer.parseInt(colorString.substring(3, 5), 16) / 255f;
        colorVector[2] = Integer.parseInt(colorString.substring(5, 7), 16) / 255f;
        colorVector[3] = opacity;
        return colorVector;
    }

    protected static float[] parseColorHex(String colorString) {
        float[] colorVector = new float[4];
        colorVector[0] = Integer.parseInt(colorString.substring(1, 3), 16) / 255f;
        colorVector[1] = Integer.parseInt(colorString.substring(3, 5), 16) / 255f;
        colorVector[2] = Integer.parseInt(colorString.substring(5, 7), 16) / 255f;
        colorVector[3] = Integer.parseInt(colorString.substring(7, 9), 16) / 255f;
        return colorVector;
    }
}
