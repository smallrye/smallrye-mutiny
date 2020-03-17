package io.smallrye.mutiny.operators;

import java.time.Duration;
import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiDistinctOp;
import io.smallrye.mutiny.operators.multi.MultiDistinctUntilChangedOp;
import io.smallrye.mutiny.operators.multi.MultiSkipLastOp;
import io.smallrye.mutiny.operators.multi.MultiSkipOp;
import io.smallrye.mutiny.operators.multi.MultiSkipUntilOp;
import io.smallrye.mutiny.operators.multi.MultiSkipUntilPublisherOp;
import io.smallrye.mutiny.operators.multi.MultiTakeLastOp;
import io.smallrye.mutiny.operators.multi.MultiTakeOp;
import io.smallrye.mutiny.operators.multi.MultiTakeUntilOtherOp;
import io.smallrye.mutiny.operators.multi.MultiTakeWhileOp;

public class MultiTransformation {

    private MultiTransformation() {
        // avoid direct instantiation
    }

    public static <T> Multi<T> skipFirst(Multi<T> upstream, long number) {
        return Infrastructure.onMultiCreation(new MultiSkipOp<>(upstream, number));
    }

    public static <T> Multi<T> skipLast(Multi<T> upstream, int number) {
        return Infrastructure.onMultiCreation(new MultiSkipLastOp<>(upstream, number));
    }

    public static <T> Multi<T> skipForDuration(Multi<T> upstream, Duration duration) {
        Multi<Long> ticks = Multi.createFrom().ticks().startingAfter(duration).every(duration);
        return Infrastructure.onMultiCreation(new MultiSkipUntilPublisherOp<>(upstream, ticks));
    }

    public static <T> Multi<T> skipWhile(Multi<T> upstream, Predicate<? super T> predicate) {
        return Infrastructure.onMultiCreation(new MultiSkipUntilOp<>(upstream, predicate));
    }

    public static <T> Multi<T> takeFirst(Multi<T> upstream, long number) {
        return Infrastructure.onMultiCreation(new MultiTakeOp<>(upstream, number));
    }

    public static <T> Multi<T> takeLast(Multi<T> upstream, int number) {
        return Infrastructure.onMultiCreation(new MultiTakeLastOp<>(upstream, number));
    }

    public static <T> Multi<T> takeForDuration(Multi<T> upstream, Duration duration) {
        Multi<Long> ticks = Multi.createFrom().ticks().startingAfter(duration).every(duration);
        return Infrastructure.onMultiCreation(new MultiTakeUntilOtherOp<>(upstream, ticks));
    }

    public static <T> Multi<T> takeWhile(Multi<T> upstream, Predicate<? super T> predicate) {
        return Infrastructure.onMultiCreation(new MultiTakeWhileOp<>(upstream, predicate));
    }

    public static <T> Multi<T> distinct(Multi<T> upstream) {
        return Infrastructure.onMultiCreation(new MultiDistinctOp<>(upstream));
    }

    public static <T> Multi<T> dropRepetitions(Multi<T> upstream) {
        return Infrastructure.onMultiCreation(new MultiDistinctUntilChangedOp<>(upstream));
    }

}
