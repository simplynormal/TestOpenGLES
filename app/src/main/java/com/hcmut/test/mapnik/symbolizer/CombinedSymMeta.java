package com.hcmut.test.mapnik.symbolizer;

import com.hcmut.test.data.VertexArray;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.List;

public class CombinedSymMeta extends SymMeta {
    private final List<SymMeta> symMetas;
    protected VertexArray vertexArray = null;

    public CombinedSymMeta() {
        this.symMetas = new ArrayList<>(0);
    }

    public CombinedSymMeta(List<SymMeta> symMetas) {
        this.symMetas = new ArrayList<>(symMetas);
    }

    @Override
    public boolean isEmpty() {
        return vertexArray == null && symMetas.isEmpty();
    }

    private static boolean isSameSymMeta(SymMeta symMeta1, SymMeta symMeta2) {
        return symMeta1.getClass().equals(symMeta2.getClass());
    }

    @Override
    public SymMeta append(SymMeta other) {
        if (other == null || other.isEmpty()) return this;

        if (other instanceof CombinedSymMeta) {
            CombinedSymMeta otherCast = (CombinedSymMeta) other;
            if (symMetas.isEmpty()) {
                return new CombinedSymMeta(otherCast.symMetas);
            }

            SymMeta last = symMetas.get(symMetas.size() - 1);
            SymMeta otherFirst = otherCast.symMetas.get(0);
            if (isSameSymMeta(last, otherFirst)) {
                List<SymMeta> result = new ArrayList<>(symMetas.subList(0, symMetas.size() - 1));
                result.add(last.append(otherFirst));
                result.addAll(otherCast.symMetas.subList(1, otherCast.symMetas.size()));
                return new CombinedSymMeta(result);
            }

            List<SymMeta> result = new ArrayList<>(symMetas);
            result.addAll(otherCast.symMetas);
            return new CombinedSymMeta(result);
        }

        List<SymMeta> rv = new ArrayList<>(symMetas);
        if (symMetas.isEmpty()) {
            rv.add(other);
            return new CombinedSymMeta(rv);
        }

        SymMeta last = symMetas.get(symMetas.size() - 1);
        if (isSameSymMeta(last, other)) {
            rv.set(rv.size() - 1, last.append(other));
        } else {
            rv.add(other);
        }
        return new CombinedSymMeta(rv);
    }

    @Override
    public void draw(Config config) {
        for (SymMeta symMeta : symMetas) {
            symMeta.draw(config);
        }
    }
}
