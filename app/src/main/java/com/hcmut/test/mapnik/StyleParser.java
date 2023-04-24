package com.hcmut.test.mapnik;

import android.content.Context;
import android.util.Log;

import com.hcmut.test.mapnik.symbolizer.LineSymbolizer;
import com.hcmut.test.mapnik.symbolizer.PolygonSymbolizer;
import com.hcmut.test.mapnik.symbolizer.TextSymbolizer;
import com.hcmut.test.osm.Way;
import com.hcmut.test.utils.Config;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StyleParser {
    private final Context ctx;
    private final int resource;
    private final Config config;
    private final XmlPullParser xpp;
    public final List<Layer> layers = new ArrayList<>();

    public StyleParser(Context ctx, int resource, Config config) throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        xpp = factory.newPullParser();
        this.ctx = ctx;
        this.resource = resource;
        this.config = config;
    }

    public void read() {
        try {
            InputStream in_s = ctx.getResources().openRawResource(resource);
            xpp.setInput(in_s, null);

            int eventType = xpp.getEventType();
            List<Style> styles = new ArrayList<>();
            Layer layerParent = null;
            Style styleParent = null;
            Rule ruleParent = null;
            String srs = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String name = xpp.getName();
                if (eventType == XmlPullParser.START_TAG) {
                    switch (name) {
                        case "Map":
                            srs = xpp.getAttributeValue(null, "srs");
                            break;
                        case "Layer":
                            layerParent = new Layer();
                            break;
                        case "StyleName":
                            assert layerParent != null;
                            layerParent.addStyleName(xpp.nextText());
                            break;
                        case "Parameter":
                            String paramName = xpp.getAttributeValue(null, "name");
                            if (paramName.equals("table")) {
                                assert layerParent != null;
                                layerParent.setQuery(xpp.nextText());
                            }
                            break;
                        case "Style":
                            styleParent = new Style(xpp.getAttributeValue(null, "name"));
                            break;
                        case "Rule":
                            ruleParent = new Rule(config);
                            break;
                        case "MaxScaleDenominator":
                            if (ruleParent == null) break;
                            ruleParent.setMaxScaleDenominator(Float.parseFloat(xpp.nextText()));
                            break;
                        case "MinScaleDenominator":
                            if (ruleParent == null) break;
                            ruleParent.setMinScaleDenominator(Float.parseFloat(xpp.nextText()));
                            break;
                        case "Filter":
                            if (ruleParent == null) break;
                            ruleParent.setFilter(xpp.nextText());
                            break;
                        case "LineSymbolizer":
                            if (ruleParent == null) break;
                            // LineSymbolizer keys: [stroke-opacity, offset, stroke-linejoin, stroke-dasharray, stroke-width, stroke, clip, stroke-linecap]
                            String stroke = xpp.getAttributeValue(null, "stroke");
                            String strokeWidth = xpp.getAttributeValue(null, "stroke-width");
                            String strokeOpacity = xpp.getAttributeValue(null, "stroke-opacity");
                            String strokeLinecap = xpp.getAttributeValue(null, "stroke-linecap");
                            String strokeLinejoin = xpp.getAttributeValue(null, "stroke-linejoin");
                            String strokeDasharray = xpp.getAttributeValue(null, "stroke-dasharray");
                            String offset = xpp.getAttributeValue(null, "offset");

                            ruleParent.addSymbolizer(new LineSymbolizer(
                                    config,
                                    strokeWidth,
                                    stroke,
                                    strokeDasharray,
                                    strokeLinecap,
                                    strokeLinejoin,
                                    strokeOpacity,
                                    offset
                            ));
                            break;
                        case "PolygonSymbolizer":
                            if (ruleParent == null) break;
                            // PolygonSymbolizer keys: [fill, fill-opacity, gamma, clip]
                            String fill = xpp.getAttributeValue(null, "fill");
                            String fillOpacity = xpp.getAttributeValue(null, "fill-opacity");
                            ruleParent.addSymbolizer(new PolygonSymbolizer(config, fill, fillOpacity));
                            break;
//                        case "TextSymbolizer":
//                            if (ruleParent == null) break;
//                            /*
//                             * textName
//                             * dx
//                             * dy
//                             * spacing
//                             * repeatDistance
//                             * maxCharAngleDelta
//                             * fill
//                             * opacity
//                             * placement
//                             * verticalAlignment
//                             * horizontalAlignment
//                             * justifyAlignment
//                             * wrapWidth
//                             * size
//                             * haloFill
//                             * haloRadius
//                             * */
//                            String dx = xpp.getAttributeValue(null, "dx");
//                            String dy = xpp.getAttributeValue(null, "dy");
//                            String spacing = xpp.getAttributeValue(null, "spacing");
//                            String repeatDistance = xpp.getAttributeValue(null, "repeatDistance");
//                            String maxCharAngleDelta = xpp.getAttributeValue(null, "maxCharAngleDelta");
//                            String fill1 = xpp.getAttributeValue(null, "fill");
//                            String opacity = xpp.getAttributeValue(null, "opacity");
//                            String placement = xpp.getAttributeValue(null, "placement");
//                            String verticalAlignment = xpp.getAttributeValue(null, "verticalAlignment");
//                            String horizontalAlignment = xpp.getAttributeValue(null, "horizontalAlignment");
//                            String justifyAlignment = xpp.getAttributeValue(null, "justifyAlignment");
//                            String wrapWidth = xpp.getAttributeValue(null, "wrapWidth");
//                            String size = xpp.getAttributeValue(null, "size");
//                            String haloFill = xpp.getAttributeValue(null, "haloFill");
//                            String haloRadius = xpp.getAttributeValue(null, "haloRadius");
//                            String textName = xpp.nextText();
//                            ruleParent.addSymbolizer(new TextSymbolizer(
//                                    config,
//                                    textName,
//                                    dx,
//                                    dy,
//                                    spacing,
//                                    repeatDistance,
//                                    maxCharAngleDelta,
//                                    fill1,
//                                    opacity,
//                                    placement,
//                                    verticalAlignment,
//                                    horizontalAlignment,
//                                    justifyAlignment,
//                                    wrapWidth,
//                                    size,
//                                    haloFill,
//                                    haloRadius
//                            ));
//                            break;
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    switch (name) {
                        case "Rule":
                            assert ruleParent != null && styleParent != null;
                            if (ruleParent.compareScaleDenominator(config.getScaleDenominator())) {
                                styleParent.rules.add(ruleParent);
                            }
                            ruleParent = null;
                            break;
                        case "Style":
                            assert styleParent != null;
                            styles.add(0, styleParent);
                            styleParent = null;
                            break;
                        case "Layer":
                            assert layerParent != null;
                            layerParent.validateStyles(styles);
                            layers.add(layerParent);
                            layerParent = null;
                            break;
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void validateWays(HashMap<String, Way> ways) {
        Log.v("StyleParser", "validating ways: " + ways.size());
        for (Layer layer : layers) {
            layer.validateWays(ways);
        }

        Log.v("StyleParser", "ways validated");
    }

    public static void test(Context ctx, int resource) {
//        System.out.println("==========test==========");
//        try {
//            StyleParser styleReader = new StyleParser(ctx, resource, new Config(null, null));
//            styleReader.read();
//            System.out.println("========Rules: " + styleReader.rules.size());
//        } catch (XmlPullParserException e) {
//            e.printStackTrace();
//        }
    }
}
