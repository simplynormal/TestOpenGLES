/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/
package com.hcmut.test.object;

import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;

import com.hcmut.test.data.Node;
import com.hcmut.test.data.VertexArray;
import com.hcmut.test.programs.ColorShaderProgram;

import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

import java.util.ArrayList;
import java.util.List;

public class ObjectBuilder {
    private float[] waysVertexData = new float[0];
    private float[] linesVertexData = new float[0];
    private float[] borderVertexData = new float[0];

    private final float[] testVertexData = new float[]{
            -0f, 1.5f, 0f,
            -1f, 1f, 0f,
            -2f, 0f, 0f,
            -3f, 1f, 0f,
            -4f, 0f, 0f,
            -5f, -5f, 0f,

    };
    private static boolean isDebug = true;

    public ObjectBuilder() {
//        addOpenWay(new Way(testVertexData), -3f, 0f, 1 / 5f);

        if (isDebug) {
            System.out.println("====================================");
            for (int i = 0; i < linesVertexData.length; i += 3) {
                System.out.println(i / 3 + ": " + linesVertexData[i] + " " + linesVertexData[i + 1] + " " + linesVertexData[i + 2]);
            }
            System.out.println("====================================");
        }
    }

    public void addWay(Way way, float originX, float originY, float scale) {
        if (way.isClosed()) {
            addClosedWay(way, originX, originY, scale);
        } else {
            addOpenWay(way, originX, originY, scale);
        }

    }

    public void addClosedWay(Way way, float originX, float originY, float scale) {
        ArrayList<PolygonPoint> points = new ArrayList<>();
        for (Node node : way.nodes) {
            points.add(new PolygonPoint(
                    (node.lon - originX) * scale,
                    (node.lat - originY) * scale)
            );
        }

        Polygon polygon = new Polygon(points);
        float[] vertices = triangulate(polygon);
        float[] angles = new float[polygon.getPoints().size()];

        for (int i = 0; i < vertices.length; i += 9) {
            float[] pointA = new float[]{vertices[i], vertices[i + 1]};
            float[] pointB = new float[]{vertices[i + 3], vertices[i + 4]};
            float[] pointC = new float[]{vertices[i + 6], vertices[i + 7]};

            float angleA = Math.abs(findAngle(new float[]{pointB[0] - pointA[0], pointB[1] - pointA[1]}, new float[]{pointC[0] - pointA[0], pointC[1] - pointA[1]}));
            float angleB = Math.abs(findAngle(new float[]{pointA[0] - pointB[0], pointA[1] - pointB[1]}, new float[]{pointC[0] - pointB[0], pointC[1] - pointB[1]}));
            float angleC = Math.abs(findAngle(new float[]{pointA[0] - pointC[0], pointA[1] - pointC[1]}, new float[]{pointB[0] - pointC[0], pointB[1] - pointC[1]}));

            int indexA = polygon.getPoints().indexOf(new PolygonPoint(pointA[0], pointA[1]));
            int indexB = polygon.getPoints().indexOf(new PolygonPoint(pointB[0], pointB[1]));
            int indexC = polygon.getPoints().indexOf(new PolygonPoint(pointC[0], pointC[1]));

            angles[indexA] += angleA;
            angles[indexB] += angleB;
            angles[indexC] += angleC;
        }

        float[] newVertexData = new float[waysVertexData.length + vertices.length];
        System.arraycopy(waysVertexData, 0, newVertexData, 0, waysVertexData.length);
        System.arraycopy(vertices, 0, newVertexData, waysVertexData.length, vertices.length);
        waysVertexData = newVertexData;
        genBorderPointPolygon(polygon, 0.002f, angles);
    }

    public void addOpenWayFromLines(Way way, float originX, float originY, float scale) {
        float[] vertices = new float[way.nodes.size() * 6];
        int index = 0;
        for (Node node : way.nodes) {
            vertices[index++] = (node.lon - originX) * scale;
            vertices[index++] = (node.lat - originY) * scale;
            vertices[index++] = 0f;
            vertices[index++] = (node.lon - originX) * scale;
            vertices[index++] = (node.lat - originY) * scale;
            vertices[index++] = 0f;
        }
        float[] newVertexData = new float[linesVertexData.length + vertices.length];
        System.arraycopy(linesVertexData, 0, newVertexData, 0, linesVertexData.length);
        System.arraycopy(vertices, 3, newVertexData, linesVertexData.length, vertices.length - 3);
        // repeat last point
        System.arraycopy(vertices, vertices.length - 3, newVertexData, linesVertexData.length + vertices.length - 3, 3);
        linesVertexData = newVertexData;
    }

