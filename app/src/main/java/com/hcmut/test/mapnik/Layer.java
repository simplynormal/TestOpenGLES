package com.hcmut.test.mapnik;

import android.annotation.SuppressLint;

import com.hcmut.test.mapnik.symbolizer.CombinedSymMeta;
import com.hcmut.test.osm.Way;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@SuppressLint("NewApi")
public class Layer {
    public final String name;
    private final Config config;
    private final List<String> stylesNames = new ArrayList<>();
    private final List<Style> styles = new ArrayList<>();
    private final HashMap<Long, Way> waysMap = new HashMap<>();
    private final TreeMap<Long, List<CombinedSymMeta>> symMetasMap = new TreeMap<>();
    private TreeMap<Long, List<CombinedSymMeta>> drawingSymMetasMap = null;

    public Layer(Config config, String name) {
        this.name = name;
        this.config = config;
    }

    public void addStyleName(String name) {
        stylesNames.add(name);
    }

    public void validateStyles(HashMap<String, Style> stylesMap) {
        for (String name : stylesNames) {
            Style style = stylesMap.get(name);
            assert style != null : "Style " + name + " not found";
            style.setLayerName(this.name);
            styles.add(style);
        }
    }

    public void addWay(Way way) {
        if (symMetasMap.containsKey(way.id)) return;
        List<CombinedSymMeta> symMetaWithWays = new ArrayList<>(styles.size());
        for (int i = 0; i < styles.size(); i++) {
            Style style = styles.get(i);
            CombinedSymMeta combinedSymMeta = style.drawWay(way);
            symMetaWithWays.add(combinedSymMeta);
        }
        symMetasMap.put(way.id, symMetaWithWays);
        waysMap.put(way.id, way);
    }

    public void save() {
        drawingSymMetasMap = new TreeMap<>(symMetasMap);
    }

    public void draw() {
        if (drawingSymMetasMap == null) return;
        for (long key : drawingSymMetasMap.keySet()) {
            Way way = waysMap.get(key);
            List<CombinedSymMeta> symMetas = drawingSymMetasMap.get(key);
            if (way == null || symMetas == null) continue;
            if (!way.getBoundBox().withinOrIntersects(config.getWorldBoundBox())) continue;

            for (CombinedSymMeta symMeta : symMetas) {
                symMeta.draw(config);
            }
        }
    }
}
