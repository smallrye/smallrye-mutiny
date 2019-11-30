package io.smallrye.mutiny.helpers.queues;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * A Single-Producer-Single-Consumer queue backed by a pre-allocated buffer.
 * <p>
 * Code inspired from https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/atomic,
 * and it's RX Java 2 version.
 *
 * @param <E> the element type of the queue
 */
public final class SpscArrayQueue<E> extends AtomicReferenceArray<E> implements Queue<E> {
    private static final Integer MAX_LOOK_AHEAD_STEP = 4096;
    private final int mask;
    private final AtomicLong producerIndex = new AtomicLong();
    private long producerLookAhead;
    private final AtomicLong consumerIndex = new AtomicLong();
    private final int lookAheadStep;

    public SpscArrayQueue(int capacity) {
        super(roundToPowerOfTwo(capacity));
        this.mask = length() - 1;
        this.lookAheadStep = Math.min(capacity / 4, MAX_LOOK_AHEAD_STEP);
    }

    /**
     * Find the next larger positive power of two value up from the given value. If value is a power of two then
     * this value will be returned.
     *
     * @param value from which next positive power of two will be found.
     * @return the next positive power of 2 or this value if it is a power of 2.
     */
    public static int roundToPowerOfTwo(final int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    @Override
    public boolean offer(E e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }
        // local load of field to avoid repeated loads after volatile reads
        final int mask = this.mask;
        final long index = producerIndex.get();
        final int offset = calcElementOffset(index, mask);
        if (index >= producerLookAhead) {
            int step = lookAheadStep;
            if (null == lvElement(calcElementOffset(index + step, mask))) { // LoadLoad
                producerLookAhead = index + step;
            } else if (null != lvElement(offset)) {
                return false;
            }
        }
        soElement(offset, e); // StoreStore
        soProducerIndex(index + 1); // ordered store -> atomic and ordered for size()
        return true;
    }

    @Override
    public E poll() {
        final long index = consumerIndex.get();
        final int offset = calcElementOffset(index);
        // local load of field to avoid repeated loads after volatile reads
        final E e = lvElement(offset); // LoadLoad
        if (null == e) {
            return null;
        }
        soConsumerIndex(index + 1); // ordered store -> atomic and ordered for size()
        soElement(offset, null); // StoreStore
        return e;
    }

    @Override
    public int size() {
        long ci = consumerIndex.get();
        for (;;) {
            long pi = producerIndex.get();
            long ci2 = consumerIndex.get();
            if (ci == ci2) {
                return (int) (pi - ci);
            }
            ci = ci2;
        }
    }

    public E peek() {
        int offset = (int) consumerIndex.get() & mask;
        return get(offset);
    }

    @Override
    public boolean isEmpty() {
        return producerIndex.get() == consumerIndex.get();
    }

    void soProducerIndex(long newIndex) {
        producerIndex.lazySet(newIndex);
    }

    void soConsumerIndex(long newIndex) {
        consumerIndex.lazySet(newIndex);
    }

    @Override
    public void clear() {
        // we have to test isEmpty because of the weaker poll() guarantee
        //noinspection StatementWithEmptyBody
        while (poll() != null || !isEmpty()) {
        }
    }

    int calcElementOffset(long index, int mask) {
        return (int) index & mask;
    }

    int calcElementOffset(long index) {
        return (int) index & mask;
    }

    void soElement(int offset, E value) {
        lazySet(offset, value);
    }

    E lvElement(int offset) {
        return get(offset);
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> R[] toArray(R[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E element() {
        throw new UnsupportedOperationException();
    }
}
