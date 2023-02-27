package com.hcmut.test.utils;

import com.hcmut.test.geometry.LineStrip;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.geometry.Vector;

import java.util.ArrayList;
import java.util.List;

public class StrokeGenerator {
    private static class PolygonalBrush {
        public final List<Point> points;
        private final Point center;

        public PolygonalBrush(int n, float r, Point center) {
            assert n % 2 == 0 && n >= 4 : "n must be even and >= 4";
            points = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                float angle = (float) (2 * Math.PI * i / n);
                points.add(new Point(center.x + r * (float) Math.cos(angle), center.y + r * (float) Math.sin(angle)));
            }
            this.center = center;
        }


        public PolygonalBrush translate(Vector v) {
            PolygonalBrush rv = new PolygonalBrush(points.size(), 0, center.add(v));
            for (Point p : points) {
                rv.points.add(p.add(v));
            }
            return rv;
        }

        public PolygonalBrush translate(float dx, float dy) {
            return translate(new Vector(dx, dy));
        }

        public PolygonalBrush relocate(Point p) {
            return translate(new Vector(center, p));
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

                    if (inTangentRange == 1) {
                        for (int j = i; j != (i + n / 2) % n; j = (j + 1) % n) {
                            rv.add(points.get(j));
                        }
                        rv.add(points.get((i + n / 2) % n));
                    } else {
                        for (int j = (i + n / 2) % n; j != i; j = (j + 1) % n) {
                            rv.add(points.get(j));
                        }
                        rv.add(points.get(i));
                    }
                }
            }

            return rv;
        }
    }

//    public static TriangleStrip generateStroke(LineStrip line, int n, float r) {
//        return generateStroke(line, new PolygonalBrush(n, r, line.points.get(0)));
//    }
//
//    private static TriangleStrip generateStroke(LineStrip line, PolygonalBrush brush) {
//    }

    public static void test() {
        PolygonalBrush brush = new PolygonalBrush(4, 1, new Point(0, 0));
        System.out.println(brush.genEndCapVertices(new Vector(1, 0)));
        System.out.println(brush.genEndCapVertices(new Vector(0, 1)));
        System.out.println(brush.genEndCapVertices(new Vector(-1, 0)));
        System.out.println(brush.genEndCapVertices(new Vector(0, -1)));
    }
}
