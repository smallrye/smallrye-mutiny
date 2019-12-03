package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.MultiTransformation;
import io.smallrye.mutiny.operators.multi.MultiFilterOp;

public class MultiTransform<T> {

    private final Multi<T> upstream;

    public MultiTransform(Multi<T> upstream) {
        this.upstream = upstream;
    }

    public Multi<T> bySkippingFirstItems(long number) {
        return MultiTransformation.skipFirst(upstream, positiveOrZero(number, "number"));
    }

    public Multi<T> bySkippingLastItems(int number) {
        return MultiTransformation.skipLast(upstream, positiveOrZero(number, "number"));
    }

    public Multi<T> bySkippingItemsWhile(Predicate<? super T> predicate) {
        return MultiTransformation.skipWhile(upstream, nonNull(predicate, "predicate"));
    }

    public Multi<T> bySkippingItemsFor(Duration duration) {
        return MultiTransformation.skipForDuration(upstream, validate(duration, "duration"));
    }

    public Multi<T> byTakingFirstItems(long number) {
        return MultiTransformation.takeFirst(upstream, positiveOrZero(number, "number"));
    }

    public Multi<T> byTakingLastItems(int number) {
        return MultiTransformation.takeLast(upstream, positiveOrZero(number, "number"));
    }

    public Multi<T> byTakingItemsFor(Duration duration) {
        return MultiTransformation.takeForDuration(upstream, validate(duration, "duration"));
    }

    public Multi<T> byTakingItemsWhile(Predicate<? super T> predicate) {
        return MultiTransformation.takeWhile(upstream, nonNull(predicate, "predicate"));
    }

    public Multi<T> byKeepingDistinctItems() {
        return MultiTransformation.distinct(upstream);
    }

    public Multi<T> byDroppingRepetitions() {
        return MultiTransformation.dropRepetitions(upstream);
    }

    @SafeVarargs
    public final Multi<T> byMergingWith(Publisher<T>... publishers) {
        List<Publisher<T>> list = new ArrayList<>();
        list.add(upstream);
        list.addAll(Arrays.asList(nonNull(publishers, "publishers")));
        return Multi.createBy().merging().streams(list);
    }

    public Multi<T> byMergingWith(Iterable<Publisher<T>> iterable) {
        List<Publisher<T>> list = new ArrayList<>();
        list.add(upstream);
        nonNull(iterable, "iterable").forEach(list::add);
        return Multi.createBy().merging().streams(list);
    }

    /**
     * Produces a {@link Multi} containing the items from this {@link Multi} passing the {@code predicate} test.
     *
     * @param predicate the predicate, must not be {@code null}
     * @return the produced {@link Multi}
     */
    public Multi<T> byFilteringItemsWith(Predicate<? super T> predicate) {
        return new MultiFilterOp<>(upstream, nonNull(predicate, "predicate"));
    }

    /**
     * Produces a {@link Multi} containing the items from this {@link Multi} passing the {@code tester}
     * asynchronous test. Unlike {@link #byFilteringItemsWith(Predicate)}, the test is asynchronous. Note that this method
     * preserves ordering of the items, even if the test is asynchronous.
     *
     * @param tester the predicate, must not be {@code null}, must not produce {@code null}
     * @return the produced {@link Multi}
     */
    public Multi<T> byTestingItemsWith(Function<? super T, ? extends Uni<Boolean>> tester) {
        nonNull(tester, "tester");
        return upstream.onItem().flatMap().multi(res -> {
            Uni<Boolean> uni = tester.apply(res);
            return uni.map(pass -> {
                if (pass) {
                    return res;
                } else {
                    return null;
                }
            }).toMulti();
        }).concatenateResults();
    }

}
