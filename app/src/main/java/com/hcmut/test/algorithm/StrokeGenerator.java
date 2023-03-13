package com.hcmut.test.algorithm;

import static com.menecats.polybool.helpers.PolyBoolHelper.epsilon;

import android.util.Pair;

import com.hcmut.test.geometry.Line;
import com.hcmut.test.geometry.LineStrip;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.Polygon;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.geometry.Vector;
import com.hcmut.test.geometry.equation.LineEquation;
import com.menecats.polybool.Epsilon;

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

    private static Pair<List<Point>, List<Point>> validateIntersection(List<Point> lastFirstTangentVertices1, List<Point> lastFirstTangentVertices2, List<Point> lastSecondTangentVertices1, List<Point> lastSecondTangentVertices2, Pair<List<Point>, List<Point>> tangentVertices) {
        Line firstLine1 = new Line(lastFirstTangentVertices2.get(lastFirstTangentVertices2.size() - 1), lastFirstTangentVertices1.get(0));
        Line firstLine2 = new Line(lastFirstTangentVertices1.get(lastFirstTangentVertices1.size() - 1), tangentVertices.first.get(0));
        Line secondLine1 = new Line(lastSecondTangentVertices2.get(lastSecondTangentVertices2.size() - 1), lastSecondTangentVertices1.get(0));
        Line secondLine2 = new Line(lastSecondTangentVertices1.get(lastSecondTangentVertices1.size() - 1), tangentVertices.second.get(0));

        Point firstIntersection = firstLine1.intersect(firstLine2);
        Point secondIntersection = secondLine1.intersect(secondLine2);

        boolean isFirstIntersectionAtEnd = firstIntersection != null && (firstIntersection.equals(firstLine1.p2) || firstIntersection.equals(firstLine2.p2));
        boolean isSecondIntersectionAtEnd = secondIntersection != null && (secondIntersection.equals(secondLine1.p2) || secondIntersection.equals(secondLine2.p2));

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

    public static Polygon removeRearHoles(Polygon p) {
        int i = 0;
        // Copy the list so that we can modify it
        List<Point> points = new ArrayList<>(p.points);
        points.remove(points.size() - 1);
        while (i < points.size()) {
            Point p1 = points.get(i);
            Point p2 = points.get((i + 1) % points.size());

            int n = points.size();
            for (int j = (i + 2) % n; j != (i - 1 + n) % n; j = (j + 1) % n) {
                Point p3 = points.get(j % n);
                Point p4 = points.get((j + 1) % n);
                Point intersection = Line.getIntersection(p1, p2, p3, p4);
                if (intersection != null) {
                    List<Point> pointsFromJToI = new ArrayList<>();
                    if (j < i) {
                        pointsFromJToI.addAll(points.subList(j + 1, i));
                    } else {
                        pointsFromJToI.addAll(points.subList(j + 1, n));
                        pointsFromJToI.addAll(points.subList(0, i + 1));
                    }
                    pointsFromJToI.add(pointsFromJToI.get(0));
                    Polygon polyFromJToI = new Polygon(pointsFromJToI);
                    boolean isJIInside = polyFromJToI.doesContain(p3);

                    if (!isJIInside) {
                        // Remove points from j + 1 to i
                        for (int k = 0; k < ((i - j + n) % n); k++) {
                            points.remove((j + 1) % points.size());
                        }
                        points.add((j + 1) % points.size(), intersection);
                    } else {
                        // Remove points from i + 1 to j
                        for (int k = 0; k < ((j - i + n) % n); k++) {
                            points.remove((i + 1) % points.size());
                        }
                        points.add((i + 1) % points.size(), intersection);
                    }
                    break;
                }
            }

            i++;
        }
        points.add(points.get(0));

        return new Polygon(points);
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