    public void addOpenWay(Way way, float originX, float originY, float scale) {
        float[] vertices = new float[way.nodes.size() * 2];
        int oldLength = linesVertexData.length;
        int index = 0;
        for (Node node : way.nodes) {
            vertices[index++] = (node.lon - originX) * scale;
            vertices[index++] = (node.lat - originY) * scale;
        }
        vertices = findBorderPointLine(vertices, 0.01f);
        int additionalLength = oldLength > 0 ? 6 : 3;
        float[] newVertexData = new float[linesVertexData.length + vertices.length + additionalLength];
        System.arraycopy(linesVertexData, 0, newVertexData, 0, linesVertexData.length);
        if (oldLength > 0) {
            System.arraycopy(vertices, 0, newVertexData, linesVertexData.length, 3);
        }
        System.arraycopy(vertices, 0, newVertexData, linesVertexData.length + additionalLength - 3, vertices.length);
        System.arraycopy(vertices, vertices.length - 3, newVertexData, linesVertexData.length + vertices.length + additionalLength - 3, 3);

        linesVertexData = newVertexData;
    }

    private float[] triangulate(Polygon polygon) {
        Poly2Tri.triangulate(polygon);

        float[] vertices = new float[polygon.getTriangles().size() * 3 * 3];
        int index = 0;
        for (DelaunayTriangle triangle : polygon.getTriangles()) {
            for (TriangulationPoint point : triangle.points) {
                vertices[index++] = (float) point.getX();
                vertices[index++] = (float) point.getY();
                vertices[index++] = 0f;
            }
        }
        return vertices;
    }

    public float findAngle(float[] vectorA, float[] vectorB) {
        return (float) Math.acos((vectorA[0] * vectorB[0] + vectorA[1] * vectorB[1]) / (Math.sqrt(vectorA[0] * vectorA[0] + vectorA[1] * vectorA[1]) * Math.sqrt(vectorB[0] * vectorB[0] + vectorB[1] * vectorB[1])));
    }

    public float[] findLargeAngleBorderPoint(float[] pointA, float[] midPoint, float[] pointB, float d) {
        float[] vectorMidA = new float[]{pointA[0] - midPoint[0], pointA[1] - midPoint[1]};
        float[] vectorMidB = new float[]{pointB[0] - midPoint[0], pointB[1] - midPoint[1]};
        float[] sumVector = new float[]{vectorMidA[0] + vectorMidB[0], vectorMidA[1] + vectorMidB[1]};

        float[] orthoVectorMidA = new float[]{-vectorMidA[1], vectorMidA[0]};
        float[] orthoVectorMidB = new float[]{-vectorMidB[1], vectorMidB[0]};

        if (findAngle(orthoVectorMidA, sumVector) < Math.PI / 2) {
            orthoVectorMidA[0] = -orthoVectorMidA[0];
            orthoVectorMidA[1] = -orthoVectorMidA[1];
        }

        if (findAngle(orthoVectorMidB, sumVector) < Math.PI / 2) {
            orthoVectorMidB[0] = -orthoVectorMidB[0];
            orthoVectorMidB[1] = -orthoVectorMidB[1];
        }

        float[] scaledOrthoVectorMidA = new float[]{orthoVectorMidA[0] * d / (float) Math.sqrt(orthoVectorMidA[0] * orthoVectorMidA[0] + orthoVectorMidA[1] * orthoVectorMidA[1]), orthoVectorMidA[1] * d / (float) Math.sqrt(orthoVectorMidA[0] * orthoVectorMidA[0] + orthoVectorMidA[1] * orthoVectorMidA[1])};
        float[] scaledOrthoVectorMidB = new float[]{orthoVectorMidB[0] * d / (float) Math.sqrt(orthoVectorMidB[0] * orthoVectorMidB[0] + orthoVectorMidB[1] * orthoVectorMidB[1]), orthoVectorMidB[1] * d / (float) Math.sqrt(orthoVectorMidB[0] * orthoVectorMidB[0] + orthoVectorMidB[1] * orthoVectorMidB[1])};

        float[] point1 = new float[]{midPoint[0] + scaledOrthoVectorMidA[0], midPoint[1] + scaledOrthoVectorMidA[1]};
        float[] point2 = new float[]{midPoint[0] + scaledOrthoVectorMidB[0], midPoint[1] + scaledOrthoVectorMidB[1]};

        // line1 parallel to vectorMidA and contains point1
        float a1 = vectorMidA[1] / vectorMidA[0];
        float b1 = point1[1] - a1 * point1[0];

        // line2 parallel to vectorMidB and contains point2
        float a2 = vectorMidB[1] / vectorMidB[0];
        float b2 = point2[1] - a2 * point2[0];

        if (a1 == Double.POSITIVE_INFINITY || a1 == Double.NEGATIVE_INFINITY) {
            return new float[]{point1[0], a2 * point1[0] + b2};
        } else if (a2 == Double.POSITIVE_INFINITY || a2 == Double.NEGATIVE_INFINITY) {
            return new float[]{point2[0], a1 * point2[0] + b1};
        }

        float x = (b2 - b1) / (a1 - a2);
        float y = a1 * x + b1;
        return new float[]{x, y};
    }

