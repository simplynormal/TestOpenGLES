/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/
package com.hcmut.test.object;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;

import android.opengl.GLES20;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.data.VertexData;
import com.hcmut.test.data.Way;
import com.hcmut.test.geometry.equation.LineEquation;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.Triangle;
import com.hcmut.test.geometry.TriangleStrip;
import com.hcmut.test.geometry.Vector;
import com.hcmut.test.programs.ColorShaderProgram;

import com.hcmut.test.geometry.Polygon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ObjectBuilder {
    //    private float[] waysVertexData = new float[0];
//    private float[] linesVertexData = new float[0];
//    private float[] borderVertexData = new float[0];
    private final float[] testVertexData = new float[]{
            -0f, 1.5f, 0f,
            -1f, 1f, 0f,
            -2f, 0f, 0f,
            -3f, 1f, 0f,
            -4f, 0f, 0f,
            -5f, -5f, 0f,

    };

    List<Triangle> triangles = new ArrayList<>();
    TriangleStrip borderTriangleStrip = new TriangleStrip();
    TriangleStrip lineTriangleStrip = new TriangleStrip();

    private static boolean isDebug = false;

    public ObjectBuilder() {
        VertexData.resetRandom();
    }

    public void addWay(Way way, float originX, float originY, float scale) {
//        System.out.println("add way" + way);
        if (way.isClosed()) {
            addClosedWay(way, originX, originY, scale);
        } else {
            addOpenWay2(way, originX, originY, scale);
        }

    }

    private void addClosedWay(Way way, float originX, float originY, float scale) {
        Polygon polygon = new Polygon(way.toPoints(originX, originY, scale));
        List<Triangle> curTriangulatedTriangles = polygon.triangulate();
        triangles.addAll(curTriangulatedTriangles);
        TriangleStrip newWay = genBorderFromPolygon(polygon, curTriangulatedTriangles, 0.002f);
        borderTriangleStrip.extend(newWay);
    }

    public void addOpenWay(Way way, float originX, float originY, float scale) {
        List<Point> linePoints = way.toPoints(originX, originY, scale);
        TriangleStrip newWay = findBorderPointLine(linePoints, 0.02f);
        if (isDebug) {
            for (Point p : newWay.points) {
                System.out.println(p);
            }
        }
        lineTriangleStrip.extend(newWay);
    }

    public void addOpenWay2(Way way, float originX, float originY, float scale) {
        List<Point> linePoints = way.toPoints(originX, originY, scale);
        Polygon polygon = genPolylineFromLine(linePoints, 0.02f);
        List<Triangle> curTriangulatedTriangles = polygon.triangulate();
        triangles.addAll(curTriangulatedTriangles);
        TriangleStrip newWay = genBorderFromPolygon(polygon, curTriangulatedTriangles, 0.002f);
        borderTriangleStrip.extend(newWay);
    }

    public Point findAngleBorderPoint(Point pointA, Point midPoint, Point pointB, float d, boolean isReflexAngle) {
        Vector vectorMidA = new Vector(midPoint, pointA);
        Vector vectorMidB = new Vector(midPoint, pointB);
        Vector sumVector = vectorMidA.add(vectorMidB);

        Vector orthoVectorMidA = vectorMidA.orthogonal();
        Vector orthoVectorMidB = vectorMidB.orthogonal();

        float angleSumA = sumVector.angle(orthoVectorMidA);
        float angleSumB = sumVector.angle(orthoVectorMidB);

        boolean isOnSameLine = new LineEquation(pointB, pointA).hasPoint(midPoint);

        if (!isOnSameLine) {
            if ((isReflexAngle && angleSumA < Math.PI / 2) || (!isReflexAngle && angleSumA > Math.PI / 2)) {
                orthoVectorMidA = orthoVectorMidA.negate();
            }
            if ((isReflexAngle && angleSumB < Math.PI / 2) || (!isReflexAngle && angleSumB > Math.PI / 2)) {
                orthoVectorMidB = orthoVectorMidB.negate();
            }
        }

        Vector scaledOrthoVectorMidA = orthoVectorMidA.mul(d / orthoVectorMidA.length());
        Vector scaledOrthoVectorMidB = orthoVectorMidB.mul(d / orthoVectorMidB.length());

        Point point1 = midPoint.add(scaledOrthoVectorMidA);
        Point point2 = midPoint.add(scaledOrthoVectorMidB);

        LineEquation line1 = new LineEquation(vectorMidA, point1, false);
        LineEquation line2 = new LineEquation(vectorMidB, point2, false);

        Point rv = LineEquation.intersect(line1, line2);
        if (rv == null) {
            return isReflexAngle ? point2 : point1;
        }
        return rv;
    }

    private Point[] findBorderPoint(Point pointA, Point midPoint, Point pointB, float d, float angle) {
        Point rv1 = findAngleBorderPoint(pointA, midPoint, pointB, d, true);
        Point rv2 = findAngleBorderPoint(pointA, midPoint, pointB, d, false);

        if (angle < Math.PI) {
            return new Point[]{rv2, rv1};
        }

        return new Point[]{rv1, rv2};
    }

    private TriangleStrip genBorderFromPolygon(Polygon polygon, List<Triangle> triangulated, float width) {
        float[] angles = new float[polygon.points.size()];

        for (Triangle triangle : triangulated) {
            int indexA = polygon.points.indexOf(triangle.p1);
            int indexB = polygon.points.indexOf(triangle.p2);
            int indexC = polygon.points.indexOf(triangle.p3);

            angles[indexA] += triangle.getAngleA();
            angles[indexB] += triangle.getAngleB();
            angles[indexC] += triangle.getAngleC();
        }

        List<Point> points = polygon.points.subList(0, polygon.points.size() - 1);
        TriangleStrip rv = new TriangleStrip();

        for (int i = 0; i < points.size(); i++) {
            Point pointA = points.get(i);
            Point midPoint = points.get((i + 1) % points.size());
            Point pointB = points.get((i + 2) % points.size());
            float angle = angles[(i + 1) % points.size()];

            Point[] border = findBorderPoint(pointA, midPoint, pointB, width, angle);
            rv.add(border);

//            if (isDebug) {
//                System.out.println("----------------------------------------");
//                System.out.println("Angle: " + angle * 180 / Math.PI);
//                System.out.println("Point A: " + pointA);
//                System.out.println("Point B: " + pointB);
//                System.out.println("Mid Point: " + midPoint);
//                System.out.println("Border 1: " + border[0]);
//                System.out.println("Border 2: " + border[1]);
//                System.out.println("----------------------------------------");
//            }
        }

        rv.add(rv.points.subList(0, 2));

        return rv;
    }

    private Point[] findOrthBorderPoint(Point firstPoint, Point secondPoint, float width) {
        Vector vectorFirstSecond = new Vector(firstPoint, secondPoint);
        Vector vectorOrthogonal = vectorFirstSecond.orthogonal();
        Vector vectorScaledOrthogonal = vectorOrthogonal.mul(width / vectorOrthogonal.length());
        Vector vectorScaledOppositeOrthogonal = vectorScaledOrthogonal.negate();

        return new Point[]{firstPoint.add(vectorScaledOrthogonal), firstPoint.add(vectorScaledOppositeOrthogonal)};
    }

    private TriangleStrip findBorderPointLine(List<Point> linePoints, float width) {
        TriangleStrip rv = new TriangleStrip();

        Point[] firstBorder = findOrthBorderPoint(linePoints.get(0), linePoints.get(1), width);
        Vector vectorFirstBorder = new Vector(firstBorder[1], firstBorder[0]);
        rv.add(firstBorder);

        int oldAngle = 100;
        boolean isSameSide = true;
        for (int i = 0; i < linePoints.size() - 2; i += 1) {
            Point pointA = linePoints.get(i);
            Point midPoint = linePoints.get(i + 1);
            Point pointB = linePoints.get(i + 2);

            if (i == 0) {
                Vector vectorMidB = new Vector(midPoint, pointB);
                boolean angleFirst = vectorFirstBorder.angle(vectorMidB) > Math.PI / 2;
                oldAngle = oldAngle * (angleFirst ? 1 : -1);
            } else {
                Point prevPoint = linePoints.get(i - 1);
                // line between midPoint and pointA
                LineEquation lineMidA = new LineEquation(midPoint, pointA);

                // check if prevPoint is on the same side as pointB through line between midPoint and pointA
                if (lineMidA.hasPoint(prevPoint) || lineMidA.hasPoint(pointB)) {
                    isSameSide = !isSameSide;
                } else {
                    isSameSide = lineMidA.isSameSide(prevPoint, pointB);
                }
            }

            int newAngle = oldAngle * (isSameSide ? 1 : -1);
            oldAngle = newAngle;
            Point[] border = findBorderPoint(pointA, midPoint, pointB, width, newAngle);
            rv.add(border);
        }

        Point[] lastBorder = findOrthBorderPoint(linePoints.get(linePoints.size() - 1), linePoints.get(linePoints.size() - 2), width);
        lastBorder = new Point[]{lastBorder[1], lastBorder[0]};
        rv.add(lastBorder);

        return rv;
    }

    private Polygon genPolylineFromLine(List<Point> linePoints, float width) {
        Point[] firstBorder = findOrthBorderPoint(linePoints.get(0), linePoints.get(1), width);
        Vector vectorFirstBorder = new Vector(firstBorder[1], firstBorder[0]);
        List<Point> rv = new ArrayList<>(Arrays.asList(firstBorder));

        int oldAngle = 100;
        boolean isSameSide = true;
        for (int i = 0; i < linePoints.size() - 2; i += 1) {
            Point pointA = linePoints.get(i);
            Point midPoint = linePoints.get(i + 1);
            Point pointB = linePoints.get(i + 2);

            if (i == 0) {
                Vector vectorMidB = new Vector(midPoint, pointB);
                boolean angleFirst = vectorFirstBorder.angle(vectorMidB) > Math.PI / 2;
                oldAngle = oldAngle * (angleFirst ? 1 : -1);
            } else {
                Point prevPoint = linePoints.get(i - 1);
                // line between midPoint and pointA
                LineEquation lineMidA = new LineEquation(midPoint, pointA);

                // check if prevPoint is on the same side as pointB through line between midPoint and pointA
                if (lineMidA.hasPoint(prevPoint) || lineMidA.hasPoint(pointB)) {
                    isSameSide = !isSameSide;
                } else {
                    isSameSide = lineMidA.isSameSide(prevPoint, pointB);
                }
            }

            int newAngle = oldAngle * (isSameSide ? 1 : -1);
            oldAngle = newAngle;
            Point[] border = findBorderPoint(pointA, midPoint, pointB, width, newAngle);
            rv.addAll(Arrays.asList(border));
        }

        Point[] lastBorder = findOrthBorderPoint(linePoints.get(linePoints.size() - 1), linePoints.get(linePoints.size() - 2), width);
        lastBorder = new Point[]{lastBorder[1], lastBorder[0]};
        rv.addAll(Arrays.asList(lastBorder));

//        System.out.println("rv: " + rv);

        List<Point> firstHalf = new ArrayList<>();
        List<Point> secondHalf = new ArrayList<>();

        for (int i = 0; i < rv.size(); i++) {
            if (i % 2 == 0) {
                firstHalf.add(rv.get(i));
            } else {
                secondHalf.add(rv.get(i));
            }
        }

        Collections.reverse(secondHalf);
        firstHalf.addAll(secondHalf);
        firstHalf.add(firstHalf.get(0));

//        System.out.println("firstHalf: " + firstHalf);

        return new Polygon(firstHalf);
    }

    public void draw(ColorShaderProgram colorProgram, float[] projectionMatrix) {
        float[] waysVertexData = triangles.size() > 0 ? new float[triangles.get(0).toVertexData().length * triangles.size()] : new float[0];
        for (int i = 0; i < triangles.size(); i++) {
            System.arraycopy(triangles.get(i).toVertexData(), 0, waysVertexData, i * triangles.get(i).toVertexData().length, triangles.get(i).toVertexData().length);
        }
        float[] linesVertexData = lineTriangleStrip.toVertexData();
        float[] borderVertexData = borderTriangleStrip.toVertexData();

        float[] vertexData = new float[borderVertexData.length + waysVertexData.length + linesVertexData.length + testVertexData.length];
        System.arraycopy(borderVertexData, 0, vertexData, 0, borderVertexData.length);
        System.arraycopy(waysVertexData, 0, vertexData, borderVertexData.length, waysVertexData.length);
        System.arraycopy(linesVertexData, 0, vertexData, borderVertexData.length + waysVertexData.length, linesVertexData.length);
        System.arraycopy(testVertexData, 0, vertexData, borderVertexData.length + waysVertexData.length + linesVertexData.length, testVertexData.length);

        int uMVPLocation = colorProgram.getUniformLocation(ColorShaderProgram.U_MATRIX);

        VertexArray vertexArray = new VertexArray(vertexData);

        int strideInElements = VertexData.getTotalComponentCount();
        for (VertexData.VertexAttrib attrib : VertexData.getVertexAttribs()) {
            int location = colorProgram.getAttributeLocation(attrib.name);
            vertexArray.setVertexAttribPointer(attrib.offset, location, attrib.count, strideInElements);
        }

        colorProgram.useProgram();
        GLES20.glUniformMatrix4fv(uMVPLocation, 1, false, projectionMatrix, 0);
//        GLES20.glUniform4f(uColorLocation, 0.57f, 0.76f, 0.88f, 1f);
        glDrawArrays(GL_TRIANGLES, borderVertexData.length / strideInElements, waysVertexData.length / strideInElements);


//        GLES20.glUniform4f(uColorLocation, 0f, 0f, 0f, 1f);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, borderVertexData.length / strideInElements);

//        GLES20.glUniform4f(uColorLocation, 0.7f, 0.7f, 0.7f, 1f);
        glDrawArrays(GL_TRIANGLE_STRIP, (borderVertexData.length + waysVertexData.length) / strideInElements, linesVertexData.length / strideInElements);


//        colorProgram.setUniformColor(0.8f, 0.8f, 0.8f);
//        glDrawArrays(GL_TRIANGLE_STRIP, (borderVertexData.length + waysVertexData.length) / 3, testVertexData.length / 3);

//        colorProgram.setUniformColor(0f, 0f, 1f);
//        glDrawArrays(GL_LINES, 0, borderVertexData.length / 3);

//        colorProgram.setUniformColor(1f, 1f, 1f);
//        int test = 38;
//        glDrawArrays(GL_TRIANGLE_STRIP, test, 4);

        isDebug = false;
    }
}
