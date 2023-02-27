package com.hcmut.test;

import com.hcmut.test.utils.StrokeGenerator;
import com.menecats.polybool.Epsilon;
import com.menecats.polybool.PolyBool;
import com.menecats.polybool.models.Polygon;

import static com.menecats.polybool.helpers.PolyBoolHelper.*;

import java.util.List;

public class Test {
    public static void union() {
        Epsilon eps = epsilon();

        Polygon union = PolyBool.union(
                eps,
                polygon(
                        region(
                                point(0, 0),
                                point(2, 0),
                                point(1.91, 5.5),
                                point(1.16, 4.12),
                                point(4.9, 4.19),
                                point(4.83, 6.65),
                                point(0, 6.65)
                        )
                ),
                polygon(
                        region(
                                point(0, 0),
                                point(2, 0),
                                point(1.91, 5.5),
                                point(1.16, 4.12),
                                point(4.9, 4.19),
                                point(4.83, 6.65),
                                point(0, 6.65)
                        )
                )
        );

        List<List<double[]>> regions = union.getRegions();
        Polygon newUnion = null;
        while (regions.size() > 1) {
            newUnion = polygon(regions.get(0));
            for (int i = 1; i < regions.size(); i++) {
                newUnion = PolyBool.union(eps, newUnion, polygon(regions.get(i)));
            }
            regions = newUnion.getRegions();
        }

        System.out.println(newUnion);
        // Polygon { inverted: false, regions: [
        //     [[50.0, 50.0], [110.0, 50.0], [110.0, 110.0]],
        //     [[178.0, 80.0], [130.0, 50.0], [130.0, 130.0], [150.0, 150.0]],
        //     [[178.0, 80.0], [190.0, 50.0], [260.0, 50.0], [260.0, 131.25]]
        // ]}}
    }

    public static void main() {
        StrokeGenerator.test();
    }
}
