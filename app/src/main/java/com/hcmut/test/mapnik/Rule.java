package com.hcmut.test.mapnik;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.hcmut.test.algorithm.CoordinateTransform;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.mapnik.symbolizer.SymMeta;
import com.hcmut.test.mapnik.symbolizer.Symbolizer;
import com.hcmut.test.mapnik.symbolizer.TextSymbolizer;
import com.hcmut.test.osm.Way;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressLint("NewApi")
public class Rule {
    private Float maxScaleDenominator;
    private Float minScaleDenominator;
    private Function<HashMap<String, String>, Boolean> filter;
    private final List<HashMap<String, SymMeta>> symMetaMapLists = new ArrayList<>();
    private final List<HashMap<Long, SymMeta>> symMetaMapLongLists = new ArrayList<>();
    private final List<Symbolizer> symbolizers = new ArrayList<>();
    private final List<SymMeta> drawingSymMetas = new ArrayList<>();
    private final Config config;
    private String filterString;

    public Rule(Config config, Float maxScaleDenominator, Float minScaleDenominator, String filter) {
        this.config = config;
        this.maxScaleDenominator = maxScaleDenominator;
        this.minScaleDenominator = minScaleDenominator;
        this.filter = createFilterFunction(filter);
        this.filterString = filter;
    }

    public Rule(Config config) {
        this(config, null, null, null);
    }

    public void setMaxScaleDenominator(Float maxScaleDenominator) {
        this.maxScaleDenominator = maxScaleDenominator;
    }

    public void setMinScaleDenominator(Float minScaleDenominator) {
        this.minScaleDenominator = minScaleDenominator;
    }

    public void setFilter(String filter) {
        this.filter = createFilterFunction(filter);
        this.filterString = filter;
    }

    public void addSymbolizer(Symbolizer symbolizer) {
        this.symbolizers.add(symbolizer);
        this.symMetaMapLists.add(new HashMap<>());
        this.symMetaMapLongLists.add(new HashMap<>());
        this.drawingSymMetas.add(null);
    }

    public boolean compareScaleDenominator(float scaleDenominator) {
        if (this.maxScaleDenominator != null && scaleDenominator > this.maxScaleDenominator) {
            return false;
        }
        if (this.minScaleDenominator != null && scaleDenominator < this.minScaleDenominator) {
            return false;
        }
        return true;
    }

    public void validateWay(String key, Way way) {
//        System.out.println("validateWay: " + key + " filter: " + filterString);

        float scaleDenominator = config.getScaleDenominator();
        if (!compareScaleDenominator(scaleDenominator)) {
            return;
        }

        HashMap<String, String> tags = new HashMap<>(way.tags);
        String wayPixelsString = tags.get("way_pixels");
        float scaledPixel = CoordinateTransform.getScalePixel(scaleDenominator);
        if (wayPixelsString != null) {
            float wayPixels = Float.parseFloat(wayPixelsString) * scaledPixel * scaledPixel;
            tags.put("way_pixels", Float.toString(wayPixels));
        }

        boolean isMatched = this.filter == null || this.filter.apply(tags);
        if (isMatched) {
            acceptWay(key, way);
        }
    }

//    public void validateWay(long key, Way way) {
//        float scaleDenominator = config.getScaleDenominator();
//        if (!compareScaleDenominator(scaleDenominator)) {
//            return;
//        }
//
//        HashMap<String, String> tags = new HashMap<>(way.tags);
//        String wayPixelsString = tags.get("way_pixels");
//        float scaledPixel = CoordinateTransform.getScalePixel(scaleDenominator);
//        if (wayPixelsString != null) {
//            float wayPixels = Float.parseFloat(wayPixelsString) * scaledPixel * scaledPixel;
//            tags.put("way_pixels", Float.toString(wayPixels));
//        }
//
//        boolean isMatched = this.filter == null || this.filter.apply(tags);
//        if (isMatched) {
//            acceptWay(key, way);
//        }
//    }

    public void validateWay(long key, Way way) {
        float scaleDenominator = config.getScaleDenominator();
        if (!compareScaleDenominator(scaleDenominator)) {
            return;
        }

        HashMap<String, String> tags = new HashMap<>(way.tags);
        boolean isMatched = this.filter == null || this.filter.apply(tags);
        if (isMatched) {
            acceptWay(key, way);
        }
    }


    public void removeWay(String key) {
        for (HashMap<String, SymMeta> vertexArrayRaw : this.symMetaMapLists) {
            vertexArrayRaw.remove(key);
        }
    }

    private void acceptWay(String key, Way way) {
        for (int i = 0; i < symbolizers.size(); i++) {
            Symbolizer symbolizer = symbolizers.get(i);
            HashMap<String, SymMeta> symMetaMap = this.symMetaMapLists.get(i);
            try {
                symMetaMap.put(key, symbolizer.toDrawable(way));
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error drawing way " + key);
                System.err.println("With symbolizer " + symbolizer);
            }
        }
    }

