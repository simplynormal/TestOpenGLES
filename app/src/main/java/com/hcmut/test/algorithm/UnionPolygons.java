package com.hcmut.test.algorithm;

import static com.menecats.polybool.helpers.PolyBoolHelper.epsilon;
import static com.menecats.polybool.helpers.PolyBoolHelper.polygon;

import com.hcmut.test.geometry.Point;
import com.menecats.polybool.Epsilon;
import com.menecats.polybool.PolyBool;
import com.menecats.polybool.models.Polygon;
import com.menecats.polybool.models.geojson.Geometry;

import java.util.ArrayList;
import java.util.List;

public class UnionPolygons {
    private static com.hcmut.test.geometry.Polygon fromPolygonGeoJSON(Geometry.PolygonGeometry polygonGeometry) {
        com.hcmut.test.geometry.Polygon pol = null;
        boolean isFirst = true;
        for (List<double[]> o : polygonGeometry.getCoordinates()) {
            List<Point> points = new ArrayList<>();
            if (isFirst) {
                for (double[] p : o) {
                    points.add(new Point((float) p[0], (float) p[1]));
                }
                pol = new com.hcmut.test.geometry.Polygon(points);
                isFirst = false;
            } else {
                for (double[] p : o) {
                    points.add(new Point((float) p[0], (float) p[1]));
                }
                pol.addHole(new com.hcmut.test.geometry.Polygon(points));
            }
        }
        return pol;
    }

    private static List<com.hcmut.test.geometry.Polygon> fromMultiPolygonGeoJSON(Geometry.MultiPolygonGeometry multiPolygonGeometry) {
        List<com.hcmut.test.geometry.Polygon> rv = new ArrayList<>();
        for (List<List<double[]>> o : multiPolygonGeometry.getCoordinates()) {
            com.hcmut.test.geometry.Polygon pol = null;
            boolean isFirst = true;
            for (List<double[]> p : o) {
                List<Point> points = new ArrayList<>();
                if (isFirst) {
                    for (double[] q : p) {
                        points.add(new Point((float) q[0], (float) q[1]));
                    }
                    pol = new com.hcmut.test.geometry.Polygon(points);
                    isFirst = false;
                } else {
                    for (double[] q : p) {
                        points.add(new Point((float) q[0], (float) q[1]));
                    }
                    pol.addHole(new com.hcmut.test.geometry.Polygon(points));
                }
            }
            rv.add(pol);
        }
        return rv;
    }

    private static List<com.hcmut.test.geometry.Polygon> toPolygon(Epsilon eps, Polygon polygon) {
        List<com.hcmut.test.geometry.Polygon> rv = new ArrayList<>();

        Geometry<?> geometry = PolyBool.polygonToGeoJSON(eps, polygon);
        Geometry.PolygonGeometry polygonGeometry = null;
        Geometry.MultiPolygonGeometry multiPolygonGeometry = null;
        if (geometry instanceof Geometry.PolygonGeometry) {
            polygonGeometry = (Geometry.PolygonGeometry) geometry;
        } else if (geometry instanceof Geometry.MultiPolygonGeometry) {
            multiPolygonGeometry = (Geometry.MultiPolygonGeometry) geometry;
        }

        if (polygonGeometry != null) {
            rv.add(fromPolygonGeoJSON(polygonGeometry));
        }
        else if (multiPolygonGeometry != null) {
            rv.addAll(fromMultiPolygonGeoJSON(multiPolygonGeometry));
        }

        return rv;
    }

    public static List<com.hcmut.test.geometry.Polygon> union(com.hcmut.test.geometry.Polygon p1, com.hcmut.test.geometry.Polygon p2) {
        Epsilon eps = new Epsilon();
        Polygon polygon1 = p1.toPolyBoolPolygon();
        Polygon polygon2 = p2.toPolyBoolPolygon();

        Polygon union = PolyBool.union(eps, polygon1, polygon2);
        return toPolygon(eps, union);
    }

    public static List<com.hcmut.test.geometry.Polygon> union(List<com.hcmut.test.geometry.Polygon> polygons) {
        Epsilon eps = new Epsilon();
        Polygon union = null;
        for (com.hcmut.test.geometry.Polygon p : polygons) {
            if (union == null) {
                union = p.toPolyBoolPolygon();
            } else {
                union = PolyBool.union(eps, union, p.toPolyBoolPolygon());
            }
        }
        return toPolygon(eps, union);
    }
}
