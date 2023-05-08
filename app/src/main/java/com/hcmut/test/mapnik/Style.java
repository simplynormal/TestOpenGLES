package com.hcmut.test.mapnik;

import com.hcmut.test.osm.Way;

import java.util.ArrayList;
import java.util.List;

public class Style {
    public final String name;
    public final List<Rule> rules = new ArrayList<>();
    private boolean hasText = false;

    public Style(String name) {
        this.name = name;
    }

    public void validateWay(String key, Way way) {
        for (Rule rule : rules) {
            rule.validateWay(key, way);
        }
    }

    public void validateWay(long key, Way way) {
        for (Rule rule : rules) {
            rule.validateWay(key, way);
        }
    }

    public void save() {
        for (Rule rule : rules) {
            rule.save();
            if (rule.hasTextSymbolizer()) {
                hasText = true;
            }
        }
    }

    public void draw() {
        for (Rule rule : rules) {
            rule.draw();
        }
    }

    public boolean hasTextSymbolizer() {
        return hasText;
    }
}
