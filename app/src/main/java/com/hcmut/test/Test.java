package com.hcmut.test;

import com.hcmut.test.algorithm.StrokeGenerator;
import com.menecats.polybool.Epsilon;
import com.menecats.polybool.PolyBool;
import com.menecats.polybool.models.Polygon;
import com.menecats.polybool.models.geojson.Geometry;

import static com.menecats.polybool.helpers.PolyBoolHelper.*;

import java.util.List;

public class Test {
    public static void union() {
        Epsilon eps = epsilon();

        Polygon union = PolyBool.union(
                eps,
                polygon(
                        region(
                                point(-5, 5),
                                point(0, 5),
                                point(0, 4),
                                point(-4, 4),
                                point(-4, -4),
                                point(0, -4),
                                point(0, -5),
                                point(-5, -5)
                        )
                ),
                polygon(
                        region(
                                point(0, 5),
                                point(5, 5),
                                point(5, -5),
                                point(0, -5),
                                point(0, -4),
                                point(4, -4),
                                point(4, 4),
                                point(0, 4)
                        )
                )
        );

        union = PolyBool.difference(eps, union, polygon(
                region(
                        point(-4.75, 4.75),
                        point(-4.25, 4.75),
                        point(-4.25, 4.25),
                        point(-4.75, 4.25)
                )
        ));

        union = PolyBool.union(eps, union, polygon(
                region(
                        point(6, 6),
                        point(7, 6),
                        point(7, 7),
                        point(6, 7)
                )
        ));

        System.out.println("============union============");
        System.out.println(union);

        Geometry<?> geometry = PolyBool.polygonToGeoJSON(eps, union);
        Geometry.PolygonGeometry polygonGeometry = null;
        Geometry.MultiPolygonGeometry multiPolygonGeometry = null;
        if (geometry instanceof Geometry.PolygonGeometry) {
            polygonGeometry = (Geometry.PolygonGeometry) geometry;
        } else if (geometry instanceof Geometry.MultiPolygonGeometry) {
            multiPolygonGeometry = (Geometry.MultiPolygonGeometry) geometry;
        }

        System.out.println("============GeoJSON============");
        System.out.println(geometry.getType());
        if (polygonGeometry != null)
            for (List<double[]> o : polygonGeometry.getCoordinates()) {
                System.out.println("[");
                for (double[] p : o) {
                    System.out.println("\t(" + p[0] + ", " + p[1] + ")");
                }
                System.out.println("]");
            }
        else if (multiPolygonGeometry != null)
            for (List<List<double[]>> o : multiPolygonGeometry.getCoordinates()) {
                System.out.println("[");
                for (List<double[]> p : o) {
                    System.out.println("\t[");
                    for (double[] q : p) {
                        System.out.println("\t\t(" + q[0] + ", " + q[1] + ")");
                    }
                    System.out.println("\t]");
                }
                System.out.println("]");
            }
    }

    public static void holes() {
        Epsilon eps = epsilon();

        Polygon pol = polygon(
                region(
                        point(-4, 4),
                        point(4, 4),
                        point(4, -4),
                        point(-4, -4)
                ),
                region(
                        point(-5, 5),
                        point(5, 5),
                        point(5, -5),
                        point(-5, -5)
                ),
                region(
                        point(6, 6),
                        point(7, 6),
                        point(7, 7),
                        point(6, 7)
                )
        );

//        pol = PolyBool.union(eps, pol, polygon(
//                region(
//                        point(-3, 3),
//                        point(3, 3),
//                        point(3, -3),
//                        point(-3, -3)
//                )
//        ));

        System.out.println("============union============");
        System.out.println(pol);

        Geometry<?> geometry = PolyBool.polygonToGeoJSON(eps, pol);
        Geometry.PolygonGeometry polygonGeometry = null;
        Geometry.MultiPolygonGeometry multiPolygonGeometry = null;
        if (geometry instanceof Geometry.PolygonGeometry) {
            polygonGeometry = (Geometry.PolygonGeometry) geometry;
        } else if (geometry instanceof Geometry.MultiPolygonGeometry) {
            multiPolygonGeometry = (Geometry.MultiPolygonGeometry) geometry;
        }

        System.out.println("============GeoJSON============");
        System.out.println(geometry.getType());
        if (polygonGeometry != null)
            for (List<double[]> o : polygonGeometry.getCoordinates()) {
                System.out.println("[");
                for (double[] p : o) {
                    System.out.println("\t(" + p[0] + ", " + p[1] + ")");
                }
                System.out.println("]");
            }
        else if (multiPolygonGeometry != null)
            for (List<List<double[]>> o : multiPolygonGeometry.getCoordinates()) {
                System.out.println("[");
                for (List<double[]> p : o) {
                    System.out.println("\t[");
                    for (double[] q : p) {
                        System.out.println("\t\t(" + q[0] + ", " + q[1] + ")");
                    }
                    System.out.println("\t]");
                }
                System.out.println("]");
            }
    }

    public static void main() {
//        StrokeGenerator.test();
    }
}
