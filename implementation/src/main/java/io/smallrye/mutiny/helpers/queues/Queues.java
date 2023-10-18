package io.smallrye.mutiny.helpers.queues;

import java.util.Queue;
import java.util.function.Supplier;

import org.jctools.queues.atomic.MpscAtomicArrayQueue;
import org.jctools.queues.atomic.MpscLinkedAtomicQueue;
import org.jctools.queues.atomic.SpscAtomicArrayQueue;
import org.jctools.queues.atomic.SpscUnboundedAtomicArrayQueue;
import org.jctools.queues.unpadded.MpscLinkedUnpaddedQueue;
import org.jctools.queues.unpadded.MpscUnpaddedArrayQueue;
import org.jctools.queues.unpadded.SpscUnboundedUnpaddedArrayQueue;
import org.jctools.queues.unpadded.SpscUnpaddedArrayQueue;

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

    public static <T> Queue<T> createSpscArrayQueue(int size) {
        if (Infrastructure.useUnsafeForQueues()) {
            return new SpscUnpaddedArrayQueue<>(size);
        } else {
            return new SpscAtomicArrayQueue<>(size);
        }
    }

    public static <T> Queue<T> createSpscUnboundedArrayQueue(int size) {
        if (Infrastructure.useUnsafeForQueues()) {
            return new SpscUnboundedUnpaddedArrayQueue<>(size);
        } else {
            return new SpscUnboundedAtomicArrayQueue<>(size);
        }
    }

    public static <T> Supplier<Queue<T>> getXsQueueSupplier() {
        return () -> createSpscArrayQueue(Infrastructure.getBufferSizeXs());
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
            return () -> createSpscArrayQueue(Infrastructure.getBufferSizeXs());
        }

        if (bufferSize == Infrastructure.getBufferSizeS()) {
            return () -> createSpscArrayQueue(Infrastructure.getBufferSizeS());
        }

        if (bufferSize == 1) {
            return SingletonQueue::new;
        }

        if (bufferSize == 0) {
            return EmptyQueue::new;
        }

        final int computedSize = Math.max(8, bufferSize);
        if (computedSize > TOO_LARGE_TO_BE_BOUNDED) {
            return () -> createSpscUnboundedArrayQueue(Infrastructure.getBufferSizeS());
        } else {
            return () -> createSpscArrayQueue(computedSize);
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
            return () -> createSpscUnboundedArrayQueue(Infrastructure.getBufferSizeXs());
        } else if (size == Integer.MAX_VALUE || size == Infrastructure.getBufferSizeS()) {
            return () -> createSpscUnboundedArrayQueue(Infrastructure.getBufferSizeS());
        } else {
            return () -> createSpscUnboundedArrayQueue(size);
        }
    }

    /**
     * Creates a new multi-producer single consumer unbounded queue.
     *
     * @param <T> the type of item
     * @return the queue
     */
    public static <T> Queue<T> createMpscQueue() {
        if (Infrastructure.useUnsafeForQueues()) {
            return new MpscLinkedUnpaddedQueue<>();
        } else {
            return new MpscLinkedAtomicQueue<>();
        }
    }

    /**
     * Creates an unbounded single producer / single consumer queue.
     *
     * @param size the chunk size
     * @return the queue
     * @param <T> the item type
     */
    public static <T> Queue<T> createSpscUnboundedQueue(int size) {
        if (Infrastructure.useUnsafeForQueues()) {
            return new SpscUnboundedUnpaddedArrayQueue<>(size);
        } else {
            return new SpscUnboundedAtomicArrayQueue<>(size);
        }
    }

    /**
     * Create a MPSC queue with a given size
     *
     * @param size the queue size, will be rounded
     * @param <T> the elements type
     * @return a new queue
     */
    public static <T> Queue<T> createMpscArrayQueue(int size) {
        if (Infrastructure.useUnsafeForQueues()) {
            return new MpscUnpaddedArrayQueue<>(size);
        } else {
            return new MpscAtomicArrayQueue<>(size);
        }
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