    private void acceptWay(long key, Way way) {
//        System.out.println("acceptWay: " + key + " symbolizers: " + symbolizers);

        for (int i = 0; i < symbolizers.size(); i++) {
            Symbolizer symbolizer = symbolizers.get(i);
            HashMap<Long, SymMeta> symMetaMap = this.symMetaMapLongLists.get(i);
            try {
                symMetaMap.put(key, symbolizer.toDrawable(way));
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error drawing way " + key);
                System.err.println("With symbolizer " + symbolizer);
            }
        }
    }

//    public void save() {
//        for (int i = 0; i < symbolizers.size(); i++) {
//            HashMap<String, SymMeta> symMetaMap = this.symMetaMapLists.get(i);
//            drawingSymMetas.set(i, null);
//            for (SymMeta symMeta : symMetaMap.values()) {
//                SymMeta currentSymMeta = drawingSymMetas.get(i);
//                if (currentSymMeta == null) {
//                    drawingSymMetas.set(i, symMeta);
//                } else {
//                    drawingSymMetas.set(i, currentSymMeta.append(symMeta));
//                }
//            }
//        }
//    }

    public void save() {
        for (int i = 0; i < symbolizers.size(); i++) {
            drawingSymMetas.set(i, null);
            for (SymMeta symMeta : symMetaMapLongLists.get(i).values()) {
                SymMeta currentSymMeta = drawingSymMetas.get(i);
                if (currentSymMeta == null) {
                    drawingSymMetas.set(i, symMeta);
                } else {
                    drawingSymMetas.set(i, currentSymMeta.append(symMeta));
                }
            }
        }
    }

//    public void save() {
//        for (int i = 0; i < symbolizers.size(); i++) {
//            Class<?> symbolizerClass = symbolizers.get(i).getClass();
//            HashMap<String, SymMeta> symMetaMap = this.symMetaMapLists.get(i);
//            drawingSymMetasMap.clear();
//            for (SymMeta symMeta : symMetaMap.values()) {
//                SymMeta currentSymMeta = drawingSymMetasMap.get(symbolizerClass);
//                if (currentSymMeta == null) {
//                    drawingSymMetasMap.put(symbolizerClass, symMeta);
//                } else {
//                    drawingSymMetasMap.put(symbolizerClass, currentSymMeta.append(symMeta));
//                }
//            }
//        }
//    }

    public void draw() {
        if (drawingSymMetas.isEmpty()) return;

        for (SymMeta symMeta : drawingSymMetas) {
            if (symMeta != null) symMeta.draw(config);
        }
    }

    public static Function<HashMap<String, String>, Boolean> createFilterFunction(String filter) {
        if (filter == null || filter.isEmpty()) {
            return null;
        }
        // Pattern to match conditions in the filter string
        Pattern conditionPattern = Pattern.compile("\\[(\\w+)]\\s*([!=<>]+)\\s*('[^']+'|\\d+)");
        Matcher conditionMatcher = conditionPattern.matcher(filter);

        // Extract conditions and operators
        List<String> keys = new ArrayList<>();
        List<String> operators = new ArrayList<>();
        List<String> values = new ArrayList<>();

        while (conditionMatcher.find()) {
            keys.add(conditionMatcher.group(1));
            operators.add(conditionMatcher.group(2));
            values.add(conditionMatcher.group(3));
        }

        // Return a function that checks if the input HashMap satisfies the filter
        return (HashMap<String, String> tags) -> {
            if (tags == null || keys.size() != operators.size() || keys.size() != values.size()) {
                return false;
            }

            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                String operator = operators.get(i);
                String value = values.get(i).replace("'", "");

                String keyVal = tags.get(key);

                if (keyVal == null) {
                    return value.equals("null");
                }

                switch (operator) {
                    case "=":
                        if (!keyVal.equals(value)) {
                            return false;
                        }
                        break;
                    case "!=":
                        if (keyVal.equals(value)) {
                            return false;
                        }
                        break;
                    case ">":
                        if (Float.parseFloat(keyVal) <= Float.parseFloat(value)) {
                            return false;
                        }
                        break;
                    case "<":
                        if (Float.parseFloat(keyVal) >= Float.parseFloat(value)) {
                            return false;
                        }
                        break;
                    case ">=":
                        if (Float.parseFloat(keyVal) < Float.parseFloat(value)) {
                            return false;
                        }
                        break;
                    case "<=":
                        if (Float.parseFloat(keyVal) > Float.parseFloat(value)) {
                            return false;
                        }
                        break;
                    default:
                        return false;
                }
            }
            return true;
        };
    }

    public boolean hasTextSymbolizer() {
        for (Symbolizer symbolizer : symbolizers) {
            if (symbolizer instanceof TextSymbolizer) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Rule{").append('\n');
        sb.append("\tfilter=`").append(filterString).append('`').append('\n');
        sb.append("\tmaxScaleDenominator=`").append(maxScaleDenominator).append('`').append('\n');
        sb.append("\tminScaleDenominator=`").append(minScaleDenominator).append('`').append('\n');
        for (Symbolizer symbolizer : symbolizers) {
            sb.append("\t").append(symbolizer.toString()).append('\n');
        }
        sb.append('}');
        return sb.toString();
    }
}
