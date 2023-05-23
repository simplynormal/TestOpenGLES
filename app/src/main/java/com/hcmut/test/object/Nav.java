package com.hcmut.test.object;

import android.content.Context;

import com.hcmut.test.mapnik.symbolizer.LineSymbolizer;
import com.hcmut.test.mapnik.symbolizer.SymMeta;
import com.hcmut.test.osm.Node;
import com.hcmut.test.osm.Way;
import com.hcmut.test.remote.Coord;
import com.hcmut.test.remote.DirectResponse;
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

    public Nav(Config config) {
        this.config = config;
    }

    public void setRoute(DirectResponse directResponse) {
        String fillStrokeWidth = String.valueOf(FILL_STROKE_WIDTH);
        String caseStrokeWidth = String.valueOf(CASE_STROKE_WIDTH);
        SymMeta fillSymMeta = null;
        SymMeta caseSymMeta = null;
        for (Coord coord : directResponse.getCoords()) {
            Node node = new Node((float) coord.getLng(), (float) coord.getLat());
            Node enode = new Node((float) coord.geteLng(), (float) coord.geteLat());
            String color = coord.getStatus().color;

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
        }

        if (caseSymMeta != null) {
            symMeta = caseSymMeta.append(fillSymMeta);
            symMeta.save(config);
        }
    }

    public void draw() {
        if (symMeta != null) {
            symMeta.draw();
        }
    }
}
