package com.hcmut.test.geometry;

public class Line {
    public final Point p1;
    public final Point p2;

    public Line(Point p1, Point p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public Point intersect(Line line) {
        float denominator = (line.p2.y - line.p1.y) * (p2.x - p1.x) - (line.p2.x - line.p1.x) * (p2.y - p1.y);
        if (denominator == 0) { // Lines are parallel.
            return null;
        }
        float ua = ((line.p2.x - line.p1.x) * (p1.y - line.p1.y) - (line.p2.y - line.p1.y) * (p1.x - line.p1.x)) / denominator;
        float ub = ((p2.x - p1.x) * (p1.y - line.p1.y) - (p2.y - p1.y) * (p1.x - line.p1.x)) / denominator;
        if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f) {
            // Get the intersection point.
            return new Point((p1.x + ua * (p2.x - p1.x)), (p1.y + ua * (p2.y - p1.y)));
        }

        return null;
    }

    public static Point getIntersection(Point p1, Point p2, Point p3, Point p4) {
        Line line1 = new Line(p1, p2);
        Line line2 = new Line(p3, p4);
        return line1.intersect(line2);
    }

    public static Point getIntersection(Line line1, Line line2) {
        return line1.intersect(line2);
    }
}
