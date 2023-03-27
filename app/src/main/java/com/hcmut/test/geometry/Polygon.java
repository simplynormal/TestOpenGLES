package com.hcmut.test.geometry;

import com.hcmut.test.data.Node;
import com.hcmut.test.data.Way;
import com.hcmut.test.geometry.equation.LineEquation;

import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Polygon {
    public final List<Point> points;
    public final List<Polygon> holes;

    public Polygon(List<Point> points) {
        this.points = points;
        this.holes = new ArrayList<>();
        checkInit();
    }

    public Polygon(Way way) {
        this.points = new ArrayList<>();
        for (Node node : way.nodes) {
            this.points.add(new Point(node.lon, node.lat));
        }
        this.holes = new ArrayList<>();
        checkInit();
    }

    public Polygon(Point[] points) {
        this.points = new ArrayList<>(Arrays.asList(points));
        this.holes = new ArrayList<>();
        checkInit();
    }

    public Polygon(Point p1, Point p2, Point p3) {
        this.points = List.of(p1, p2, p3);
        this.holes = new ArrayList<>();
        checkInit();
    }

    public Polygon(float[] points) {
        this.points = new ArrayList<>();
        for (int i = 0; i < points.length; i += 3) {
            this.points.add(new Point(points[i], points[i + 1], points[i + 2]));
        }
        this.holes = new ArrayList<>();
        checkInit();
    }

    public void addHole(Polygon hole) {
        assert hole.holes.size() == 0 : "Hole cannot have hole";
        holes.add(hole);
    }

    public void addHole(List<Point> hole) {
        holes.add(new Polygon(hole));
    }

    public boolean isClosed() {
        return points.size() > 0 && points.get(0).equals(points.get(points.size() - 1));
    }

    private void checkInit() {
        assert points.size() > 3 : "Polygon must have at least 3 points";
        assert isClosed() : "Polygon must be closed";

        for (int i = 0; i < points.size() - 1; i++) {
            if (points.get(i).equals(points.get(i + 1))) {
                points.remove(i);
                i--;
            }
        }
        removeSameLinePoints();
    }

    public List<Triangle> triangulate() {
        ArrayList<PolygonPoint> triangulatePoints = new ArrayList<>();
        for (Point p : points) {
            triangulatePoints.add(new PolygonPoint(p.x, p.y));
        }

        org.poly2tri.geometry.polygon.Polygon polygon = new org.poly2tri.geometry.polygon.Polygon(triangulatePoints);
        for (Polygon hole : holes) {
            ArrayList<PolygonPoint> holePoints = new ArrayList<>();
            for (Point p : hole.points) {
                holePoints.add(new PolygonPoint(p.x, p.y));
            }
            polygon.addHole(new org.poly2tri.geometry.polygon.Polygon(holePoints));
        }
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

    public boolean doesContain(Point point) {
        Line line = new Line(point, new Point(-999999999, point.y));
        int count = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get((i + 1) % points.size());
            Line edge = new Line(p1, p2);
            if (line.intersect(edge) != null) {
                count++;
            }
        }

        return count % 2 == 1;
    }

    public void removeSameLinePoints() {
        List<Point> points = this.points.subList(0, this.points.size() - 1);
        for (int i = 0; i < points.size(); i++) {
            int middleIdx = (i + 1) % points.size();
            Point p1 = points.get(i);
            Point p2 = points.get(middleIdx);
            Point p3 = points.get((i + 2) % points.size());

            boolean isSameLine = new LineEquation(p1, p2).hasPoint(p3);
            if (isSameLine) {
                points.remove(middleIdx);
                if (middleIdx == 0) {
                    this.points.set(this.points.size() - 1, this.points.get(0));
                }
            }
        }
    }
}
