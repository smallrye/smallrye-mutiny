package io.smallrye.mutiny.helpers.queues;

import java.util.Queue;
import java.util.function.Supplier;

import org.jctools.queues.atomic.unpadded.MpscAtomicUnpaddedArrayQueue;
import org.jctools.queues.atomic.unpadded.MpscUnboundedAtomicUnpaddedArrayQueue;
import org.jctools.queues.atomic.unpadded.SpscAtomicUnpaddedArrayQueue;
import org.jctools.queues.atomic.unpadded.SpscChunkedAtomicUnpaddedArrayQueue;
import org.jctools.queues.atomic.unpadded.SpscUnboundedAtomicUnpaddedArrayQueue;

import io.smallrye.mutiny.infrastructure.Infrastructure;

public class Queues {

    private Queues() {
        // avoid direct instantiation
    }

    public static <T> Queue<T> createSpscArrayQueue(int capacity) {
        return new SpscAtomicUnpaddedArrayQueue<>(capacity);
    }

    public static <T> Queue<T> createSpscUnboundedArrayQueue(int chunkSize) {
        return new SpscUnboundedAtomicUnpaddedArrayQueue<>(chunkSize);
    }

    public static <T> Queue<T> createSpscChunkedArrayQueue(int capacity) {
        return new SpscChunkedAtomicUnpaddedArrayQueue<>(capacity);
    }

    public static <T> Supplier<Queue<T>> getXsQueueSupplier() {
        return () -> createSpscArrayQueue(Infrastructure.getBufferSizeXs());
    }

    /**
     * Gets a supplier to create queues with the given buffer size (size of the array allocated as backend of the queue).
     * <p>
     * The type of the queue and configuration is computed based on the given buffer size.
     *
     * @param capacity the buffer size
     * @param <T> the type of element
     * @return the supplier.
     */
    public static <T> Supplier<Queue<T>> get(int capacity) {
        if (capacity == Infrastructure.getBufferSizeXs()) {
            return () -> createSpscArrayQueue(Infrastructure.getBufferSizeXs());
        }

        if (capacity == Infrastructure.getBufferSizeS()) {
            return () -> createSpscArrayQueue(Infrastructure.getBufferSizeS());
        }

        if (capacity == 1) {
            return SingletonQueue::new;
        }

        if (capacity == 0) {
            return EmptyQueue::new;
        }

        return () -> createSpscChunkedArrayQueue(capacity);
    }

    /**
     * Returns an unbounded Queue.
     * The queue is array-backed. Each array has the given size. If the queue is full, new arrays can be allocated.
     *
     * @param chunkSize the size of the array
     * @param <T> the type of item
     * @return the unbound queue supplier
     */
    public static <T> Supplier<Queue<T>> unbounded(int chunkSize) {
        if (chunkSize == Infrastructure.getBufferSizeXs()) {
            return () -> createSpscUnboundedArrayQueue(Infrastructure.getBufferSizeXs());
        } else if (chunkSize == Integer.MAX_VALUE || chunkSize == Infrastructure.getBufferSizeS()) {
            return () -> createSpscUnboundedArrayQueue(Infrastructure.getBufferSizeS());
        } else {
            return () -> createSpscUnboundedArrayQueue(chunkSize);
        }
    }

    /**
     * Creates a new multi-producer single consumer unbounded queue.
     *
     * @param <T> the type of item
     * @return the queue
     */
    public static <T> Queue<T> createMpscQueue() {
        return new MpscUnboundedAtomicUnpaddedArrayQueue<>(Infrastructure.getBufferSizeS());
    }

    /**
     * Creates an unbounded single producer / single consumer queue.
     *
     * @param chunkSize the chunk size
     * @param <T> the item type
     * @return the queue
     */
    public static <T> Queue<T> createSpscUnboundedQueue(int chunkSize) {
        return new SpscUnboundedAtomicUnpaddedArrayQueue<>(chunkSize);
    }

    /**
     * Create a MPSC queue with a given size
     *
     * @param capacity the queue size, will be rounded
     * @param <T> the elements type
     * @return a new queue
     */
    public static <T> Queue<T> createMpscArrayQueue(int capacity) {
        return new MpscAtomicUnpaddedArrayQueue<>(capacity);
    }
}
