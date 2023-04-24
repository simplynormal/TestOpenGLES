package com.hcmut.test;

import android.annotation.SuppressLint;

import com.hcmut.test.algorithm.CoordinateTransform;
import com.hcmut.test.geometry.LineStrip;
import com.hcmut.test.geometry.Point;
import com.hcmut.test.geometry.Vector;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.operation.buffer.OffsetCurveBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressLint("NewApi")
public class Test {
    public static LineStrip offsetLine(LineStrip lineStrip, float offset) {
        GeometryFactory geometryFactory = new GeometryFactory();

        // Create a JTS LineString from the input points
        Coordinate[] coordinates = lineStrip.points.stream()
                .map(p -> new Coordinate(p.x, p.y))
                .toArray(Coordinate[]::new);
        LineString lineString = geometryFactory.createLineString(coordinates);

        // Offset the line using JTS OffsetCurveBuilder
        BufferParameters bufferParameters = new BufferParameters();
        bufferParameters.setEndCapStyle(BufferParameters.CAP_ROUND);
        OffsetCurveBuilder offsetCurveBuilder = new OffsetCurveBuilder(geometryFactory.getPrecisionModel(), bufferParameters);
        Coordinate[] offsetCoordinates = offsetCurveBuilder.getOffsetCurve(coordinates, offset);

        // Convert the offset coordinates back to a list of Point objects
        List<Point> offsetPoints = new ArrayList<>();
        for (Coordinate offsetCoordinate : offsetCoordinates) {
            offsetPoints.add(new Point((float) offsetCoordinate.x, (float) offsetCoordinate.y));
        }

        return new LineStrip(offsetPoints);
    }
    public static List<Point> transformLine(List<Point> line, float offset) {
        List<Point> transformedPoints = new ArrayList<>();

        int n = line.size();

        for (int i = 0; i < n - 1; i++) {
            Point p1 = line.get(i);
            Point p2 = line.get(i + 1);

            // Calculate the direction vector
//            Point direction = new Point(p2.x - p1.x, p2.y - p1.y);
            Vector direction = new Vector(p1, p2);

            // Normalize the direction vector (unit vector)
            Vector unitVector = direction.normalize();

            // Calculate the perpendicular vector
            Vector perpVector = unitVector.orthogonal2d();

            // Scale the perpendicular vector by the offset value
            Point offsetVector = new Point(perpVector.x * offset, perpVector.y * offset);

            // Calculate the new points
            Point newP1 = new Point(p1.x + offsetVector.x, p1.y + offsetVector.y);
            Point newP2 = new Point(p2.x + offsetVector.x, p2.y + offsetVector.y);

            // Append the new points to the transformed points list
            if (i == 0) {
                transformedPoints.add(newP1); // Add the first transformed point
            }
            transformedPoints.add(newP2);
        }

        return transformedPoints;
    }

    public static void test() {
    }
}
