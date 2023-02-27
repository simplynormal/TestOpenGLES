package com.hcmut.test.utils;

import android.util.Pair;

import com.hcmut.test.geometry.LineStrip;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.Polygon;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.geometry.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StrokeGenerator {
    private static boolean DEBUG = true;

//    public static Point getIntersection(Point p1, Point p2, Point p3, Point p4) {
//        float m1 = (p2.y - p1.y) / (p2.x - p1.x);
//        float b1 = p1.y - m1 * p1.x;
//        float m2 = (p4.y - p3.y) / (p4.x - p3.x);
//        float b2 = p3.y - m2 * p3.x;
//
//        if (Float.compare(m1, m2) == 0) {
//            // lines are parallel, no intersection
//            return null;
//        }
//
//        float x = (b2 - b1) / (m1 - m2);
//
//        if (x < Math.min(p1.x, p2.x) || x > Math.max(p1.x, p2.x) || x < Math.min(p3.x, p4.x) || x > Math.max(p3.x, p4.x)) {
//            // intersection point is outside the range of the line segments
//            return null;
//        }
//
//        float y = m1 * x + b1;
//
//        if (y < Math.min(p1.y, p2.y) || y > Math.max(p1.y, p2.y) || y < Math.min(p3.y, p4.y) || y > Math.max(p3.y, p4.y)) {
//            // intersection point is outside the range of the line segments
//            return null;
//        }
//
//        return new Point(x, y);
//    }

    public static Point getIntersection(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        float denominator = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denominator == 0.0) { // Lines are parallel.
            return null;
        }
        float ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denominator;
        float ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denominator;
        if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f) {
            // Get the intersection point.
            return new Point((x1 + ua * (x2 - x1)), (y1 + ua * (y2 - y1)));
        }

        return null;
    }

    public static Point getIntersection(Point[] line1, Point[] line2) {
        return getIntersection(line1[0].x, line1[0].y, line1[1].x, line1[1].y, line2[0].x, line2[0].y, line2[1].x, line2[1].y);
    }


    private static class PolygonalBrush {
        private List<Point> points;
        private Point center;
        private int curActivePointIdx = 0;
        private int prevTangentCoef = 1;

        public PolygonalBrush(int n, float r, Point center) {
            assert n % 2 == 0 && n >= 4 : "n must be even and >= 4";
            points = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                float angle = (float) -(2 * Math.PI * i / n);
                points.add(new Point(center.x + r * (float) Math.cos(angle), center.y + r * (float) Math.sin(angle)));
            }
            this.center = center;
        }


        public void translate(Vector v) {
            List<Point> newPoints = new ArrayList<>();
            for (Point p : points) {
                newPoints.add(p.add(v));
            }
            points = newPoints;
            center = center.add(v);
        }

        public void translate(float dx, float dy) {
            translate(new Vector(dx, dy));
        }

        public void relocate(Point p) {
            translate(new Vector(center, p));
        }

        public Point[] getCurrentAndOppositeVertices(int i) {
            int n = points.size();
            return new Point[]{points.get(i), points.get((i + n / 2) % n)};
        }

        private int isInTangentRange(int i, Vector v) {
            int n = points.size();
            Point prev = points.get((i - 1 + n) % n);
            Point curr = points.get(i);
            Point next = points.get((i + 1) % n);

            Vector currToPrev = new Vector(curr, prev);
            Vector nextToCurr = new Vector(next, curr);

            Vector prevToCurr = new Vector(prev, curr);
            Vector currToNext = new Vector(curr, next);

            if (v.isInBetween(currToPrev, nextToCurr)) {
                return -1;
            } else if (v.isInBetween(prevToCurr, currToNext)) {
                return 1;
            } else {
                return 0;
            }
        }

        public List<Point> genEndCapVertices(Vector v) {
            List<Point> rv = new ArrayList<>();
            for (int i = 0; i < points.size(); i++) {
                int inTangentRange = isInTangentRange(i, v);
                if (inTangentRange != 0) {
                    int n = points.size();

                    if (inTangentRange == -1) {
                        for (int j = i; j != (i + n / 2) % n; j = (j + 1) % n) {
                            rv.add(points.get(j));
                        }
                        rv.add(points.get((i + n / 2) % n));
                        curActivePointIdx = (i + n / 2) % n;
                    } else {
                        for (int j = (i + n / 2) % n; j != i; j = (j + 1) % n) {
                            rv.add(points.get(j));
                        }
                        rv.add(points.get(i));
                        curActivePointIdx = i;
                    }
                    return rv;
                }
            }

            return rv;
        }

        public Pair<List<Point>, List<Point>> genTangentVertices(Vector v) {
            List<Point> rv1 = new ArrayList<>();
            List<Point> rv2 = new ArrayList<>();

            int n = points.size();
            for (int i = curActivePointIdx; i != (curActivePointIdx + n / 2) % n; i = (i + 1) % n) {
                int inTangentRange = isInTangentRange(i, v);
                if (inTangentRange == prevTangentCoef) {
                    for (int j = curActivePointIdx; j != i; j = (j + 1) % n) {
                        Point[] currAndOpposite = getCurrentAndOppositeVertices(j);
                        rv1.add(currAndOpposite[0]);
                        rv2.add(currAndOpposite[1]);
                    }
                    Point[] currAndOpposite = getCurrentAndOppositeVertices(i);
                    rv1.add(currAndOpposite[0]);
                    rv2.add(currAndOpposite[1]);
                    curActivePointIdx = i;
                    prevTangentCoef = inTangentRange;
                    return new Pair<>(rv1, rv2);
                }

                if (inTangentRange == -prevTangentCoef) {
                    int oppositeI = (i + n / 2) % n;
                    for (int j = curActivePointIdx; j != oppositeI; j = (j - 1 + n) % n) {
                        Point[] currAndOpposite = getCurrentAndOppositeVertices(j);
                        rv1.add(currAndOpposite[0]);
                        rv2.add(currAndOpposite[1]);
                    }
                    Point[] currAndOpposite = getCurrentAndOppositeVertices(oppositeI);
                    rv1.add(currAndOpposite[0]);
                    rv2.add(currAndOpposite[1]);
                    curActivePointIdx = oppositeI;
                    prevTangentCoef = -inTangentRange;
                    return new Pair<>(rv1, rv2);
                }
            }

            return new Pair<>(rv1, rv2);
        }
    }

    private static Pair<List<Point>, List<Point>> validateIntersection(List<Point> lastFirstTangentVertices1, List<Point> lastFirstTangentVertices2, List<Point> lastSecondTangentVertices1, List<Point> lastSecondTangentVertices2, Pair<List<Point>, List<Point>> tangentVertices) {
        Point[] firstLine1 = new Point[]{lastFirstTangentVertices2.get(lastFirstTangentVertices2.size() - 1), lastFirstTangentVertices1.get(0)};
        Point[] firstLine2 = new Point[]{lastFirstTangentVertices1.get(lastFirstTangentVertices1.size() - 1), tangentVertices.first.get(0)};
        Point[] secondLine1 = new Point[]{lastSecondTangentVertices2.get(lastSecondTangentVertices2.size() - 1), lastSecondTangentVertices1.get(0)};
        Point[] secondLine2 = new Point[]{lastSecondTangentVertices1.get(lastSecondTangentVertices1.size() - 1), tangentVertices.second.get(0)};

        Point firstIntersection = getIntersection(firstLine1, firstLine2);
        Point secondIntersection = getIntersection(secondLine1, secondLine2);

        boolean isFirstIntersectionAtEnd = firstIntersection != null && (firstIntersection.equals(firstLine1[1]) || firstIntersection.equals(firstLine2[1]));
        boolean isSecondIntersectionAtEnd = secondIntersection != null && (secondIntersection.equals(secondLine1[1]) || secondIntersection.equals(secondLine2[1]));

        List<Point> first;
        List<Point> second;

        if (firstIntersection != null && !isFirstIntersectionAtEnd) {
            first = List.of(firstIntersection);
        } else {
            first = lastFirstTangentVertices1;
        }

        if (secondIntersection != null && !isSecondIntersectionAtEnd) {
            second = List.of(secondIntersection);
        } else {
            second = new ArrayList<>(lastSecondTangentVertices1);
            Collections.reverse(second);
        }

        return new Pair<>(first, second);
    }

    private static Polygon generateStroke(LineStrip line, PolygonalBrush brush) {
        assert line.points.size() >= 2 : "line must have at least 2 points";
        List<Point> vertices = new ArrayList<>();
        List<Point> head = brush.genEndCapVertices(new Vector(line.points.get(0), line.points.get(1)));
        List<Point> firstHalf = new ArrayList<>();
        List<Point> secondHalf = new ArrayList<>();

        List<Point> lastFirstTangentVertices1 = List.of(head.get(head.size() - 1));
        List<Point> lastSecondTangentVertices1 = List.of(head.get(0));
        List<Point> lastFirstTangentVertices2 = null;
        List<Point> lastSecondTangentVertices2 = null;

        for (int i = 1; i < line.points.size() - 1; i++) {
            brush.relocate(line.points.get(i));
            Vector v = new Vector(line.points.get(i), line.points.get(i + 1));
            Pair<List<Point>, List<Point>> tangentVertices = brush.genTangentVertices(v);
            if (lastFirstTangentVertices2 != null) {
                Pair<List<Point>, List<Point>> validated = validateIntersection(lastFirstTangentVertices1, lastFirstTangentVertices2, lastSecondTangentVertices1, lastSecondTangentVertices2, tangentVertices);
                firstHalf.addAll(validated.first);
                secondHalf.addAll(0, validated.second);
            }

            lastFirstTangentVertices2 = lastFirstTangentVertices1;
            lastSecondTangentVertices2 = lastSecondTangentVertices1;
            lastFirstTangentVertices1 = tangentVertices.first;
            lastSecondTangentVertices1 = tangentVertices.second;
        }

        brush.relocate(line.points.get(line.points.size() - 1));
        List<Point> tail = brush.genEndCapVertices(new Vector(line.points.get(line.points.size() - 1), line.points.get(line.points.size() - 2)));

        if (lastFirstTangentVertices2 != null) {
            Pair<List<Point>, List<Point>> validated = validateIntersection(lastFirstTangentVertices1, lastFirstTangentVertices2, lastSecondTangentVertices1, lastSecondTangentVertices2, new Pair<>(List.of(tail.get(0)), List.of(tail.get(tail.size() - 1))));
            firstHalf.addAll(validated.first);
            secondHalf.addAll(0, validated.second);
        }

        vertices.addAll(head);
        vertices.addAll(firstHalf);
        vertices.addAll(tail);
        vertices.addAll(secondHalf);
        vertices.add(vertices.get(0));

        Polygon rv = new Polygon(vertices);

        if (DEBUG) {
            for (Point p : rv.points) {
                System.out.println("(" + p.x + ", " + p.y + ")");
            }
            DEBUG = false;
        }

        return rv;
    }

    public static Polygon generateStroke(LineStrip line, int n, float r) {
        return generateStroke(line, new PolygonalBrush(n, r, line.points.get(0)));
    }

    public static void test() {
//        Point p1 = new Point(0, 0);
//        Point q1 = new Point(-1, 1);
//        Point p2 = new Point(1, 0);
//        Point q2 = new Point(0, 1);
//
//        Point intersection = findIntersection(p1, q1, p2, q2);
//        System.out.println("Intersection: " + intersection);
    }
}
