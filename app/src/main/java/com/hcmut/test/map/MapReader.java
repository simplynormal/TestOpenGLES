package com.hcmut.test.map;

import android.content.Context;
import android.content.res.Resources;

import com.hcmut.test.data.Node;
import com.hcmut.test.data.Way;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class MapReader {
    private final XmlPullParser xpp;
    public final HashMap<String, Way> ways = new HashMap<>();

    public Node center;
    public float height;
    public float width;

    public MapReader(Context ctx, int resource) throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        xpp = factory.newPullParser();

        Resources res = ctx.getResources();
        InputStream in_s = res.openRawResource(resource);
        xpp.setInput(in_s, null);

        read();
    }

    public void read() {
        try {
            HashMap<String, Node> nodes = new HashMap<>();

            int eventType = xpp.getEventType();
            Way wayParent = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = xpp.getName();
                    switch (name) {
                        case "bounds":
                            float minlon = Float.parseFloat(xpp.getAttributeValue(null, "minlon"));
                            float maxlon = Float.parseFloat(xpp.getAttributeValue(null, "maxlon"));
                            float minlat = Float.parseFloat(xpp.getAttributeValue(null, "minlat"));
                            float maxlat = Float.parseFloat(xpp.getAttributeValue(null, "maxlat"));
                            center = new Node((minlon + maxlon) / 2, (minlat + maxlat) / 2);
                            height = maxlat - minlat;
                            width = maxlon - minlon;
                            break;
                        case "node":
                            nodes.put(xpp.getAttributeValue(null, "id"), new Node(
                                    Float.parseFloat(xpp.getAttributeValue(null, "lon")),
                                    Float.parseFloat(xpp.getAttributeValue(null, "lat"))
                            ));
                            break;
                        case "way":
                            String id = xpp.getAttributeValue(null, "id");
                            ways.put(id, new Way());
                            wayParent = ways.get(id);
                            break;
                        case "nd":
                            if (wayParent != null) {
                                if (xpp.getAttributeValue(null, "ref") == null)
                                    throw new XmlPullParserException("nd tag has no ref attribute");
                                String ref = xpp.getAttributeValue(null, "ref");
                                wayParent.addNode(nodes.get(ref));
                            }
                            break;
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    String name = xpp.getName();
                    if (name.equals("way")) {
                        wayParent = null;
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }

    public void printObj() {
        System.out.println("Ways:");
        for (String key : ways.keySet()) {
            System.out.println(key + ": " + ways.get(key));
        }
    }
}
