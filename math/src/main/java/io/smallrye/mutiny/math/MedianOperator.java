package io.smallrye.mutiny.math;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;

/**
 * Median operator emitting the median all all the item emitter by the upstream.
 * <p>
 * Everytime it gets an item from upstream, it emits the <em>median</em> of the already received items.
 * If the stream emits the completion event without having emitting any item before, the completion event is emitted.
 * If the upstream emits a failure, then, the failure is propagated.
 */
public class MedianOperator<T extends Number & Comparable<T>>
        implements Function<Multi<T>, Multi<Double>> {

    private final Queue<T> minHeap;
    private final Queue<T> maxHeap;

    public MedianOperator() {
        minHeap = new PriorityQueue<>();
        maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
    }

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
