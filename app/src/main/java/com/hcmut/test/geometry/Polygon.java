package com.hcmut.test.geometry;

import static com.menecats.polybool.helpers.PolyBoolHelper.epsilon;
import static com.menecats.polybool.helpers.PolyBoolHelper.point;
import static com.menecats.polybool.helpers.PolyBoolHelper.polygon;
import static com.menecats.polybool.helpers.PolyBoolHelper.region;

import com.hcmut.test.data.Node;
import com.hcmut.test.data.Way;
import com.menecats.polybool.Epsilon;
import com.menecats.polybool.PolyBool;

import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

import java.util.ArrayList;
import java.util.List;

public class Polygon {
    public final List<Point> points;

    public Polygon(List<Point> points) {
        this.points = points;
        checkInit();
    }

    public Polygon(Way way) {
        this.points = new ArrayList<>();
        for (Node node : way.nodes) {
            this.points.add(new Point(node.lon, node.lat));
        }
        checkInit();
    }

    public Polygon(Point[] points) {
        this.points = List.of(points);
        checkInit();
    }

    public Polygon(Point p1, Point p2, Point p3) {
        this.points = List.of(p1, p2, p3);
        checkInit();
    }

    public boolean isClosed() {
        return points.size() > 0 && points.get(0).equals(points.get(points.size() - 1));
    }

    private void checkInit() {
        assert points.size() > 3 : "Polygon must have at least 3 points";
        assert isClosed() : "Polygon must be closed";

        for (int i = 0; i < points.size() - 1; i++) {
            assert !points.get(i).equals(points.get(i + 1)) : "Polygon must not have duplicate points";
        }
    }

    public List<Triangle> triangulate() {
        ArrayList<PolygonPoint> triangulatePoints = new ArrayList<>();
        for (Point p : points) {
            triangulatePoints.add(new PolygonPoint(p.x, p.y));
        }

        org.poly2tri.geometry.polygon.Polygon polygon = new org.poly2tri.geometry.polygon.Polygon(triangulatePoints);
        Poly2Tri.triangulate(polygon);

        List<Triangle> triangles = new ArrayList<>();
        for (DelaunayTriangle triangle : polygon.getTriangles()) {
            triangles.add(new Triangle(
                    (float) triangle.points[0].getX(), (float) triangle.points[0].getY(), 0,
                    (float) triangle.points[1].getX(), (float) triangle.points[1].getY(), 0,
                    (float) triangle.points[2].getX(), (float) triangle.points[2].getY(), 0
            ));
        }
        return triangles;
    }

    private com.menecats.polybool.models.Polygon toPolyboolPolygon() {
        List<double[]> polyboolPoints = new ArrayList<>();

        for (Point p : points) {
            polyboolPoints.add(new double[]{p.x, p.y});
        }

        return new com.menecats.polybool.models.Polygon(List.of(polyboolPoints));
    }

    public void union(Polygon polygon) {
        Epsilon eps = epsilon();

        com.menecats.polybool.models.Polygon self = toPolyboolPolygon();
        com.menecats.polybool.models.Polygon other = polygon.toPolyboolPolygon();

        com.menecats.polybool.models.Polygon result = PolyBool.union(eps, self, other);

        points.clear();
        for (List<double[]> region : result.getRegions()) {
            for (double[] point : region) {
                points.add(new Point((float) point[0], (float) point[1]));
            }
        }

        points.add(points.get(0));
    }
}
