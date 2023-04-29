package com.hcmut.test.osm;

import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.PointList;
import com.hcmut.test.geometry.Polygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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

    public void addMember(String role, Element element) {
        members.add(new Member(role, element));
    }

    @Override
    public List<PointList> toPointLists(float originX, float originY, float scale) {
        String type = tags.get("type");
        if (Objects.equals(type, "multipolygon")) {
            return multiPolyToPointLists(originX, originY, scale);
        }

        List<PointList> rv = new ArrayList<>();
        for (Member member : members) {
            rv.addAll(member.element.toPointLists(originX, originY, scale));
        }
        return rv;
    }

    @Override
    public List<PointList> toPointLists(float scale) {
        return toPointLists(0, 0, scale);
    }

    @Override
    public List<PointList> toPointLists() {
        return toPointLists(0, 0, 1);
    }

    private List<PointList> multiPolyToPointLists(float originX, float originY, float scale) {
        List<Polygon> polygons = new ArrayList<>();
        List<Point> points = new ArrayList<>();
        boolean isOuter = true;
        for (Member member : members) {
            if (!(member.element instanceof Way)) continue;
            Way way = (Way) member.element;
            boolean currentIsOuter = member.role.equals("outer");
            if (isOuter != currentIsOuter) {
                if (isOuter) {
                    try {
                        polygons.add(new Polygon(points));
                    } catch (Exception | AssertionError e) {
                        System.err.println("Error when multiPolyToPointLists:");
                        e.printStackTrace();
                        return new ArrayList<>();
                    }
                } else {
                    polygons.get(polygons.size() - 1).addHole(points);
                }
                points = new ArrayList<>();
                isOuter = currentIsOuter;
            } else {
                if (points.size() == 0) {
                    points.addAll(way.toPoints());
                } else {
                    Point first = points.get(0);
                    Point last = points.get(points.size() - 1);
                    Point firstOfWay = way.nodes.get(0).toPoint();
                    Point lastOfWay = way.nodes.get(way.nodes.size() - 1).toPoint();

                    if (first.equals(firstOfWay)) {
                        points.addAll(0, way.toPoints(originX, originY, scale));
                    } else if (first.equals(lastOfWay)) {
                        List<Point> wayPoints = way.toPoints(originX, originY, scale);
                        for (int i = wayPoints.size() - 1; i >= 0; i--) {
                            points.add(0, wayPoints.get(i));
                        }
                    } else if (last.equals(firstOfWay)) {
                        points.addAll(way.toPoints(originX, originY, scale));
                    } else if (last.equals(lastOfWay)) {
                        points.addAll(way.toPoints(originX, originY, scale));
                    } else {
                        try {
                            polygons.add(new Polygon(points));
                        } catch (Exception | AssertionError e) {
                            System.err.println("Error when multiPolyToPointLists:");
                            e.printStackTrace();
                            return new ArrayList<>();
                        }
                        points = way.toPoints();
                    }
                }
            }
        }

        // cast to PointList
        return new ArrayList<>(polygons);
    }
}
