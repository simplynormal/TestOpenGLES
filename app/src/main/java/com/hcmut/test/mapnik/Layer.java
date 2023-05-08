package com.hcmut.test.mapnik;

import android.annotation.SuppressLint;

import com.hcmut.test.osm.Way;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressLint("NewApi")
public class Layer {
    public final String name;
    private final List<String> stylesNames = new ArrayList<>();
    private final List<Style> styles = new ArrayList<>();

    public Layer(String name) {
        this.name = name;
    }

    public void addStyleName(String name) {
        stylesNames.add(name);
    }

    public void validateStyles(HashMap<String, Style> stylesMap) {
        for (String name : stylesNames) {
            Style style = stylesMap.get(name);
            assert style != null : "Style " + name + " not found";
            styles.add(style);
        }
    }

    public void addWay(long key, Way way) {
        for (Style style : styles) {
            style.validateWay(key, way);
        }
    }

    public void save() {
        for (Style style : styles) {
            style.save();
        }
    }

    public void draw() {
        for (Style style : styles) {
            style.draw();
        }
    }
}
