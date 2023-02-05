package com.hcmut.test.data;

import androidx.annotation.NonNull;

public class Triangle {
    public final Node p1;
    public final Node p2;
    public final Node p3;

    public Triangle(Node p1, Node p2, Node p3) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
    }

    @NonNull
    @Override
    public String toString() {
        return "Triangle{" +
                "p1=" + p1 +
                ", p2=" + p2 +
                ", p3=" + p3 +
                '}';
    }
}
