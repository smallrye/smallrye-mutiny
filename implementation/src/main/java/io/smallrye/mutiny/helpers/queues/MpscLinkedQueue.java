package io.smallrye.mutiny.helpers.queues;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A multi-producer single consumer unbounded queue.
 * Code from RX Java 2.
 *
 * @param <T> the contained value type
 */
public final class MpscLinkedQueue<T> implements Queue<T> {
    private final AtomicReference<LinkedQueueNode<T>> producerNode;
    private final AtomicReference<LinkedQueueNode<T>> consumerNode;

    public MpscLinkedQueue() {
        producerNode = new AtomicReference<>();
        consumerNode = new AtomicReference<>();
        LinkedQueueNode<T> node = new LinkedQueueNode<>();
        spConsumerNode(node);
        xchgProducerNode(node); // this ensures correct construction: StoreLoad
    }

    @Override
    public boolean add(T t) {
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
    public boolean addAll(Collection<? extends T> c) {
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

    /**
     * {@inheritDoc} <br>
     * <p>
     * IMPLEMENTATION NOTES:<br>
     * Offer is allowed from multiple threads.<br>
     * Offer allocates a new node and:
     * <ol>
     * <li>Swaps it atomically with current producer node (only one producer 'wins')
     * <li>Sets the new node as the node following from the swapped producer node
     * </ol>
     * This works because each producer is guaranteed to 'plant' a new node and link the old node. No 2 producers can
     * get the same producer node as part of XCHG guarantee.
     *
     * @see java.util.Queue#offer(Object)
     */
    @Override
    public boolean offer(final T e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }
        final LinkedQueueNode<T> nextNode = new LinkedQueueNode<>(e);
        final LinkedQueueNode<T> prevProducerNode = xchgProducerNode(nextNode);
        // Should a producer thread get interrupted here the chain WILL be broken until that thread is resumed
        // and completes the store in prev.next.
        prevProducerNode.soNext(nextNode); // StoreStore
        return true;
    }

    @Override
    public T remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc} <br>
     * <p>
     * IMPLEMENTATION NOTES:<br>
     * Poll is allowed from a SINGLE thread.<br>
     * Poll reads the next node from the consumerNode and:
     * <ol>
     * <li>If it is null, the queue is assumed empty (though it might not be).
     * <li>If it is not null set it as the consumer node and return it's now evacuated value.
     * </ol>
     * This means the consumerNode.value is always null, which is also the starting point for the queue. Because null
     * values are not allowed to be offered this is the only node with it's value set to null at any one time.
     *
     * @see java.util.Queue#poll()
     */
    @Override
    public T poll() {
        LinkedQueueNode<T> currConsumerNode = lpConsumerNode(); // don't load twice, it's alright
        LinkedQueueNode<T> nextNode = currConsumerNode.lvNext();
        if (nextNode != null) {
            // we have to null out the value because we are going to hang on to the node
            final T nextValue = nextNode.getAndNullValue();
            spConsumerNode(nextNode);
            return nextValue;
        } else if (currConsumerNode != lvProducerNode()) {
            // spin, we are no longer wait free
            //noinspection StatementWithEmptyBody
            while ((nextNode = currConsumerNode.lvNext()) == null) {
            } // got the next node...

            // we have to null out the value because we are going to hang on to the node
            final T nextValue = nextNode.getAndNullValue();
            spConsumerNode(nextNode);
            return nextValue;
        }
        return null;
    }

    @Override
    public T element() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T peek() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        //noinspection StatementWithEmptyBody
        while (poll() != null && !isEmpty()) {
        }
    }

    LinkedQueueNode<T> lvProducerNode() {
        return producerNode.get();
    }

    LinkedQueueNode<T> xchgProducerNode(LinkedQueueNode<T> node) {
        return producerNode.getAndSet(node);
    }

    LinkedQueueNode<T> lvConsumerNode() {
        return consumerNode.get();
    }

    LinkedQueueNode<T> lpConsumerNode() {
        return consumerNode.get();
    }

    void spConsumerNode(LinkedQueueNode<T> node) {
        consumerNode.lazySet(node);
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc} <br>
     * <p>
     * IMPLEMENTATION NOTES:<br>
     * Queue is empty when producerNode is the same as consumerNode. An alternative implementation would be to observe
     * the producerNode.value is null, which also means an empty queue because only the consumerNode.value is allowed to
     * be null.
     */
    @Override
    public boolean isEmpty() {
        return lvConsumerNode() == lvProducerNode();
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        throw new UnsupportedOperationException();
    }

    static final class LinkedQueueNode<E> extends AtomicReference<LinkedQueueNode<E>> {

        private static final long serialVersionUID = 2404266111789071508L;

        private E value;

        LinkedQueueNode() {
        }

        LinkedQueueNode(E val) {
            spValue(val);
        }

        /**
         * Gets the current value and nulls out the reference to it from this node.
         *
         * @return value
         */
        public E getAndNullValue() {
            E temp = lpValue();
            spValue(null);
            return temp;
        }

        public E lpValue() {
            return value;
        }

        public void spValue(E newValue) {
            value = newValue;
        }

        public void soNext(LinkedQueueNode<E> n) {
            lazySet(n);
        }

        public LinkedQueueNode<E> lvNext() {
            return get();
        }
    }
}
