package com.hcmut.test.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.hcmut.test.osm.Node;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
public class WayEntity {
    @PrimaryKey
    public long id;

    public String tileIds;
    public String nodes;
    public String tags;

    public WayEntity() {
    }

    public WayEntity(long id, List<Node> nodes, HashMap<String, HashMap<String, String>> tags, Set<Integer> tileIds) {
        this.id = id;
        this.nodes = Objects.requireNonNull(JSONObject.wrap(nodes)).toString();
        this.tags = Objects.requireNonNull(JSONObject.wrap(tags)).toString();
        this.tileIds = Objects.requireNonNull(JSONObject.wrap(tileIds)).toString();
    }
}
