package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class FlatMapManager<T> {

    private volatile T[] array = empty();

    private int[] free = FREE_EMPTY;

    private long producerIndex;
    private long consumerIndex;

    private AtomicInteger size = new AtomicInteger();

    private static final int[] FREE_EMPTY = new int[0];

    abstract T[] empty();

    abstract T[] terminated();

    abstract T[] newArray(int size);

    abstract void unsubscribeEntry(T entry);

    abstract void setIndex(T entry, int index);

    final void unsubscribe() {
        T[] a;
        T[] t = terminated();
        synchronized (this) {
            a = array;
            if (a == t) {
                return;
            }
            size.lazySet(0);
            free = null;
            array = t;
        }
        for (T e : a) {
            if (e != null) {
                unsubscribeEntry(e);
            }
        }
    }

    final T[] get() {
        return array;
    }

    final boolean add(T entry) {
        T[] a = array;
        if (a == terminated()) {
            return false;
        }
        synchronized (this) {
            a = array;
            if (a == terminated()) {
                return false;
            }

            int idx = pollFree();
            if (idx < 0) {
                int n = a.length;
                T[] b = n != 0 ? newArray(n << 1) : newArray(4);
                System.arraycopy(a, 0, b, 0, n);

                array = b;
                a = b;

                int m = b.length;
                int[] u = new int[m];
                for (int i = n + 1; i < m; i++) {
                    u[i] = i;
                }
                free = u;
                consumerIndex = n + 1;
                producerIndex = m;

                idx = n;
            }
            setIndex(entry, idx);
            a[idx] = entry;
            size.incrementAndGet();
        }
        return true;
    }

    final void remove(int index) {
        synchronized (this) {
            T[] a = array;
            if (a != terminated()) {
                a[index] = null;
                offerFree(index);
                size.decrementAndGet();
            }
        }
    }

    int pollFree() {
        int[] a = free;
        int m = a.length - 1;
        long ci = consumerIndex;
        if (producerIndex == ci) {
            return -1;
        }
        int offset = (int) ci & m;
        consumerIndex = ci + 1;
        return a[offset];
    }

    void offerFree(int index) {
        int[] a = free;
        int m = a.length - 1;
        long pi = producerIndex;
        int offset = (int) pi & m;
        a[offset] = index;
        producerIndex = pi + 1;
    }

    final boolean isEmpty() {
        return size.get() == 0;
    }
}
