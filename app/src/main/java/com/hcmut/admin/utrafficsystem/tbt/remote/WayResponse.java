package com.hcmut.admin.utrafficsystem.tbt.remote;

import java.util.HashMap;

public class WayResponse {
    public final long id;
    public final long[] refs;
    public final HashMap<String, HashMap<String, String>> tags;

    public WayResponse(long id, long[] refs, HashMap<String, HashMap<String, String>> tags) {
        this.id = id;
        this.refs = refs;
        this.tags = tags;
    }
}
