package com.hcmut.test.mapnik;

import com.hcmut.test.mapnik.symbolizer.CombinedSymMeta;
import com.hcmut.test.mapnik.symbolizer.SymMeta;
import com.hcmut.test.osm.Way;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.List;

public class Style {
    public final String name;
    private String layerName;
    private final List<Rule> rules = new ArrayList<>();
    private boolean hasText = false;

    public Style(String name) {
        this.name = name;
    }

//    public void validateWay(long key, Way way) {
//        for (Rule rule : rules) {
//            rule.validateWay(key, way);
//        }
//    }

    public CombinedSymMeta drawWay(Way way) {
        CombinedSymMeta combinedSymMeta = new CombinedSymMeta();
        for (Rule rule : rules) {
            combinedSymMeta = (CombinedSymMeta) combinedSymMeta.append(rule.toDrawable(way, layerName));
        }
        return combinedSymMeta;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public void addRule(Rule rule) {
        rules.add(rule);
        if (rule.hasTextSymbolizer()) {
            hasText = true;
        }
    }

//    public void save() {
//        for (Rule rule : rules) {
//            rule.save();
//        }
//    }

    public void save() {
    }

//    public void draw() {
//        for (Rule rule : rules) {
//            rule.draw();
//        }
//    }

//    public void draw() {
//        if (orderedSymMetaWithWays.isEmpty()) return;
//        for (SymMetaWithWay symMetaWithWay : orderedSymMetaWithWays) {
//            symMetaWithWay.symMeta.draw(config);
//        }
//    }

    public boolean hasTextSymbolizer() {
        return hasText;
    }
}
