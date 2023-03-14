package io.smallrye.mutiny.helpers.queues;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Supplier;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class Queues {

    /**
     * Queues with a requested with a capacity greater than this value are unbounded.
     */
    public static final int TO_LARGE_TO_BE_BOUNDED = 10_000_000;

    private Queues() {
        // avoid direct instantiation
    }

    public static final int BUFFER_XS = Math.max(8,
            Integer.parseInt(System.getProperty("mutiny.buffer-size.xs", "32")));

    public static final int BUFFER_S = Math.max(16,
            Integer.parseInt(System.getProperty("mutiny.buffer-size.s", "256")));

    static final Supplier EMPTY_QUEUE_SUPPLIER = EmptyQueue::new;
    static final Supplier SINGLETON_QUEUE_SUPPLIER = SingletonQueue::new;

    static final Supplier XS_QUEUE_SUPPLIER = () -> new SpscArrayQueue<>(BUFFER_XS);
    static final Supplier S_QUEUE_SUPPLIER = () -> new SpscArrayQueue<>(BUFFER_S);

    static final Supplier UNBOUNDED_QUEUE_SUPPLIER = () -> new SpscLinkedArrayQueue<>(BUFFER_S);
    static final Supplier XS_UNBOUNDED_QUEUE_SUPPLIER = () -> new SpscLinkedArrayQueue<>(BUFFER_XS);

    public static <T> Supplier<Queue<T>> getXsQueueSupplier() {
        return (Supplier<Queue<T>>) XS_QUEUE_SUPPLIER;
    }

    /**
     * Gets a supplier to create queues with the given buffer size (size of the array allocated as backend of the queue).
     * <p>
     * The type of the queue and configuration is computed based on the given buffer size.
     *
     * @param bufferSize the buffer size
     * @param <T> the type of element
     * @return the supplier.
     */
    public static <T> Supplier<Queue<T>> get(int bufferSize) {
        if (bufferSize == BUFFER_XS) {
            return XS_QUEUE_SUPPLIER;
        }

        if (bufferSize == BUFFER_S) {
            return S_QUEUE_SUPPLIER;
        }

        if (bufferSize == 1) {
            return SINGLETON_QUEUE_SUPPLIER;
        }

        if (bufferSize == 0) {
            return EMPTY_QUEUE_SUPPLIER;
        }

        final int computedSize = Math.max(8, bufferSize);
        if (computedSize > TO_LARGE_TO_BE_BOUNDED) {
            return UNBOUNDED_QUEUE_SUPPLIER;
        } else {
            return () -> new SpscArrayQueue<>(computedSize);
        }
    }

    /**
     * Returns an unbounded Queue.
     * The queue is array-backed. Each array has the given size. If the queue is full, new arrays can be allocated.
     *
     * @param size the size of the array
     * @param <T> the type of item
     * @return the unbound queue supplier
     */
    @SuppressWarnings("unchecked")
    public static <T> Supplier<Queue<T>> unbounded(int size) {
        if (size == BUFFER_XS) {
            return XS_UNBOUNDED_QUEUE_SUPPLIER;
        } else if (size == Integer.MAX_VALUE || size == BUFFER_S) {
            return UNBOUNDED_QUEUE_SUPPLIER;
        } else {
            return () -> new SpscLinkedArrayQueue<>(size);
        }
    }

    /**
     * Creates a new multi-producer single consumer unbounded queue.
     *
     * @param <T> the type of item
     * @return the queue
     */
    public static <T> Queue<T> createMpscQueue() {
        return new MpscLinkedQueue<>();
    }

    /**
     * Create a queue of a strict fixed size.
     *
     * @param size the queue size
     * @param <T> the elements type
     * @return a new queue
     */
    public static <T> Queue<T> createStrictSizeQueue(int size) {
        return new ArrayBlockingQueue<>(size);
    }
}
