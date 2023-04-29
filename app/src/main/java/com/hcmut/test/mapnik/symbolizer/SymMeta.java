package com.hcmut.test.mapnik.symbolizer;

public abstract class SymMeta {
    public abstract boolean isEmpty();

    public abstract SymMeta append(SymMeta other);

    public static float[] appendTriangleStrip(float[] oldData, float[] newData, int totalVertexAttribCount) {
        if (oldData.length == 0) return newData;

        boolean oldDrawableEmpty = newData.length == 0;
        float[] result = new float[newData.length + oldData.length + (oldDrawableEmpty ? 0 : totalVertexAttribCount * 2)];
        System.arraycopy(newData, 0, result, 0, newData.length);
        if (!oldDrawableEmpty) {
            System.arraycopy(newData, newData.length - totalVertexAttribCount, result, newData.length, totalVertexAttribCount);
            System.arraycopy(oldData, 0, result, newData.length + totalVertexAttribCount, totalVertexAttribCount);
        }
        System.arraycopy(oldData, 0, result, newData.length + (oldDrawableEmpty ? 0 : totalVertexAttribCount * 2), oldData.length);
        return result;
    }

    public static float[] appendRegular(float[] oldDrawable, float[] newDrawable) {
        float[] result = new float[oldDrawable.length + newDrawable.length];
        System.arraycopy(oldDrawable, 0, result, 0, oldDrawable.length);
        System.arraycopy(newDrawable, 0, result, oldDrawable.length, newDrawable.length);
        return result;
    }
}
