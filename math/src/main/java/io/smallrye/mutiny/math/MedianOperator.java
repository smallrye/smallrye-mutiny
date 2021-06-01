package io.smallrye.mutiny.math;

import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;

/**
 * Median operator emitting the median all all the item emitter by the upstream.
 * <p>
 * Everytime it gets an item from upstream, it emits the <em>median</em> of the already received items.
 * If the stream emits the completion event without having emitting any item before, the completion event is emitted.
 * If the upstream emits a failure, then, the failure is propagated.
 */
public class MedianOperator<T extends Number & Comparable<T>> implements Function<Multi<T>, Multi<Double>> {

    // Using the same initial capacity 11 as PriorityQueue
    private final Queue<T> minHeap = new PriorityBlockingQueue<>(11);
    private final Queue<T> maxHeap = new PriorityBlockingQueue<>(11, Comparator.reverseOrder());

    @Override
    public Multi<Double> apply(Multi<T> multi) {
        return multi
                .onItem().transform(x -> {
                    push(x);
                    return getMedian();
                });
    }

    void push(T num) {
        if (minHeap.size() == maxHeap.size()) {
            maxHeap.offer(num);
            minHeap.offer(maxHeap.poll());
        } else {
            minHeap.offer(num);
            maxHeap.offer(minHeap.poll());
        }
    }

    double getMedian() {
        if (minHeap.size() > maxHeap.size()) {
            return minHeap.peek().doubleValue();
        } else {
            return (minHeap.peek().doubleValue() + maxHeap.peek().doubleValue()) / 2.0d;
        }
    }

}
