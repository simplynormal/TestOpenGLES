/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/
package com.hcmut.test.object;

import static android.opengl.GLES20.GL_ALWAYS;
import static android.opengl.GLES20.GL_EQUAL;
import static android.opengl.GLES20.GL_INVERT;
import static android.opengl.GLES20.GL_KEEP;
import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glColorMask;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glStencilFunc;
import static android.opengl.GLES20.glStencilMask;
import static android.opengl.GLES20.glStencilOp;

import com.hcmut.test.data.Node;
import com.hcmut.test.data.Triangle;
import com.hcmut.test.data.VertexArray;
import com.hcmut.test.programs.ColorShaderProgram;

import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.geometry.primitives.Point;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

import java.util.ArrayList;
import java.util.List;

public class ObjectBuilder {
    private float[] waysVertexData = new float[0];
    private float[] borderVertexData = new float[0];
    private static boolean isDrawBorder = false;

    public void addWay(Way way, float originX, float originY, float scale) {
        if (way.isClosed()) {
            ArrayList<PolygonPoint> points = new ArrayList<>();
            for (Node node : way.nodes) {
                points.add(new PolygonPoint(
                        (node.lat - originX) * scale,
                        (node.lon - originY) * scale)
                );
            }

            Polygon polygon = new Polygon(points);
            float[] vertices = triangulate(polygon);
            float[] angles = new float[polygon.getPoints().size()];

            for (int i = 0; i < vertices.length; i += 9) {
                float[] pointA = new float[]{vertices[i], vertices[i + 1]};
                float[] pointB = new float[]{vertices[i + 3], vertices[i + 4]};
                float[] pointC = new float[]{vertices[i + 6], vertices[i + 7]};

                float angleA = (float) Math.abs(findAngle(new float[]{pointB[0] - pointA[0], pointB[1] - pointA[1]}, new float[]{pointC[0] - pointA[0], pointC[1] - pointA[1]}));
                float angleB = (float) Math.abs(findAngle(new float[]{pointA[0] - pointB[0], pointA[1] - pointB[1]}, new float[]{pointC[0] - pointB[0], pointC[1] - pointB[1]}));
                float angleC = (float) Math.abs(findAngle(new float[]{pointA[0] - pointC[0], pointA[1] - pointC[1]}, new float[]{pointB[0] - pointC[0], pointB[1] - pointC[1]}));

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
            findBorderPointPolygon(polygon, 0.005f, angles);
        }
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

    public float[] findLargeAngleBorderPoint(float[] pointA, float[] pointB, float[] midPoint, float d) {
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

    public float[] findSmallAngleBorderPoint(float[] pointA, float[] pointB, float[] midPoint, float d) {
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

    private float[] findBorderPoint(float[] pointA, float[] pointB, float[] midPoint, float d, float angle) {
        float[] rv1 = findLargeAngleBorderPoint(pointA, pointB, midPoint, d);
        float[] rv2 = findSmallAngleBorderPoint(pointA, pointB, midPoint, d);

        if (angle < Math.PI) {
            return new float[]{rv2[0], rv2[1], rv1[0], rv1[1]};
        }

        return new float[]{rv1[0], rv1[1], rv2[0], rv2[1]};
    }

    private void findBorderPointPolygon(Polygon polygon, float width, float[] angles) {
        List<TriangulationPoint> points = polygon.getPoints();

        for (int i = 0; i < points.size(); i++) {
            float[] pointA = new float[]{(float) points.get(i).getX(), (float) points.get(i).getY()};
            float[] pointB = new float[]{(float) points.get((i + 2) % points.size()).getX(), (float) points.get((i + 2) % points.size()).getY()};
            float[] midPoint = new float[]{(float) points.get((i + 1) % points.size()).getX(), (float) points.get((i + 1) % points.size()).getY()};
            float angle = angles[(i + 1) % points.size()];

            float[] border = findBorderPoint(pointA, pointB, midPoint, width, angle);
            float[] newBorderVertexData = new float[borderVertexData.length + 6];
            System.arraycopy(borderVertexData, 0, newBorderVertexData, 0, borderVertexData.length);
            newBorderVertexData[borderVertexData.length] = border[0];
            newBorderVertexData[borderVertexData.length + 1] = border[1];
            newBorderVertexData[borderVertexData.length + 2] = 0f;
            newBorderVertexData[borderVertexData.length + 3] = border[2];
            newBorderVertexData[borderVertexData.length + 4] = border[3];
            newBorderVertexData[borderVertexData.length + 5] = 0f;
            borderVertexData = newBorderVertexData;

            if (!isDrawBorder) {
                System.out.println("----------------------------------------");
                System.out.println("Angle: " + angle * 180 / Math.PI);
                System.out.println("Point A: " + pointA[0] + " " + pointA[1]);
                System.out.println("Point B: " + pointB[0] + " " + pointB[1]);
                System.out.println("Mid Point: " + midPoint[0] + " " + midPoint[1]);
                System.out.println("Border 1: " + border[0] + " " + border[1]);
                System.out.println("Border 2: " + border[2] + " " + border[3]);
                System.out.println("----------------------------------------");
            }
        }

        float[] newBorderVertexData = new float[borderVertexData.length + 6];
        System.arraycopy(borderVertexData, 0, newBorderVertexData, 0, borderVertexData.length);
        System.arraycopy(borderVertexData, 0, newBorderVertexData, borderVertexData.length, 6);
        borderVertexData = newBorderVertexData;
    }

    public void draw(ColorShaderProgram colorProgram, float[] projectionMatrix) {
        float[] vertexData = new float[borderVertexData.length + waysVertexData.length];
        System.arraycopy(borderVertexData, 0, vertexData, 0, borderVertexData.length);
        System.arraycopy(waysVertexData, 0, vertexData, borderVertexData.length, waysVertexData.length);

        if (!isDrawBorder) {
            isDrawBorder = true;
            for (int i = 0; i < borderVertexData.length; i += 3) {
                System.out.println(i / 3 + ": " + borderVertexData[i] + " " + borderVertexData[i + 1] + " " + borderVertexData[i + 2]);
            }
        }

        VertexArray vertexArray = new VertexArray(vertexData);
        vertexArray.setVertexAttribPointer(0, colorProgram.getPositionAttributeLocation(), 3, 0);

        colorProgram.useProgram();
        colorProgram.setUniformMVP(projectionMatrix);
        colorProgram.setUniformColor(0f, 1f, 0f);
        glDrawArrays(GL_TRIANGLES, borderVertexData.length / 3, waysVertexData.length / 3);

//        glColorMask(false, false, false, false);
//        glStencilMask(1);
//        glStencilFunc(GL_ALWAYS, 0, 1);
//        glStencilOp(GL_KEEP, GL_KEEP, GL_INVERT);
//
//        glDrawArrays(GL_TRIANGLE_STRIP, 0, borderVertexData.length / 3);
//
//        glColorMask(true, true, true, true);
//        glStencilFunc(GL_EQUAL, 0, 1);
//        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        
        colorProgram.setUniformColor(1f, 0f, 0f);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, borderVertexData.length / 3);

//        colorProgram.setUniformColor(0f, 0f, 1f);
//        glDrawArrays(GL_LINES, 0, borderVertexData.length / 3);
    }
}