    public float[] findSmallAngleBorderPoint(float[] pointA, float[] midPoint, float[] pointB, float d) {
        float[] vectorMidA = new float[]{pointA[0] - midPoint[0], pointA[1] - midPoint[1]};
        float[] vectorMidB = new float[]{pointB[0] - midPoint[0], pointB[1] - midPoint[1]};
        float[] sumVector = new float[]{vectorMidA[0] + vectorMidB[0], vectorMidA[1] + vectorMidB[1]};

        float[] orthoVectorMidA = new float[]{-vectorMidA[1], vectorMidA[0]};
        float[] orthoVectorMidB = new float[]{-vectorMidB[1], vectorMidB[0]};

        if (findAngle(orthoVectorMidA, sumVector) > Math.PI / 2) {
            orthoVectorMidA[0] = -orthoVectorMidA[0];
            orthoVectorMidA[1] = -orthoVectorMidA[1];
        }

        if (findAngle(orthoVectorMidB, sumVector) > Math.PI / 2) {
            orthoVectorMidB[0] = -orthoVectorMidB[0];
            orthoVectorMidB[1] = -orthoVectorMidB[1];
        }

        float[] scaledOrthoVectorMidA = new float[]{orthoVectorMidA[0] * d / (float) Math.sqrt(orthoVectorMidA[0] * orthoVectorMidA[0] + orthoVectorMidA[1] * orthoVectorMidA[1]), orthoVectorMidA[1] * d / (float) Math.sqrt(orthoVectorMidA[0] * orthoVectorMidA[0] + orthoVectorMidA[1] * orthoVectorMidA[1])};
        float[] scaledOrthoVectorMidB = new float[]{orthoVectorMidB[0] * d / (float) Math.sqrt(orthoVectorMidB[0] * orthoVectorMidB[0] + orthoVectorMidB[1] * orthoVectorMidB[1]), orthoVectorMidB[1] * d / (float) Math.sqrt(orthoVectorMidB[0] * orthoVectorMidB[0] + orthoVectorMidB[1] * orthoVectorMidB[1])};

        float[] point1 = new float[]{midPoint[0] + scaledOrthoVectorMidA[0], midPoint[1] + scaledOrthoVectorMidA[1]};
        float[] point2 = new float[]{midPoint[0] + scaledOrthoVectorMidB[0], midPoint[1] + scaledOrthoVectorMidB[1]};

        // line1 parallel to vectorMidA and contains point1
        float a1 = vectorMidA[1] / vectorMidA[0];
        float b1 = point1[1] - a1 * point1[0];

        // line2 parallel to vectorMidB and contains point2
        float a2 = vectorMidB[1] / vectorMidB[0];
        float b2 = point2[1] - a2 * point2[0];

        if (a1 == Double.POSITIVE_INFINITY || a1 == Double.NEGATIVE_INFINITY) {
            return new float[]{point1[0], a2 * point1[0] + b2};
        } else if (a2 == Double.POSITIVE_INFINITY || a2 == Double.NEGATIVE_INFINITY) {
            return new float[]{point2[0], a1 * point2[0] + b1};
        }

        float x = (b2 - b1) / (a1 - a2);
        float y = a1 * x + b1;
        return new float[]{x, y};
    }

    private float[] findBorderPoint(float[] pointA, float[] midPoint, float[] pointB, float d, float angle) {
        float[] rv1 = findLargeAngleBorderPoint(pointA, midPoint, pointB, d);
        float[] rv2 = findSmallAngleBorderPoint(pointA, midPoint, pointB, d);

        if (angle < Math.PI) {
            return new float[]{rv2[0], rv2[1], 0f, rv1[0], rv1[1], 0f};
        }

        return new float[]{rv1[0], rv1[1], 0f, rv2[0], rv2[1], 0f};
    }

