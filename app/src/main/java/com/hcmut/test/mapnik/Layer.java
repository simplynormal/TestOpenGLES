package com.hcmut.test.mapnik;

import android.annotation.SuppressLint;
import android.util.Log;

import com.hcmut.test.mapnik.symbolizer.CombinedSymMeta;
import com.hcmut.test.osm.Way;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
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
    private final Map<Long, CombinedSymMeta> symMetaMap = Collections.synchronizedMap(new HashMap<>());
    private Map<Long, CombinedSymMeta> drawingSymMetasMap = null;

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

//    private void addWay(Way way, long tileId) {
//        if (addedWays.containsKey(way.id)) {
//            SymMetasWithWay currentSymMetasWithWay = addedWays.get(way.id);
//            if (currentSymMetasWithWay != null) {
//                currentSymMetasWithWay.tileIds.add(tileId);
//            }
//            return;
//        }
//
//        List<CombinedSymMeta> symMetas = new ArrayList<>(styles.size());
//        for (int i = 0; i < styles.size(); i++) {
//            Style style = styles.get(i);
//            CombinedSymMeta combinedSymMeta = style.toDrawable(way);
//            combinedSymMeta.save(config);
//            symMetas.add(combinedSymMeta);
//        }
//
//        SymMetasWithWay symMetasWithWay = null;
//        if (!symMetas.isEmpty()) {
//            int order = Integer.parseInt(Objects.requireNonNull(Objects.requireNonNull(way.tags.get(name)).get("postgisOrder")));
//            symMetasWithWay = new SymMetasWithWay(order, symMetas, way);
//            symMetasWithWay.tileIds.add(tileId);
//            symMetasMap.put(order, symMetasWithWay);
//        }
//        addedWays.put(way.id, symMetasWithWay);
//    }

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
        CombinedSymMeta combinedSymMeta = new CombinedSymMeta();
        HashMap<Long, CombinedSymMeta> waysSymMetasMap = new HashMap<>(ways.size());
        TreeMap<Integer, Long> waysOrder = new TreeMap<>();
        for (Style style : styles) {
            for (Way way : ways) {
                CombinedSymMeta curCombinedSymMeta = style.toDrawable(way);
                waysSymMetasMap.put(way.id, curCombinedSymMeta);
                int order = Integer.parseInt(Objects.requireNonNull(Objects.requireNonNull(way.tags.get(name)).get("postgisOrder")));
                waysOrder.put(order, way.id);
            }

            for (long wayId : waysOrder.values()) {
                combinedSymMeta = (CombinedSymMeta) combinedSymMeta.append(waysSymMetasMap.get(wayId));
            }
        }
        combinedSymMeta.save(config);
        symMetaMap.put(tileId, combinedSymMeta);
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
        if (tileIds.isEmpty() || symMetaMap.isEmpty()) {
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
        
        for (Long tileId : tileIds) {
            symMetaMap.remove(tileId);
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
    
//    public void removeWays(Set<Long> tileIds) {
//        if (tileIds.isEmpty() || symMetasMap.isEmpty()) {
//            return;
//        }
//
//        saveLock.lock();
//        try {
//            if (isSaving) {
//                while (isSaving) {
//                    saveFinished.await();
//                }
//            }
//        } catch (InterruptedException e) {
//            Log.e("Layer", "removeWays: ", e);
//        } finally {
//            saveLock.unlock();
//        }
//
//        modifyCounter.incrementAndGet();
//        List<SymMetasWithWay> removed = new ArrayList<>(addedWays.size());
//        for (SymMetasWithWay symMetasWithWay : addedWays.values()) {
//            if (symMetasWithWay != null) {
//                symMetasWithWay.tileIds.removeAll(tileIds);
//                if (symMetasWithWay.tileIds.isEmpty()) {
//                    removed.add(symMetasWithWay);
//                }
//            }
//        }
//
//        for (SymMetasWithWay symMetasWithWay : removed) {
//            if (symMetasWithWay != null) {
//                symMetasMap.remove(symMetasWithWay.order);
//                addedWays.remove(symMetasWithWay.way.id);
//            }
//        }
//
//        if (modifyCounter.decrementAndGet() == 0) {
//            saveLock.lock();
//            try {
//                allModified.signalAll();
//            } finally {
//                saveLock.unlock();
//            }
//        }
//    }

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
            drawingSymMetasMap = Collections.synchronizedMap(new HashMap<>(symMetaMap));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            isSaving = false;
            saveFinished.signalAll();
            saveLock.unlock();
        }
    }

//    public void draw() {
//        if (drawingSymMetasMap == null) return;
//        Map<Integer, SymMetasWithWay> drawingSymMetasMap = this.drawingSymMetasMap;
//        for (int i = 0; i < styles.size(); i++) {
//            for (int key : drawingSymMetasMap.keySet()) {
//                SymMetasWithWay symMetasWithWay = drawingSymMetasMap.get(key);
//                if (symMetasWithWay == null) continue;
//                List<CombinedSymMeta> symMetas = symMetasWithWay.symMetas;
//                Way way = symMetasWithWay.way;
//                if (way == null || symMetas == null) continue;
//                if (!way.getBoundBox().withinOrIntersects(config.getWorldBoundBox())) continue;
//                symMetas.get(i).draw();
//            }
//        }
//    }

    public void draw() {
        if (drawingSymMetasMap == null) return;
        Map<Long, CombinedSymMeta> drawingSymMetasMap = this.drawingSymMetasMap;

        for (CombinedSymMeta combinedSymMeta : drawingSymMetasMap.values()) {
            combinedSymMeta.draw();
        }
    }

    public boolean hasTextSymbolizer() {
        return hasText;
    }
}
