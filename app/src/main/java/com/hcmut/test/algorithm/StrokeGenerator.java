package com.hcmut.test.algorithm;


import android.util.Pair;

import com.hcmut.test.geometry.Line;
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
    private static final float Z_BORDER = -5e-4f;

    private static class PolygonalBrush {
        private List<Point> points;
        private Point center;
        private int curActivePointIdx = 0;
        private int prevTangentCoef = 1;

        public PolygonalBrush(int n, float r, Point center) {
            this(n, r, center, 0);
        }

        public PolygonalBrush(int n, float r, Point center, float z) {
            assert n % 2 == 0 && n >= 4 : "n must be even and >= 4";
            points = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                float angle = (float) -(2 * Math.PI * i / n);
                points.add(new Point(center.x + r * (float) Math.cos(angle), center.y + r * (float) Math.sin(angle), z));
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

        public List<Point> genEndCapTriangleStrip(Vector v, boolean isStart) {
            List<Point> points = genEndCapVertices(v);
            List<Point> rv = new ArrayList<>();
            int n = points.size();
            for (int i = 0; i <= n / 2; i++) {
                rv.add(points.get(i));
                rv.add(points.get(n - 1 - i));
            }

            if (isStart) {
                Collections.reverse(rv);
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

            Point[] currAndOpposite = getCurrentAndOppositeVertices(curActivePointIdx);
            return new Pair<>(List.of(currAndOpposite[0]), List.of(currAndOpposite[1]));
        }
    }

    public static class Stroke {
        private final List<Point> points;

        public Stroke() {
            this(new ArrayList<>());
        }

        public Stroke(List<Point> points) {
            this.points = points;
        }

        public TriangleStrip toTriangleStrip() {
            return new TriangleStrip(new ArrayList<>(this.points));
        }

        public List<Point> toOrderedPoints() {
            List<Point> firstHalf = new ArrayList<>();
            List<Point> secondHalf = new ArrayList<>();

            for (int i = 0; i < points.size(); i += 2) {
                firstHalf.add(points.get(i));
                secondHalf.add(points.get(i + 1));
            }

            List<Point> points = new ArrayList<>(firstHalf);
            Collections.reverse(secondHalf);
            points.addAll(secondHalf);

            return points;
        }
    }

    public static Polygon removeRearHoles(Polygon p) {
        int i = 0;
        // Copy the list so that we can modify it
        List<Point> points = new ArrayList<>(p.points);
        points.remove(points.size() - 1);
        while (i < points.size()) {
            Point p1 = points.get(i);
            Point p2 = points.get((i + 1) % points.size());

            int n = points.size();
            for (int j = (i - 1 + n) % n; j != (i + 2) % n; j = (j - 1 + n) % n) {
                Point p3 = points.get((j - 1 + n) % n);
                Point p4 = points.get(j);
                Point intersection = Line.getIntersection(p1, p2, p3, p4);
                if (intersection != null) {
                    // Remove points from i + 1 to j - 1
                    for (int k = (i + 1) % n; k != j; k = (k + 1) % n) {
                        points.remove((i + 1) % points.size());
                    }
                    points.add((i + 1) % points.size(), intersection);
                    break;
                }
            }

            i++;
        }
        points.add(points.get(0));

        return new Polygon(points);
    }

    private static Polygon generateStroke(LineStrip line, PolygonalBrush brush) {
        List<Point> vertices = new ArrayList<>();
        List<Point> head = brush.genEndCapVertices(new Vector(line.points.get(0), line.points.get(1)));
        List<Point> firstHalf = new ArrayList<>();
        List<Point> secondHalf = new ArrayList<>();

        for (int i = 1; i < line.points.size() - 1; i++) {
            brush.relocate(line.points.get(i));
            Vector v = new Vector(line.points.get(i), line.points.get(i + 1));
            Pair<List<Point>, List<Point>> tangentVertices = brush.genTangentVertices(v);
            Collections.reverse(tangentVertices.second);
            firstHalf.addAll(tangentVertices.first);
            secondHalf.addAll(0, tangentVertices.second);
        }

        brush.relocate(line.points.get(line.points.size() - 1));
        List<Point> tail = brush.genEndCapVertices(new Vector(line.points.get(line.points.size() - 1), line.points.get(line.points.size() - 2)));

        vertices.addAll(head);
        vertices.addAll(firstHalf);
        vertices.addAll(tail);
        vertices.addAll(secondHalf);
        vertices.add(vertices.get(0));

        Polygon rv = new Polygon(vertices);
        rv = removeRearHoles(rv);

        if (DEBUG) {
            for (Point p : rv.points) {
                System.out.println("(" + p.x + ", " + p.y + ")");
            }
            DEBUG = false;
        }

        return rv;
    }

    private static Stroke generateStrokeT(LineStrip line, PolygonalBrush brush) {
        Stroke rv = new Stroke();
        List<Point> head = brush.genEndCapTriangleStrip(new Vector(line.points.get(0), line.points.get(1)), true);

        if (line.isClosed()) {
            Point first = head.get(head.size() - 2);
            Point second = head.get(head.size() - 1);
            rv.points.add(0, first);
            rv.points.add(1, second);
        } else {
            rv.points.addAll(0, head);
        }

        for (int i = 1; i < line.points.size() - 1; i++) {
            brush.relocate(line.points.get(i));
            Vector v = new Vector(line.points.get(i), line.points.get(i + 1));
            Pair<List<Point>, List<Point>> tangentVertices = brush.genTangentVertices(v);
            for (int j = 0; j < tangentVertices.first.size(); j++) {
                rv.points.add(tangentVertices.first.get(j));
                rv.points.add(tangentVertices.second.get(j));
            }
        }

        brush.relocate(line.points.get(line.points.size() - 1));
        List<Point> tail = brush.genEndCapTriangleStrip(new Vector(line.points.get(line.points.size() - 1), line.points.get(line.points.size() - 2)), false);

        if (line.isClosed()) {
            Point first = head.get(head.size() - 2);
            Point second = head.get(head.size() - 1);
            rv.points.add(first);
            rv.points.add(second);
        } else {
            rv.points.addAll(tail);
        }

        return rv;
    }

    public static Polygon generateStroke(LineStrip line, int n, float r) {
        return generateStroke(line, new PolygonalBrush(n, r, line.points.get(0)));
    }

    public static Stroke generateStrokeT(LineStrip line, int n, float r) {
        return generateStrokeT(line, new PolygonalBrush(n, r, line.points.get(0)));
    }

    public static List<Point> strokeToOrderedPoints(TriangleStrip stroke) {
        List<Point> firstHalf = new ArrayList<>();
        List<Point> secondHalf = new ArrayList<>();
        for (int i = 0; i < stroke.points.size(); i += 2) {
            firstHalf.add(stroke.points.get(i));
            secondHalf.add(stroke.points.get(i + 1));
        }

        List<Point> points = new ArrayList<>(firstHalf);
        Collections.reverse(secondHalf);
        points.addAll(secondHalf);
        return points;
    }

    public static TriangleStrip generateBorder(LineStrip line, int n, float r) {
        return generateStrokeT(line, new PolygonalBrush(n, r, line.points.get(0), Z_BORDER)).toTriangleStrip();
    }

    public static TriangleStrip generateBorder(Polygon polygon, int n, float r) {
        TriangleStrip rv = generateBorder(new LineStrip(polygon.points), n, r);

        for (Polygon hole : polygon.holes) {
            rv.extend(generateBorder(new LineStrip(hole.points), n, r));
        }

        return rv;
    }

    public static TriangleStrip generateBorderFromStroke(Stroke stroke, int n, float r) {
        LineStrip line = new LineStrip(stroke.toOrderedPoints());
        return generateBorder(line, n, r);
    }

    public static void test() {
    }
}
