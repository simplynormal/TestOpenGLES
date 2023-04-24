package com.hcmut.test.reader;

import android.content.Context;

import com.hcmut.test.osm.Node;
import com.hcmut.test.osm.Way;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class MapReader {
    public final HashMap<String, Way> ways = new HashMap<>();
    private final Context ctx;
    private final int resource;
    private final XmlPullParser xpp;
    float minlon = -1, maxlon = -1, minlat = -1, maxlat = -1;

    public MapReader(Context ctx, int resource) throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        xpp = factory.newPullParser();
        this.ctx = ctx;
        this.resource = resource;

//        read();
    }

    public void setBounds(float minlon, float maxlon, float minlat, float maxlat) {
        this.minlon = minlon;
        this.maxlon = maxlon;
        this.minlat = minlat;
        this.maxlat = maxlat;
    }

    public void read() {
        assert minlon != -1 && maxlon != -1 && minlat != -1 && maxlat != -1 :
                "Bounds not set";
        try {
            InputStream in_s = ctx.getResources().openRawResource(resource);
            xpp.setInput(in_s, null);
            HashMap<String, Node> nodes = new HashMap<>();

            int eventType = xpp.getEventType();
            Way wayParent = null;
            boolean isAtleastOneNodeInBound = false;
            int numNodes = 0;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String name = xpp.getName();
                if (eventType == XmlPullParser.START_TAG) {
                    switch (name) {
                        case "node":
                            float lon = Float.parseFloat(xpp.getAttributeValue(null, "lon"));
                            float lat = Float.parseFloat(xpp.getAttributeValue(null, "lat"));
                            nodes.put(xpp.getAttributeValue(null, "id"), new Node(
                                    lon,
                                    lat
                            ));
                            numNodes++;
                            break;
                        case "way":
                            String id = xpp.getAttributeValue(null, "id");
                            ways.put(id, new Way());
                            wayParent = ways.get(id);
                            break;
                        case "tag":
                            if (wayParent != null) {
                                wayParent.tags.put(xpp.getAttributeValue(null, "k"), xpp.getAttributeValue(null, "v"));
                            }
                            break;
                        case "nd":
                            if (wayParent != null) {
                                if (xpp.getAttributeValue(null, "ref") == null)
                                    throw new XmlPullParserException("nd tag has no ref attribute");
                                String ref = xpp.getAttributeValue(null, "ref");
                                Node node = nodes.get(ref);
                                if (node == null)
                                    throw new XmlPullParserException("Node with id " + ref + " not found");

                                boolean isInBound = node.lon >= minlon && node.lon <= maxlon && node.lat >= minlat && node.lat <= maxlat;
                                if (isInBound)
                                    isAtleastOneNodeInBound = true;
                                wayParent.addNode(node);
                            }
                            break;
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (name.equals("way")) {
                        if (wayParent != null) {
                            if (!isAtleastOneNodeInBound) {
                                ways.values().remove(wayParent);
                            } else {
                                wayParent.wrapUpNodes();
                            }
                        }
                        wayParent = null;
                        isAtleastOneNodeInBound = false;
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }
}
