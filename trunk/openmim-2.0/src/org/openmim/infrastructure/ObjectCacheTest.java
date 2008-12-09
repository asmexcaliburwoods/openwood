package org.openmim.infrastructure;

import java.util.*;
import org.openmim.infrastructure.ObjectCache;

public class ObjectCacheTest {
    private final static int OBJECT_CACHE_MAX_CAPACITY = 100000;
    private final static int SIZEOF_MEASURE_OBJECT_COUNT = 1000;
    private final static Runtime rt = Runtime.getRuntime();
    private static long OBJCOUNT = -1;

    private static ObjectCache cache = new ObjectCache(OBJECT_CACHE_MAX_CAPACITY, OBJECT_CACHE_MAX_CAPACITY) {
        protected Object create() {
            return new Object();
        }
    };

    public static void main(String[] args) {
        try {
            Object[] arr = new Object[SIZEOF_MEASURE_OBJECT_COUNT];
            gc();
            long m1 = rt.freeMemory();
            for (int i = 0; i < SIZEOF_MEASURE_OBJECT_COUNT; ++i) arr[i] = new Object();
            long m2 = rt.freeMemory();
            double sizeof_d = (m1 - m2 + 0.0) / SIZEOF_MEASURE_OBJECT_COUNT;
            int sizeof = (int) Math.round(sizeof_d);
            System.out.println("sizeof(Object): " + sizeof + " (" + sizeof_d + ") bytes");
            arr = null;
            gc();
            for (; ;) {
                System.out.println("free  memory: " + ((m1 + 0.0) / (1024 * 1024)) + " MB");
                System.out.println("total memory: " + ((rt.totalMemory() + 0.0) / (1024 * 1024)) + " MB");
                OBJCOUNT = (100 * m1) / sizeof;
                System.out.println("creating " + OBJCOUNT + " objects using `new' (taking " + ((OBJCOUNT * sizeof) / (1024 * 1024)) + " MB of memory)...");
                gc();
                int oc = (int) OBJCOUNT;
                long start = System.currentTimeMillis();
                for (int i = 0; i < oc; ++i) new Object();
                long end = System.currentTimeMillis();
                System.out.println("done.  Time spent: " + (end - start) + " millis");
                cacheTest(8);
                cacheTest(0);
                cacheTest(6);
                cacheTest(4);
                cacheTest(2);
                m1 = rt.freeMemory();
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
        }
    }

    private static void gc() {
        rt.gc();
        rt.gc();
        rt.gc();
        try {
            Thread.currentThread().sleep(3000);
        } catch (InterruptedException ex) {
        }
    }

    private static void cacheTest(int RELEASE_RATE) //0..8
    {
        System.out.println("creating " + OBJCOUNT + " objects using ObjectCache, release() rate: " + ((RELEASE_RATE + 0.0) / 8) + "...");
        gc();
        int oc = (int) OBJCOUNT;
        ObjectCache cache = ObjectCacheTest.cache;
        long start = System.currentTimeMillis();
        if (RELEASE_RATE == 8)
            for (int i = 0; i < oc; ++i) {
                cache.release(cache.get());
            }
        else if (RELEASE_RATE == 0)
            for (int i = 0; i < oc; ++i) {
                cache.get();
            }
        else
            for (int i = 0; i < oc; ++i) {
                Object o = cache.get();
                if ((i & 7) <= RELEASE_RATE) cache.release(o);
            }
        long end = System.currentTimeMillis();
        System.out.println("done.  Time spent: " + (end - start) + " millis");
        gc();
    }
}
