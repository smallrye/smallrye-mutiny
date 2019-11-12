package io.smallrye.reactive.operators;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.queues.SpscArrayQueue;
import io.smallrye.reactive.operators.multi.*;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class MultiTransformation {

    private MultiTransformation() {
        // avoid direct instantiation
    }

    public static <T> Multi<T> skipFirst(Multi<T> upstream, int number) {
        return new MultiSkipOp<>(upstream, number);
    }

    public static <T> Multi<T> skipLast(Multi<T> upstream, int number) {
        return new MultiSkipLastOp<>(upstream, number);
    }

    public static <T> Multi<T> skipForDuration(Multi<T> upstream, Duration duration) {
        Multi<Long> ticks = Multi.createFrom().ticks().startingAfter(duration).every(duration);
        return new MultiSkipUntilPublisherOp<>(upstream, ticks);
    }

    public static <T> Multi<T> skipWhile(Multi<T> upstream, Predicate<? super T> predicate) {
        return new MultiSkipUntilOp<>(upstream, predicate);
    }

    public static <T> Multi<T> takeFirst(Multi<T> upstream, int number) {
        return new MultiTakeOp<>(upstream, number);
    }

    public static <T> Multi<T> takeLast(Multi<T> upstream, int number) {
        return new MultiTakeLastOp<>(upstream, number);
    }

    public static <T> Multi<T> takeForDuration(Multi<T> upstream, Duration duration) {
        Multi<Long> ticks = Multi.createFrom().ticks().startingAfter(duration).every(duration);
        return new MultiTakeUntilOtherOp<>(upstream, ticks);
    }

    public static <T> Multi<T> takeWhile(Multi<T> upstream, Predicate<? super T> predicate) {
        return new MultiTakeWhileOp<>(upstream, predicate);
    }

    public static <T> Multi<T> distinct(Multi<T> upstream) {
        return new MultiDistinctOp<>(upstream);
    }

    public static <T> Multi<T> dropRepetitions(Multi<T> upstream) {
        return new MultiDistinctUntilChangedOp<>(upstream);
    }

    public static <T> Multi<T> merge(Multi<T> upstream, Publisher<T>... publishers) {
        return merge(upstream, Arrays.asList(publishers));
    }

    public static <T> Multi<T> merge(Multi<T> upstream, Iterable<Publisher<T>> iterable) {
        List<Publisher<? extends T>> list = new ArrayList<>();
        list.add(upstream);
        iterable.forEach(list::add);
        return new MultiMergeOp<>(
                list,
                false,
                list.size(),
                8,
                () -> new SpscArrayQueue<>(16),
                () -> new SpscArrayQueue<>(8)
        );

    }

}
