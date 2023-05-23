package com.hcmut.test.algorithm;

import android.location.Location;

import java.util.List;

public class Navigation {
    public static boolean isLocationNearPolyline(Location location, List<Location> polyline, double bufferDistanceInMeters) {
        for (int i = 0; i < polyline.size() - 1; i++) {
            Location pathStart = polyline.get(i);
            Location pathEnd = polyline.get(i + 1);

            // Check if the location is near this segment of the polyline
            if (isLocationNearPath(location, pathStart, pathEnd, bufferDistanceInMeters)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLocationNearPath(Location location, Location pathStart, Location pathEnd, double bufferDistanceInMeters) {
        // Calculate the closest point on the path to the location
        double[] closestPoint = calculateClosestPoint(location, pathStart, pathEnd);

        // Create a Location object from the closest point
        Location closestLocation = new Location("");
        closestLocation.setLatitude(closestPoint[0]);
        closestLocation.setLongitude(closestPoint[1]);

        // Check if the distance from the location to the closest point is within the buffer distance
        return location.distanceTo(closestLocation) <= bufferDistanceInMeters;
    }

    public static double[] calculateClosestPoint(Location point, Location lineStart, Location lineEnd) {
        double xDelta = lineEnd.getLongitude() - lineStart.getLongitude();
        double yDelta = lineEnd.getLatitude() - lineStart.getLatitude();

        if ((xDelta == 0) && (yDelta == 0)) {
            throw new IllegalArgumentException("Line start and end points are the same");
        }

        double u = ((point.getLongitude() - lineStart.getLongitude()) * xDelta + (point.getLatitude() - lineStart.getLatitude()) * yDelta) / (xDelta * xDelta + yDelta * yDelta);

        final double[] closestPoint;
        if (u < 0) {
            closestPoint = new double[]{lineStart.getLatitude(), lineStart.getLongitude()};
        } else if (u > 1) {
            closestPoint = new double[]{lineEnd.getLatitude(), lineEnd.getLongitude()};
        } else {
            closestPoint = new double[]{lineStart.getLatitude() + u * yDelta, lineStart.getLongitude() + u * xDelta};
        }

        return closestPoint;
    }
}
