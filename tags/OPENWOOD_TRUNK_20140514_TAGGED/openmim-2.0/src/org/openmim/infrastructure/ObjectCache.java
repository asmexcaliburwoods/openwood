package org.openmim.infrastructure;

import java.util.*;

public abstract class ObjectCache {
    private final Object[] cache;
    private int size;
    private final int max;

    protected abstract Object create();

    /**
     @param init  number of instances initially created
     @param max   maximum number of instances cached
     */
    public ObjectCache(int init, int max) {
        if (init < 0) throw new IllegalArgumentException("init < 0: init=" + init);
        if (max < init || max == 0) throw new IllegalArgumentException("max < init || max == 0: init=" + init + ", max=" + max);
        this.max = max;
        cache = new Object[max];
        for (int i = 0; i < init; ++i) cache[i] = create();
        size = init;
    }

    /**
     No instances created initially.
     @param max  maximum number of instances cached
     */
    public ObjectCache(int max) {
        this(0, max);
    }

    public final synchronized Object get() {
        if (size > 0)
            return cache[--size];
        else
            return create();
    }

    public final synchronized void release(Object o) {
        //if (DEBUG && cache.indexOf(o) != -1) throw new RuntimeException("release of released object");
        if (size < max) {
            cache[size++] = o;
        }
    }
}
