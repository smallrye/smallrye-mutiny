package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

public class MultiTransform<T> {

    private final Multi<T> upstream;

    public MultiTransform(Multi<T> upstream) {
        this.upstream = upstream;
    }

    @CheckReturnValue
    public Multi<T> bySkippingFirstItems(long number) {
        return upstream.skip().first(number);
    }

    @CheckReturnValue
    public Multi<T> bySkippingLastItems(int number) {
        return upstream.skip().last(number);
    }

    @CheckReturnValue
    public Multi<T> bySkippingItemsWhile(Predicate<? super T> predicate) {
        return upstream.skip().first(predicate);
    }

    @CheckReturnValue
    public Multi<T> bySkippingItemsFor(Duration duration) {
        return upstream.skip().first(duration);
    }

    @CheckReturnValue
    public Multi<T> byTakingFirstItems(long number) {
        return upstream.select().first(number);
    }

    @CheckReturnValue
    public Multi<T> byTakingLastItems(int number) {
        return upstream.select().last(number);
    }

    @CheckReturnValue
    public Multi<T> byTakingItemsFor(Duration duration) {
        return upstream.select().first(duration);
    }

    @CheckReturnValue
    public Multi<T> byTakingItemsWhile(Predicate<? super T> predicate) {
        return upstream.select().first(predicate);
    }

    @CheckReturnValue
    public Multi<T> byDroppingDuplicates() {
        return upstream.select().distinct();
    }

    @CheckReturnValue
    public Multi<T> byDroppingRepetitions() {
        return upstream.skip().repetitions();
    }

    @SafeVarargs
    @CheckReturnValue
    public final Multi<T> byMergingWith(Publisher<T>... publishers) {
        List<Publisher<T>> list = new ArrayList<>();
        list.add(upstream);
        list.addAll(Arrays.asList(nonNull(publishers, "publishers")));
        return Multi.createBy().merging().streams(list);
    }

    @CheckReturnValue
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
    @CheckReturnValue
    public Multi<T> byFilteringItemsWith(Predicate<? super T> predicate) {
        return upstream.select().where(predicate);
    }

    /**
     * Produces a {@link Multi} containing the items from this {@link Multi} passing the {@code predicate}
     * asynchronous test. Unlike {@link #byFilteringItemsWith(Predicate)}, the test is asynchronous. Note that this method
     * preserves ordering of the items, even if the test is asynchronous.
     *
     * @param predicate the predicate, must not be {@code null}, must not produce {@code null}
     * @return the produced {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> byTestingItemsWith(Function<? super T, Uni<Boolean>> predicate) {
        // Decoration happens in `when`
        return upstream.select().when(predicate);
    }

    /**
     * Produces a new {@link Multi} transforming the upstream into a hot stream.
     * With a hot stream, when no subscribers are present, emitted items are dropped.
     * Late subscribers would only receive items emitted after their subscription.
     * If the upstream has already been terminated, the termination event (failure or completion) is forwarded to the
     * subscribers.
     * <p>
     * Note that this operator consumes the upstream stream without back-pressure.
     * It still enforces downstream back-pressure.
     * If the subscriber is not ready to receive an item when the upstream emits an item, the subscriber gets a
     * {@link io.smallrye.mutiny.subscription.BackPressureFailure} failure.
     *
     * @return the new multi.
     * @deprecated Use {@link Multi#toHotStream()} instead
     */
    @Deprecated
    @CheckReturnValue
    public Multi<T> toHotStream() {
        BroadcastProcessor<T> processor = BroadcastProcessor.create();
        upstream.subscribe(processor);
        return processor;
    }

}
