package com.hcmut.test.algorithm;

import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.Polygon;
import com.hcmut.test.geometry.Triangle;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.geometry.Vector;
import com.hcmut.test.geometry.equation.LineEquation;

import java.util.ArrayList;
import java.util.List;

public class BorderGenerator {
    private static Point findAngleBorderPoint(Point pointA, Point midPoint, Point pointB, float d, boolean isReflexAngle) {
        Vector vectorMidA = new Vector(midPoint, pointA);
        Vector vectorMidB = new Vector(midPoint, pointB);
        Vector sumVector = vectorMidA.add(vectorMidB);

        Vector orthoVectorMidA = vectorMidA.orthogonal();
        Vector orthoVectorMidB = vectorMidB.orthogonal();

        float angleSumA = sumVector.angle(orthoVectorMidA);
        float angleSumB = sumVector.angle(orthoVectorMidB);

        if ((isReflexAngle && angleSumA < Math.PI / 2) || (!isReflexAngle && angleSumA > Math.PI / 2)) {
            orthoVectorMidA = orthoVectorMidA.negate();
        }
        if ((isReflexAngle && angleSumB < Math.PI / 2) || (!isReflexAngle && angleSumB > Math.PI / 2)) {
            orthoVectorMidB = orthoVectorMidB.negate();
        }

        Vector scaledOrthoVectorMidA = orthoVectorMidA.mul(d / orthoVectorMidA.length());
        Vector scaledOrthoVectorMidB = orthoVectorMidB.mul(d / orthoVectorMidB.length());

        Point point1 = midPoint.add(scaledOrthoVectorMidA);
        Point point2 = midPoint.add(scaledOrthoVectorMidB);

        LineEquation line1 = new LineEquation(vectorMidA, point1, false);
        LineEquation line2 = new LineEquation(vectorMidB, point2, false);

        Point rv = LineEquation.intersect(line1, line2);
        if (rv == null) {
            System.out.println("========================null=======>>>>>>>>>>>>>>>>>>");
            return isReflexAngle ? point2 : point1;
        }
        return rv;
    }

    private static Point[] findBorderPoint(Point pointA, Point midPoint, Point pointB, float d, float angle) {
        Point rv1 = findAngleBorderPoint(pointA, midPoint, pointB, d, true);
        Point rv2 = findAngleBorderPoint(pointA, midPoint, pointB, d, false);

        if (angle < Math.PI) {
            return new Point[]{rv2, rv1};
        }

        return new Point[]{rv1, rv2};
    }

    private static TriangleStrip genBorder(Polygon polygon, float width, float[] angles, boolean isHole) {
        List<Point> points = new ArrayList<>(polygon.points.subList(0, polygon.points.size() - 1));
        List<Point> rvPoints = new ArrayList<>();

        for (int i = 0; i < points.size(); i++) {
            Point pointA = points.get(i);
            Point midPoint = points.get((i + 1) % points.size());
            Point pointB = points.get((i + 2) % points.size());
            float angle = angles[(i + 1) % points.size()];
            if (isHole) angle = 2 * (float) Math.PI - angle;

            Point[] border = findBorderPoint(pointA, midPoint, pointB, width, angle);
            rvPoints.addAll(List.of(border));

//            System.out.println("----------------------------------------");
//            System.out.println("Angle: " + angle * 180 / Math.PI);
//            System.out.println("Point A: " + pointA);
//            System.out.println("Mid Point: " + midPoint);
//            System.out.println("Point B: " + pointB);
//            System.out.println("Border 1: " + border[0]);
//            System.out.println("Border 2: " + border[1]);
//            System.out.println("----------------------------------------");
        }
        rvPoints.addAll(rvPoints.subList(0, 2));

        return new TriangleStrip(rvPoints);
    }

    public static TriangleStrip generateBorderFromPolygon(Polygon polygon, List<Triangle> triangulated, float width) {
        float[] angles = new float[polygon.points.size()];
        List<float[]> holeAngles = new ArrayList<>();

        for (Triangle triangle : triangulated) {
            int indexA = polygon.points.indexOf(triangle.p1);
            int indexB = polygon.points.indexOf(triangle.p2);
            int indexC = polygon.points.indexOf(triangle.p3);

            if (indexA != -1)
                angles[indexA] += triangle.getAngleA();
            if (indexB != -1)
                angles[indexB] += triangle.getAngleB();
            if (indexC != -1)
                angles[indexC] += triangle.getAngleC();

            for (Polygon hole : polygon.holes) {
                if (indexA == -1 && indexB == -1 && indexC == -1) break;
                float[] curHoleAngles = new float[hole.points.size()];
                if (indexA == -1) {
                    int holeIndexA = hole.points.indexOf(triangle.p1);
                    if (holeIndexA != -1) {
                        curHoleAngles[holeIndexA] += triangle.getAngleA();
                    }
                }
                if (indexB == -1) {
                    int holeIndexB = hole.points.indexOf(triangle.p2);
                    if (holeIndexB != -1) {
                        curHoleAngles[holeIndexB] += triangle.getAngleB();
                    }
                }
                if (indexC == -1) {
                    int holeIndexC = hole.points.indexOf(triangle.p3);
                    if (holeIndexC != -1) {
                        curHoleAngles[holeIndexC] += triangle.getAngleC();
                    }
                }
                holeAngles.add(curHoleAngles);
            }
        }

        TriangleStrip rv = genBorder(polygon, width, angles, false);

        for (Polygon hole : polygon.holes) {
            TriangleStrip holeRv = genBorder(hole, width, holeAngles.get(polygon.holes.indexOf(hole)), true);
            rv.extend(holeRv);
        }

        return rv;
    }
}
