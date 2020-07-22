package io.smallrye.mutiny.helpers.queues;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class SingletonQueue<T> implements Queue<T> {

    private final AtomicReference<T> element = new AtomicReference<>();

    @Override
    public boolean add(T t) {
        return offer(t);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        if (c.isEmpty()) {
            return true;
        }
        if (c.size() == 1) {
            return offer(c.iterator().next());
        }
        return false;
    }

    @Override
    public void clear() {
        element.set(null);
    }

    @Override
    public boolean contains(Object o) {
        return Objects.equals(element.get(), o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c.isEmpty()) {
            return true;
        }
        if (c.size() == 1) {
            return Objects.equals(c.iterator().next(), peek());
        }
        return false;
    }

    @Override
    public T element() {
        return element.get();
    }

    @Override
    public boolean isEmpty() {
        return element.get() == null;
    }

    @Override
    public Iterator<T> iterator() {
        return new SingletonIterator<>(this);
    }

    @Override
    public boolean offer(T t) {
        if (element.get() != null) {
            return false;
        }
        element.lazySet(t);
        return true;
    }

    @Override
    public T peek() {
        return element.get();
    }

    @Override
    public T poll() {
        T v = element.get();
        if (v != null) {
            element.lazySet(null);
        }
        return v;
    }

    @Override
    public T remove() {
        return element.getAndSet(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
        try {
            return element.compareAndSet((T) o, null);
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        // Not supported
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        // Not supported
        return false;
    }

    @Override
    public int size() {
        return element.get() == null ? 0 : 1;
    }

    @Override
    public Object[] toArray() {
        T t = element.get();
        if (t == null) {
            return new Object[0];
        }
        return new Object[] { t };
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T1> T1[] toArray(T1[] a) {
        int size = size();
        if (a.length < size) {
            a = (T1[]) Array.newInstance(a.getClass().getComponentType(), size);
        }
        if (size == 1) {
            a[0] = (T1) element.get();
        }
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    private static final class SingletonIterator<T> implements Iterator<T> {

        final SingletonQueue<T> queue;
        final AtomicBoolean consumed = new AtomicBoolean();

        public SingletonIterator(SingletonQueue<T> queue) {
            this.queue = queue;
        }

        @Override
        public boolean hasNext() {
            if (queue.isEmpty()) {
                return false;
            }
            return !consumed.get();
        }

        @Override
        public T next() {
            if (consumed.compareAndSet(false, true)) {
                return queue.peek();
            }
            return null;
        }

        @Override
        public void remove() {
            queue.remove();
        }
    }
}
