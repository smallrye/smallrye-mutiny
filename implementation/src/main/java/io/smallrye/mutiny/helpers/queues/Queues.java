package io.smallrye.mutiny.helpers.queues;

import java.util.Queue;
import java.util.function.Supplier;

import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.MpscLinkedQueue;
import org.jctools.queues.SpscArrayQueue;
import org.jctools.queues.SpscUnboundedArrayQueue;

import io.smallrye.mutiny.infrastructure.Infrastructure;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class Queues {

    /**
     * Queues with a requested with a capacity greater than this value are unbounded.
     */
    public static final int TOO_LARGE_TO_BE_BOUNDED = 10_000_000;

    private Queues() {
        // avoid direct instantiation
    }

    static final Supplier EMPTY_QUEUE_SUPPLIER = EmptyQueue::new;
    static final Supplier SINGLETON_QUEUE_SUPPLIER = SingletonQueue::new;

    static final Supplier XS_QUEUE_SUPPLIER = () -> new SpscArrayQueue<>(Infrastructure.getBufferSizeXs());
    static final Supplier S_QUEUE_SUPPLIER = () -> new SpscArrayQueue<>(Infrastructure.getBufferSizeS());

    static final Supplier UNBOUNDED_QUEUE_SUPPLIER = () -> new SpscUnboundedArrayQueue<>(Infrastructure.getBufferSizeS());
    static final Supplier XS_UNBOUNDED_QUEUE_SUPPLIER = () -> new SpscUnboundedArrayQueue<>(Infrastructure.getBufferSizeXs());

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
        if (bufferSize == Infrastructure.getBufferSizeXs()) {
            return XS_QUEUE_SUPPLIER;
        }

        if (bufferSize == Infrastructure.getBufferSizeS()) {
            return S_QUEUE_SUPPLIER;
        }

        if (bufferSize == 1) {
            return SINGLETON_QUEUE_SUPPLIER;
        }

        if (bufferSize == 0) {
            return EMPTY_QUEUE_SUPPLIER;
        }

        final int computedSize = Math.max(8, bufferSize);
        if (computedSize > TOO_LARGE_TO_BE_BOUNDED) {
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
        if (size == Infrastructure.getBufferSizeXs()) {
            return XS_UNBOUNDED_QUEUE_SUPPLIER;
        } else if (size == Integer.MAX_VALUE || size == Infrastructure.getBufferSizeS()) {
            return UNBOUNDED_QUEUE_SUPPLIER;
        } else {
            return () -> new SpscUnboundedArrayQueue<>(size);
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
     * Create a MPSC queue with a given size
     *
     * @param size the queue size, will be rounded
     * @param <T> the elements type
     * @return a new queue
     */
    public static <T> Queue<T> createMpscArrayQueue(int size) {
        return new MpscArrayQueue<>(size);
    }

    /**
     * Check when a non-strictly sized queue overflow.
     *
     * @param queue the queue
     * @param limit the limit, a negative value assumes an unbounded queue
     * @return {@code true} if the queue overflow, {@code false} otherwise
     */
    public static boolean isOverflowing(Queue<?> queue, int limit) {
        if (limit < 0) {
            return false;
        }
        return queue.size() >= limit;
    }
}
