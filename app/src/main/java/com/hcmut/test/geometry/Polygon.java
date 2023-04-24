package com.hcmut.test.geometry;

import com.hcmut.test.osm.Node;
import com.hcmut.test.osm.Way;
import com.hcmut.test.geometry.equation.LineEquation;

import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

import java.util.ArrayList;
import java.util.List;

public class Polygon extends PointList {
    public final List<Polygon> holes;
    private final List<Triangle> triangulatedTriangles = new ArrayList<>();

    public Polygon(List<Point> points) {
        super(points);
        this.holes = new ArrayList<>();
        checkInit();
    }

    public Polygon(PointList pointList) {
        this(pointList.points);
    }

    public Polygon(Way way) {
        this(new ArrayList<>() {{
            for (Node node : way.nodes) {
                add(new Point(node.lon, node.lat));
            }
        }});
    }

    public void addHole(Polygon hole) {
        assert hole.holes.size() == 0 : "Hole cannot have hole";
        holes.add(hole);
    }

    public void addHole(List<Point> hole) {
        holes.add(new Polygon(hole));
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
        if (triangulatedTriangles.size() > 0) {
            return triangulatedTriangles;
        }

        ArrayList<PolygonPoint> triangulatePoints = new ArrayList<>();
        for (Point p : points) {
            triangulatePoints.add(new PolygonPoint(p.x, p.y));
        }

        float curZ = points.get(0).z;

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
                    (float) triangle.points[0].getX(), (float) triangle.points[0].getY(), curZ,
                    (float) triangle.points[1].getX(), (float) triangle.points[1].getY(), curZ,
                    (float) triangle.points[2].getX(), (float) triangle.points[2].getY(), curZ
            ));
        }

        triangulatedTriangles.addAll(triangles);
        return triangles;
    }

    public float getArea() {
        if (triangulatedTriangles.size() == 0) {
            triangulate();
        }

        float area = 0;
        for (Triangle triangle : triangulatedTriangles) {
            area += triangle.getArea();
        }
        return area;
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

    public Polygon scale(float scale) {
        List<Point> scaledPoints = new ArrayList<>();
        for (Point point : points) {
            scaledPoints.add(point.scale(scale));
        }
        Polygon rv = new Polygon(scaledPoints);
        for (Polygon hole : holes) {
            rv.addHole(hole.scale(scale));
        }
        for (Triangle triangle : triangulatedTriangles) {
            Triangle scaledTriangle = new Triangle(
                    triangle.p1.scale(scale),
                    triangle.p2.scale(scale),
                    triangle.p3.scale(scale)
            );
            rv.triangulatedTriangles.add(scaledTriangle);
        }
        return rv;
    }

    public Polygon transform(float originX, float originY, float scale) {
        List<Point> scaledPoints = new ArrayList<>();
        for (Point point : points) {
            scaledPoints.add(point.transform(originX, originY, scale));
        }
        Polygon rv = new Polygon(scaledPoints);
        for (Polygon hole : holes) {
            rv.addHole(hole.transform(originX, originY, scale));
        }
        for (Triangle triangle : triangulatedTriangles) {
            Triangle scaledTriangle = new Triangle(
                    triangle.p1.transform(originX, originY, scale),
                    triangle.p2.transform(originX, originY, scale),
                    triangle.p3.transform(originX, originY, scale)
            );
            rv.triangulatedTriangles.add(scaledTriangle);
        }
        return rv;
    }
}
