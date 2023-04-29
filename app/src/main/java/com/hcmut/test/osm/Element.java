package com.hcmut.test.osm;

import com.hcmut.test.geometry.PointList;

import java.util.HashMap;
import java.util.List;

public abstract class Element {
    public final HashMap<String, String> tags;

    protected Element() {
        tags = new HashMap<>(0);
    }

    abstract public List<PointList> toPointLists();
    abstract public List<PointList> toPointLists(float scale);
    abstract public List<PointList> toPointLists(float originX, float originY, float scale);
}
