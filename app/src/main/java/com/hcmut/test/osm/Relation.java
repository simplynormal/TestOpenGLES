package com.hcmut.test.osm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Relation extends Element {
    public static class Member {
        public String role;
        public Element element;

        public Member(String role, Element element) {
            this.role = role;
            this.element = element;
        }
    }

    public final List<Member> members = new ArrayList<>();
    public final HashMap<String, String> tags = new HashMap<>();

    public void addMember(String role, Element element) {
        members.add(new Member(role, element));
    }
}
