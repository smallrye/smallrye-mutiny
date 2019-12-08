package io.smallrye.mutiny.operators;

import java.time.Duration;
import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.*;

public class MultiTransformation {

    private MultiTransformation() {
        // avoid direct instantiation
    }

    public static <T> Multi<T> skipFirst(Multi<T> upstream, long number) {
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

    public static <T> Multi<T> takeFirst(Multi<T> upstream, long number) {
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

}
