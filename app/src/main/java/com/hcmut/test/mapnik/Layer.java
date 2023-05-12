package com.hcmut.test.mapnik;

import android.annotation.SuppressLint;

import com.hcmut.test.mapnik.symbolizer.CombinedSymMeta;
import com.hcmut.test.osm.Way;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

@SuppressLint("NewApi")
public class Layer {
    public final String name;
    private final Config config;
    private final List<String> stylesNames = new ArrayList<>();
    private final List<Style> styles = new ArrayList<>();

    private static class SymMetasWithWay {
        public final List<CombinedSymMeta> symMetas;
        public final Way way;

        public SymMetasWithWay(List<CombinedSymMeta> symMetas, Way way) {
            this.symMetas = symMetas;
            this.way = way;
        }
    }

    private final Set<Long> addedWays = new HashSet<>();
    private final TreeMap<Integer, SymMetasWithWay> symMetasMap = new TreeMap<>();
    private TreeMap<Integer, SymMetasWithWay> drawingSymMetasMap = null;

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
        if (addedWays.contains(way.id)) return;
        List<CombinedSymMeta> symMetas = new ArrayList<>(styles.size());
        for (int i = 0; i < styles.size(); i++) {
            Style style = styles.get(i);
            CombinedSymMeta combinedSymMeta = style.toDrawable(way);
            combinedSymMeta.save(config);
            symMetas.add(combinedSymMeta);
        }
        addedWays.add(way.id);
        if (symMetas.isEmpty()) return;
        int order = Integer.parseInt(Objects.requireNonNull(Objects.requireNonNull(way.tags.get(name)).get("postgisOrder")));
        symMetasMap.put(order, new SymMetasWithWay(symMetas, way));
    }

    public void addWays(List<Way> ways) {
        for (Way way : ways) {
            addWay(way);
        }
    }

    public void save() {
        drawingSymMetasMap = new TreeMap<>(symMetasMap);
    }

    public void draw() {
        if (drawingSymMetasMap == null) return;
        for (int i = 0; i < styles.size(); i++) {
            for (int key : drawingSymMetasMap.keySet()) {
                SymMetasWithWay symMetasWithWay = drawingSymMetasMap.get(key);
                if (symMetasWithWay == null) continue;
                List<CombinedSymMeta> symMetas = symMetasWithWay.symMetas;
                Way way = symMetasWithWay.way;
                if (way == null || symMetas == null) continue;
                if (!way.getBoundBox().withinOrIntersects(config.getWorldBoundBox())) continue;
                symMetas.get(i).draw();
            }
        }
    }
}
