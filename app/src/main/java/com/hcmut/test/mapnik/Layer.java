package com.hcmut.test.mapnik;

import com.hcmut.test.osm.Way;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Layer {
    private String query;
    private final List<String> stylesNames = new ArrayList<>();
    private final List<Style> styles = new ArrayList<>();
    private HashMap<String, Way> ways;

    public void setQuery(String query) {
        this.query = query;
    }

    public void addStyleName(String name) {
        stylesNames.add(name);
    }

    public void validateStyles(List<Style> styles) {
        for (String name : stylesNames) {
            for (Style style : styles) {
                if (style.name.equals(name)) {
                    this.styles.add(style);
                    break;
                }
            }
        }
    }

    public boolean validateWay(Way way) {
        if (way == null) return false;
        for (String tagKey : way.tags.keySet()) {
            String tagValue = way.tags.get(tagKey);
            if (query.contains(tagKey) || (tagValue != null && query.contains(tagValue))) {
                return true;
            }
        }

        return false;
    }

//    public boolean validateWay(Way way) {
//        if (way == null) return false;
//        for (String tagKey : way.tags.keySet()) {
//            String tagValue = way.tags.get(tagKey);
//            if (!(query.contains(tagKey) || (tagValue != null && query.contains(tagValue)))) {
//                return true;
//            }
//        }
//
//        return false;
//    }

    public void validateWays(HashMap<String, Way> ways) {
        this.ways = ways;
        for (Style style : styles) {
            for (String key : ways.keySet()) {
                Way way = ways.get(key);
                if (!validateWay(way)) continue;
                style.validateWay(key, way);
            }
            style.wrapUpWays();
        }
    }

    public void draw() {
        for (Style style : styles) {
            style.draw();
        }
    }
}
