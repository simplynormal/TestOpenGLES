package com.hcmut.test.mapnik;

import com.hcmut.test.osm.Way;

import java.util.ArrayList;
import java.util.List;

public class Style {
    public final String name;
    public final List<Rule> rules = new ArrayList<>();

    public Style(String name) {
        this.name = name;
    }

    public void validateWay(String key, Way way) {
        for (Rule rule : rules) {
            rule.validateElement(key, way);
        }
    }

    public void wrapUpWays() {
        for (Rule rule : rules) {
            rule.save();
        }
    }

    public void draw() {
        for (Rule rule : rules) {
            rule.draw();
        }
    }
}
