package com.hcmut.test.mapnik;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.hcmut.test.algorithm.CoordinateTransform;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.mapnik.symbolizer.SymMeta;
import com.hcmut.test.mapnik.symbolizer.Symbolizer;
import com.hcmut.test.osm.Element;
import com.hcmut.test.osm.Node;
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
    private final HashMap<String, Element> elements = new HashMap<>();
    private final List<HashMap<String, SymMeta>> symMetaMapLists = new ArrayList<>();
    private final List<Symbolizer> symbolizers = new ArrayList<>();
    private final List<SymMeta> drawingSymMetas = new ArrayList<>();
    private final Config config;
    //    private final Runnable unsubscribe;
    private String filterString;

    public Rule(Config config, Float maxScaleDenominator, Float minScaleDenominator, String filter) {
        this.config = config;
        this.maxScaleDenominator = maxScaleDenominator;
        this.minScaleDenominator = minScaleDenominator;
        this.filter = createFilterFunction(filter);
        this.filterString = filter;
//        this.unsubscribe = config.addListener((Config _config, Set<Config.Property> diffs) -> {
//            if (!diffs.contains(Config.Property.SCALE_DENOMINATOR)) return;
//        });
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

    public void validateElement(String key, Element element) {
//        System.out.println("validateWay: " + key + " filter: " + filterString);
        if (element instanceof Node || element.tags == null) {
            return;
        }

        float scaleDenominator = config.getScaleDenominator();
        if (!compareScaleDenominator(scaleDenominator)) {
            return;
        }

        HashMap<String, String> tags = new HashMap<>(element.tags);
        String wayPixelsString = tags.get("way_pixels");
        float scaledPixel = CoordinateTransform.getScalePixel(scaleDenominator);
        if (wayPixelsString != null) {
            float wayPixels = Float.parseFloat(wayPixelsString) * scaledPixel * scaledPixel;
//            System.out.println("way_pixels: " + wayPixels);
            tags.put("way_pixels", Float.toString(wayPixels));
        }

        boolean isMatched = this.filter == null || this.filter.apply(tags);
        if (isMatched) {
            List<PointList> shapes = element.toPointLists(config.getOriginX(), config.getOriginY(), scaledPixel * config.getLengthPerPixel());
            acceptElement(key, element, shapes);
        }
    }

    public void removeElement(String key) {
        elements.remove(key);
        for (HashMap<String, SymMeta> vertexArrayRaw : this.symMetaMapLists) {
            vertexArrayRaw.remove(key);
        }
    }

    private void acceptElement(String key, Element element, List<PointList> shapes) {
        elements.put(key, element);

//        if (!symbolizers.isEmpty()) {
//            System.out.println("Way accepted: " + key);
//        }

        for (int i = 0; i < symbolizers.size(); i++) {
            Symbolizer symbolizer = symbolizers.get(i);
            HashMap<String, SymMeta> symMetaMap = this.symMetaMapLists.get(i);
            for (PointList shape : shapes) {
                try {
                    symMetaMap.put(key, symbolizer.toDrawable(element, shape));
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Error drawing way " + key);
                    for (Point point : shape.points) {
                        System.err.println(point.x + "f, " + point.y + "f, " + point.z + "f,");
                    }
                }
            }
        }
    }

    public void save() {
        for (int i = 0; i < symbolizers.size(); i++) {
            HashMap<String, SymMeta> symMetaMap = this.symMetaMapLists.get(i);
            drawingSymMetas.set(i, null);
            for (SymMeta symMeta : symMetaMap.values()) {
                SymMeta currentSymMeta = drawingSymMetas.get(i);
                if (currentSymMeta == null) {
                    drawingSymMetas.set(i, symMeta);
                } else {
                    drawingSymMetas.set(i, currentSymMeta.append(symMeta));
                }
            }
        }
    }

    public void draw() {
        if (drawingSymMetas.isEmpty()) return;

        for (int i = 0; i < symbolizers.size(); i++) {
            Symbolizer symbolizer = symbolizers.get(i);
            SymMeta symMeta = drawingSymMetas.get(i);
            if (symMeta != null) {
                symbolizer.draw(symMeta);
            }
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

                boolean found = false;
                if (Objects.equals(key, "feature")) {
                    for (String tagKey : tags.keySet()) {
                        String tagValue = tags.get(tagKey);
                        String feature = tagKey + "_" + tagValue;
                        if (feature.equals(value)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) return false;
                    continue;
                }

                String keyVal = tags.get(key);

                if (keyVal == null) {
                    keyVal = tags.get(key.replace("_", ":"));
                    if (keyVal == null && !value.equals("null")) {
                        return false;
                    } else {
                        continue;
                    }
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

    public static void test() {
        String filter = "([feature] = 'amenity_fire_station') and ([way_pixels] > 900) and ([ways] <= 1100)";
        Function<HashMap<String, String>, Boolean> filterFunction = createFilterFunction(filter);

        HashMap<String, String> tags1 = new HashMap<>();
        tags1.put("amenity", "fire_station");
        tags1.put("way_pixels", "1000");
        tags1.put("ways", "1100");
        System.out.println("======([feature] = 'amenity_fire_station') and ([way_pixels] = 1000) " + filterFunction.apply(tags1)); // true

        HashMap<String, String> tags2 = new HashMap<>();
        tags2.put("amenity", "fire_station");
        tags2.put("way_pixels", "500");
        tags2.put("ways", "1000");
        System.out.println("======([feature] = 'amenity_fire_station') and ([way_pixels] = 1000) " + filterFunction.apply(tags2)); // false
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
//        unsubscribe.run();
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
