package com.hcmut.test.mapnik;

import android.annotation.SuppressLint;
import android.util.Log;

import com.hcmut.test.mapnik.symbolizer.CombinedSymMeta;
import com.hcmut.test.osm.Way;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@SuppressLint("NewApi")
public class Layer {
    public final String name;
    private final Config config;
    private final List<String> stylesNames = new ArrayList<>();
    private final List<Style> styles = new ArrayList<>();
    private boolean hasText = false;
    private final ReentrantLock saveLock = new ReentrantLock();
    private final Condition allModified = saveLock.newCondition();
    private boolean isSaving = false;
    private final Condition saveFinished = saveLock.newCondition();

    private final AtomicInteger modifyCounter = new AtomicInteger(0);

    private static class SymMetasWithWay {
        public final int order;
        public final List<CombinedSymMeta> symMetas;
        public final Way way;
        public final Set<Long> tileIds = Collections.synchronizedSet(new HashSet<>(9));

        public SymMetasWithWay(int order, List<CombinedSymMeta> symMetas, Way way) {
            this.order = order;
            this.symMetas = symMetas;
            this.way = way;
        }
    }

    private final Map<Long, SymMetasWithWay> addedWays = new HashMap<>();
    private final Map<Integer, SymMetasWithWay> symMetasMap = Collections.synchronizedMap(new TreeMap<>());
    private Map<Integer, SymMetasWithWay> drawingSymMetasMap = null;

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
            if (style.hasTextSymbolizer()) {
                hasText = true;
            }
            styles.add(style);
        }
    }

    public void addWay(Way way, long tileId) {
        if (addedWays.containsKey(way.id)) {
            SymMetasWithWay currentSymMetasWithWay = addedWays.get(way.id);
            if (currentSymMetasWithWay != null) {
                currentSymMetasWithWay.tileIds.add(tileId);
            }
            return;
        }

        List<CombinedSymMeta> symMetas = new ArrayList<>(styles.size());
        for (int i = 0; i < styles.size(); i++) {
            Style style = styles.get(i);
            CombinedSymMeta combinedSymMeta = style.toDrawable(way);
            combinedSymMeta.save(config);
            symMetas.add(combinedSymMeta);
        }

        SymMetasWithWay symMetasWithWay = null;
        if (!symMetas.isEmpty()) {
            int order = Integer.parseInt(Objects.requireNonNull(Objects.requireNonNull(way.tags.get(name)).get("postgisOrder")));
            symMetasWithWay = new SymMetasWithWay(order, symMetas, way);
            symMetasWithWay.tileIds.add(tileId);
            symMetasMap.put(order, symMetasWithWay);
        }
        addedWays.put(way.id, symMetasWithWay);
    }

    public void addWays(List<Way> ways, long tileId) {
        if (ways.isEmpty()) {
            return;
        }

        saveLock.lock();
        try {
            if (isSaving) {
                while (isSaving) {
                    saveFinished.await();
                }
            }
        } catch (InterruptedException e) {
            Log.e("Layer", "removeWays: ", e);
        } finally {
            saveLock.unlock();
        }

        modifyCounter.incrementAndGet();
        for (Way way : ways) {
            addWay(way, tileId);
        }
        if (modifyCounter.decrementAndGet() == 0) {
            saveLock.lock();
            try {
                allModified.signalAll();
            } finally {
                saveLock.unlock();
            }
        }
    }

    public void removeWays(Set<Long> tileIds) {
        if (tileIds.isEmpty() || symMetasMap.isEmpty()) {
            return;
        }

        saveLock.lock();
        try {
            if (isSaving) {
                while (isSaving) {
                    saveFinished.await();
                }
            }
        } catch (InterruptedException e) {
            Log.e("Layer", "removeWays: ", e);
        } finally {
            saveLock.unlock();
        }

        modifyCounter.incrementAndGet();
        List<SymMetasWithWay> removed = new ArrayList<>(addedWays.size());
        for (SymMetasWithWay symMetasWithWay : addedWays.values()) {
            if (symMetasWithWay != null) {
                symMetasWithWay.tileIds.removeAll(tileIds);
                if (symMetasWithWay.tileIds.isEmpty()) {
                    removed.add(symMetasWithWay);
                }
            }
        }

        for (SymMetasWithWay symMetasWithWay : removed) {
            if (symMetasWithWay != null) {
                symMetasMap.remove(symMetasWithWay.order);
                addedWays.remove(symMetasWithWay.way.id);
            }
        }

        if (modifyCounter.decrementAndGet() == 0) {
            saveLock.lock();
            try {
                allModified.signalAll();
            } finally {
                saveLock.unlock();
            }
        }
    }

    public void save() {
        saveLock.lock();
        try {
            while (modifyCounter.get() > 0) {
                allModified.await();
            }
            isSaving = true;
//            if (drawingSymMetasMap != null && drawingSymMetasMap.size() > 0 && drawingSymMetasMap.size() != symMetasMap.size()) {
//                Log.d("Layer ", "Saved size " + drawingSymMetasMap.size() + " -> " + symMetasMap.size());
//            }
            drawingSymMetasMap = Collections.synchronizedMap(new TreeMap<>(symMetasMap));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            isSaving = false;
            saveFinished.signalAll();
            saveLock.unlock();
        }
    }

    public void draw() {
        if (drawingSymMetasMap == null) return;
        Map<Integer, SymMetasWithWay> drawingSymMetasMap = this.drawingSymMetasMap;
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

    public boolean hasTextSymbolizer() {
        return hasText;
    }
}