    private void genBorderPointPolygon(Polygon polygon, float width, float[] angles) {
        List<TriangulationPoint> points = polygon.getPoints();

        int oldLength = borderVertexData.length;

        for (int i = 0; i < points.size(); i++) {
            float[] pointA = new float[]{(float) points.get(i).getX(), (float) points.get(i).getY()};
            float[] midPoint = new float[]{(float) points.get((i + 1) % points.size()).getX(), (float) points.get((i + 1) % points.size()).getY()};
            float[] pointB = new float[]{(float) points.get((i + 2) % points.size()).getX(), (float) points.get((i + 2) % points.size()).getY()};
            float angle = angles[(i + 1) % points.size()];

            float[] border = findBorderPoint(pointA, midPoint, pointB, width, angle);
            boolean canRepeatFirstPoint = i == 0 && oldLength > 0;
            int additionalLength = canRepeatFirstPoint ? 9 : 6;
            float[] newBorderVertexData = new float[borderVertexData.length + additionalLength];
            System.arraycopy(borderVertexData, 0, newBorderVertexData, 0, borderVertexData.length);
            System.arraycopy(border, 0, newBorderVertexData, borderVertexData.length, 3);
            if (canRepeatFirstPoint) {
                System.arraycopy(border, 0, newBorderVertexData, borderVertexData.length + 3, 3);
            }
            System.arraycopy(border, 3, newBorderVertexData, borderVertexData.length + additionalLength - 3, 3);
            borderVertexData = newBorderVertexData;

//            if (isDebug) {
//                System.out.println("----------------------------------------");
//                System.out.println("Angle: " + angle * 180 / Math.PI);
//                System.out.println("Point A: " + pointA[0] + " " + pointA[1]);
//                System.out.println("Point B: " + pointB[0] + " " + pointB[1]);
//                System.out.println("Mid Point: " + midPoint[0] + " " + midPoint[1]);
//                System.out.println("Border 1: " + border[0] + " " + border[1]);
//                System.out.println("Border 2: " + border[2] + " " + border[3]);
//                System.out.println("----------------------------------------");
//            }
        }

        int firstVertexIndex = oldLength > 0 ? oldLength + 3 : oldLength;
        float[] newBorderVertexData = new float[borderVertexData.length + 9];
        System.arraycopy(borderVertexData, 0, newBorderVertexData, 0, borderVertexData.length);
        System.arraycopy(borderVertexData, firstVertexIndex, newBorderVertexData, borderVertexData.length, 6);
        System.arraycopy(borderVertexData, firstVertexIndex + 3, newBorderVertexData, borderVertexData.length + 6, 3);

        borderVertexData = newBorderVertexData;

        if (isDebug) {
            System.out.println("------------------Length: " + borderVertexData.length / 3 + "----------------");
        }
    }

    private float[] findOrthBorderPoint(float[] firstPoint, float[] secondPoint, float width) {
        float[] vectorFirstSecond = new float[]{secondPoint[0] - firstPoint[0], secondPoint[1] - firstPoint[1]};
        float[] vectorOrthogonal = new float[]{-vectorFirstSecond[1], vectorFirstSecond[0]};
        float[] vectorScaledOrthogonal = new float[]{vectorOrthogonal[0] * width / (float) Math.sqrt(vectorOrthogonal[0] * vectorOrthogonal[0] + vectorOrthogonal[1] * vectorOrthogonal[1]), vectorOrthogonal[1] * width / (float) Math.sqrt(vectorOrthogonal[0] * vectorOrthogonal[0] + vectorOrthogonal[1] * vectorOrthogonal[1])};
        float[] vectorScaledOppositeOrthogonal = new float[]{-vectorScaledOrthogonal[0], -vectorScaledOrthogonal[1]};

        return new float[]{firstPoint[0] + vectorScaledOrthogonal[0], firstPoint[1] + vectorScaledOrthogonal[1], 0f, firstPoint[0] + vectorScaledOppositeOrthogonal[0], firstPoint[1] + vectorScaledOppositeOrthogonal[1], 0f};
    }

