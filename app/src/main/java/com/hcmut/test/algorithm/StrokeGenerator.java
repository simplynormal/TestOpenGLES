package com.hcmut.test.algorithm;


import android.util.Pair;

import com.hcmut.test.geometry.Line;
import com.hcmut.test.geometry.LineStrip;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.Polygon;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.geometry.Vector;
import com.hcmut.test.geometry.equation.LineEquation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StrokeGenerator {
    private static boolean DEBUG = false;

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

    public static Polygon generateStroke(LineStrip line, int n, float r) {
        return generateStroke(line, new PolygonalBrush(n, r, line.points.get(0)));
    }

    public static void test() {
    }
}
