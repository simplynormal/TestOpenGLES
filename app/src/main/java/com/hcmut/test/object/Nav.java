package com.hcmut.test.object;

import android.content.Context;

import com.hcmut.test.mapnik.symbolizer.LineSymbolizer;
import com.hcmut.test.mapnik.symbolizer.SymMeta;
import com.hcmut.test.osm.Node;
import com.hcmut.test.osm.Way;
import com.hcmut.test.utils.Config;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Nav {
    private final Config config;
    private static final float FILL_STROKE_WIDTH = 10;
    private static final float CASE_STROKE_WIDTH = 14;
    private SymMeta symMeta;

    private String loadJSONFromAsset(Context context) {
        String json;
        try {
            InputStream is = context.getAssets().open("nav1.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public Nav(Config config) {
        this.config = config;
        String json = loadJSONFromAsset(config.context);
        String fillStrokeWidth = String.valueOf(FILL_STROKE_WIDTH);
        String caseStrokeWidth = String.valueOf(CASE_STROKE_WIDTH);
        SymMeta fillSymMeta = null;
        SymMeta caseSymMeta = null;
        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length() - 1; i++) {
                JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                JSONArray coords = jsonObject.getJSONArray("coords");
                for (int j = 0; j < coords.length(); j++) {
                    JSONObject coord = (JSONObject) coords.get(j);
                    JSONObject status = (JSONObject) coord.get("status");
                    String color = status.getString("color");
                    float lat = Float.parseFloat(coord.getString("lat"));
                    float lon = Float.parseFloat(coord.getString("lng"));
                    float elat = Float.parseFloat(coord.getString("elat"));
                    float elon = Float.parseFloat(coord.getString("elng"));

                    Node node = new Node(lon, lat);
                    Node enode = new Node(elon, elat);

                    Way way = new Way(List.of(node, enode));
                    LineSymbolizer fillLineSymbolizer = new LineSymbolizer(config, fillStrokeWidth, color, null, "butt", "round", null, null);
                    LineSymbolizer caseLineSymbolizer = new LineSymbolizer(config, caseStrokeWidth, "#7092FF", null, "butt", "round", null, null);

                    SymMeta localFillSymMeta = fillLineSymbolizer.toDrawable(way, null);
                    SymMeta localCaseSymMeta = caseLineSymbolizer.toDrawable(way, null);
                    if (fillSymMeta == null) {
                        fillSymMeta = localFillSymMeta;
                        caseSymMeta = localCaseSymMeta;
                    } else {
                        fillSymMeta = fillSymMeta.append(localFillSymMeta);
                        caseSymMeta = caseSymMeta.append(localCaseSymMeta);
                    }
//                    if (this.symMeta == null) {
//                        this.symMeta = localCaseSymMeta.append(localFillSymMeta);
//                    } else {
//                        this.symMeta = this.symMeta.append(localCaseSymMeta.append(localFillSymMeta));
//                    }
                }
            }

            if (caseSymMeta != null) {
                symMeta = caseSymMeta.append(fillSymMeta);
                symMeta.save(config);
            }
//            symMeta.save(config);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
//        System.out.println(json);
    }

    public void draw() {
        if (symMeta != null) {
            symMeta.draw();
        }
    }
}