    private float[] findBorderPointLine(float[] lines, float width) {
        float[] vertices = new float[6];

        float[] firstBorder = findOrthBorderPoint(new float[]{lines[0], lines[1]}, new float[]{lines[2], lines[3]}, width);
        float[] vectorFirstBorder = new float[]{firstBorder[0] - firstBorder[3], firstBorder[1] - firstBorder[4]};
        System.arraycopy(firstBorder, 0, vertices, 0, 6);

        int oldAngle = 100;
        boolean isSameSide = false;
        for (int i = 0; i < lines.length - 4; i += 2) {
            float[] pointA = new float[]{lines[i], lines[i + 1]};
            float[] midPoint = new float[]{lines[i + 2], lines[i + 3]};
            float[] pointB = new float[]{lines[i + 4], lines[i + 5]};

            if (i == 0) {
                isSameSide = true;
                float[] vectorMidB = new float[]{pointB[0] - midPoint[0], pointB[1] - midPoint[1]};
                boolean angleFirst = findAngle(vectorFirstBorder, vectorMidB) > Math.PI / 2;
                oldAngle = oldAngle * (angleFirst ? 1 : -1);
            } else {
                float[] prevPoint = new float[]{lines[i - 2], lines[i - 1]};
                // line between midPoint and pointA
                float a1 = (midPoint[1] - pointA[1]) / (midPoint[0] - pointA[0]);
                float b1 = midPoint[1] - a1 * midPoint[0];

                // check if prevPoint is on the same side as pointB through line between midPoint and pointA
                if (a1 != Double.POSITIVE_INFINITY && a1 != Double.NEGATIVE_INFINITY) {
                    isSameSide = (prevPoint[1] - a1 * prevPoint[0] - b1) * (pointB[1] - a1 * pointB[0] - b1) > 0;
                } else {
                    isSameSide = (prevPoint[0] - pointB[0]) * (pointB[0] - midPoint[0]) > 0;
                }
            }

            int newAngle = oldAngle * (isSameSide ? 1 : -1);
            oldAngle = newAngle;
            int additionalLength = 6;
            float[] border = findBorderPoint(pointA, midPoint, pointB, width, newAngle);
            float[] newVertices = new float[vertices.length + additionalLength];
            System.arraycopy(vertices, 0, newVertices, 0, vertices.length);
            System.arraycopy(border, 0, newVertices, vertices.length, additionalLength);
            vertices = newVertices;
        }

        float[] lastBorder = findOrthBorderPoint(new float[]{lines[lines.length - 2], lines[lines.length - 1]}, new float[]{lines[lines.length - 4], lines[lines.length - 3]}, width);
        lastBorder = new float[]{lastBorder[3], lastBorder[4], lastBorder[5], lastBorder[0], lastBorder[1], lastBorder[2]};

        float[] newVertices = new float[vertices.length + 6];
        System.arraycopy(vertices, 0, newVertices, 0, vertices.length);
        System.arraycopy(lastBorder, 0, newVertices, vertices.length, 6);

        vertices = newVertices;

        return vertices;
    }

    public void draw(ColorShaderProgram colorProgram, float[] projectionMatrix) {
        float[] vertexData = new float[borderVertexData.length + waysVertexData.length + linesVertexData.length + testVertexData.length];
        System.arraycopy(borderVertexData, 0, vertexData, 0, borderVertexData.length);
        System.arraycopy(waysVertexData, 0, vertexData, borderVertexData.length, waysVertexData.length);
        System.arraycopy(linesVertexData, 0, vertexData, borderVertexData.length + waysVertexData.length, linesVertexData.length);
        System.arraycopy(testVertexData, 0, vertexData, borderVertexData.length + waysVertexData.length + linesVertexData.length, testVertexData.length);

//        if (isDebug) {
//            System.out.println("Border length " + linesVertexData.length / 3);
//
//            for (int i = 0; i < linesVertexData.length; i += 3) {
//                System.out.println(i / 3 + ": " + linesVertexData[i] + " " + linesVertexData[i + 1] + " " + linesVertexData[i + 2]);
//            }
//        }

        VertexArray vertexArray = new VertexArray(vertexData);
        vertexArray.setVertexAttribPointer(0, colorProgram.getPositionAttributeLocation(), 3, 0);

        colorProgram.useProgram();
        colorProgram.setUniformMVP(projectionMatrix);
        colorProgram.setUniformColor(0.57f, 0.76f, 0.88f);
        glDrawArrays(GL_TRIANGLES, borderVertexData.length / 3, waysVertexData.length / 3);


        colorProgram.setUniformColor(0f, 0f, 0f);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, borderVertexData.length / 3);

        colorProgram.setUniformColor(0.7f, 0.7f, 0.7f);
        glDrawArrays(GL_TRIANGLE_STRIP, (borderVertexData.length + waysVertexData.length) / 3, linesVertexData.length / 3);


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
