package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.*;

/**
 * Selects items from the upstream {@link Multi}.
 *
 * @param <T> the type of item
 * @see MultiSkip
 */
public class MultiSelect<T> {

    private final Multi<T> upstream;

    public MultiSelect(Multi<T> upstream) {
        this.upstream = upstream;
    }

    /**
     * Select the first item from the {@link Multi}.
     * <p>
     * If the upstream {@link Multi} contains more than one item, the others are dropped.
     * If the upstream emits a failure before emitting an item, the produced {@link Multi} emits the same failure.
     * If the upstream completes without emitting an item first, the produced {@link Multi} is empty.
     *
     * @return the resulting {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> first() {
        return first(1);
    }

    /**
     * Select the last item from the {@link Multi}.
     * <p>
     * If the upstream {@link Multi} contains more than one item, the others are dropped, only the last one is emitted
     * by the produced {@link Multi}.
     * If the upstream emits a failure, the produced {@link Multi} emits the same failure.
     * If the upstream completes without emitting an item first, the produced {@link Multi} is empty.
     *
     * @return the resulting {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> last() {
        return last(1);
    }

    /**
     * Selects the first {@code n} items from the {@link Multi}.
     * <p>
     * If the upstream {@link Multi} contains more than n items, the others are dropped.
     * If the upstream {@link Multi} emits less than n items, all the items are emitted by the produced {@link Multi}.
     * If the upstream emits a failure before emitting n items, the produced {@link Multi} emits the same failure after
     * having emitted the first items.
     * If the upstream completes without emitting an item first, the produced {@link Multi} is empty.
     *
     * @param n the number of items to select, must be positive. If 0, the resulting {@link Multi} is empty.
     * @return the resulting {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> first(long n) {
        return Infrastructure.onMultiCreation(new MultiSelectFirstOp<>(upstream, n));
    }

    /**
     * Selects the last {@code n} items from the {@link Multi}.
     * <p>
     * If the upstream {@link Multi} contains more than n items, the others are dropped.
     * If the upstream {@link Multi} emits less than n items, all the items are emitted by the produced {@link Multi}.
     * If the upstream emits a failure, the produced {@link Multi} emits the same failure after. No items will
     * be emitted by the produced {@link Multi}.
     * If the upstream completes without emitting an item first, the produced {@link Multi} is empty.
     *
     * @param n the number of items to select, must be positive. If 0, the resulting {@link Multi} is empty.
     * @return the resulting {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> last(int n) {
        return Infrastructure.onMultiCreation(new MultiSelectLastOp<>(upstream, n));
    }

    /**
     * Selects the first items while the given predicate returns {@code true}.
     * It calls the predicates for each items, until the predicate returns {@code false}.
     * Each item for which the predicates returned {@code true} is emitted by the produced {@link Multi}.
     * As soon as the predicate returns {@code false} for an item, it stops emitting the item and sends the completion
     * event. The last checked item is not emitted.
     * <p>
     * If the upstream {@link Multi} is empty, the produced {@link Multi} is empty.
     * If the upstream {@link Multi} is emitting a failure, while the predicate has not returned {@code false} yet, the
     * failure is emitted by the produced {@link Multi}.
     * If the predicates throws an exception while testing an item, the produced {@link Multi} emits that exception as
     * failure. No more items will be tested or emitted.
     * If the predicates returns {@code true} for each items from upstream, all the items are selected.
     * Once the predicate returns {@code false}, it cancels the subscription to the upstream, and completes the produced
     * {@link Multi}.
     *
     * @param predicate the predicate to test the items, must not be {@code null}
     * @return the resulting {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> first(Predicate<? super T> predicate) {
        Predicate<? super T> actual = Infrastructure.decorate(nonNull(predicate, "predicate"));
        return Infrastructure.onMultiCreation(new MultiSelectFirstWhileOp<>(upstream, actual));
    }

    /**
     * Selects the first items for the given duration.
     * It selects each items emitted after the subscription for the given duration.
     * <p>
     * If the upstream {@link Multi} is empty, the produced {@link Multi} is empty.
     * If the upstream {@link Multi} is emitting a failure, before the duration expires, the failure is emitted by the
     * produced {@link Multi}.
     * If the upstream completes before the given duration, all the items are selected.
     * <p>
     * Once the duration expires, it cancels the subscription to the upstream, and completes the produced
     * {@link Multi}.
     *
     * @param duration the duration, must not be {@code null}, must be strictly positive.
     * @return the resulting {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> first(Duration duration) {
        Multi<Long> ticks = Multi.createFrom().ticks().startingAfter(duration).every(duration);
        return Infrastructure.onMultiCreation(new MultiSelectFirstUntilOtherOp<>(upstream, ticks));
    }

    /**
     * Selects the items where the given predicate returns {@code true}.
     * It calls the predicates for each items.
     * Each item for which the predicates returned {@code true} is emitted by the produced {@link Multi}.
     * Others are dropped.
     * <p>
     * If the upstream {@link Multi} is empty, the produced {@link Multi} is empty.
     * If the upstream {@link Multi} is emitting a failure, the failure is emitted by the produced {@link Multi}.
     * If the predicates throws an exception while testing an item, the produced {@link Multi} emits that exception as
     * failure. No more items will be tested or emitted.
     * If the predicates returns {@code true} for each items from upstream, all the items are selected.
     * The produced {@link Multi} completes when the upstream completes.
     *
     * @param predicate the predicate to test the items, must not be {@code null}
     * @return the resulting {@link Multi}
     * @see #when(Function)
     */
    @CheckReturnValue
    public Multi<T> where(Predicate<? super T> predicate) {
        Predicate<? super T> actual = Infrastructure.decorate(nonNull(predicate, "predicate"));
        return Infrastructure.onMultiCreation(new MultiSelectWhereOp<>(upstream, actual));
    }

    /**
     * Like {@link #when(Function)}, but select at most {@code limit} items.
     *
     * @param predicate the predicate to test the items, must not be {@code null}
     * @param limit the maximum number of item to select, must be positive. 0 would produce an empty {@link Multi}
     * @return the resulting {@link Multi}
     * @see #when(Function)
     */
    @CheckReturnValue
    public Multi<T> where(Predicate<? super T> predicate, int limit) {
        // Decoration happens in where.
        return where(predicate)
                .select().first(limit);
    }

    /**
     * Selects the items where the given function produced a {@link Uni} emitting {@code true}.
     * This method is the asynchronous version of {@link #where(Predicate)}.
     * Instead of a synchronous predicate, it accepts a function producing {@link Uni}.
     * It calls the function for every item, and depending of the produced {@link Uni}, it emits the item downstream or
     * drops it. If the returned {@link Uni} produces {@code true}, the item is selected and emitted by the produced
     * {@link Multi}, otherwise the item is dropped.
     * The item is only emitted when {@link Uni} produced for that item emits {@code true}.
     * <p>
     * If the upstream {@link Multi} is empty, the produced {@link Multi} is empty.
     * If the upstream {@link Multi} is emitting a failure, the failure is emitted by the produced {@link Multi}.
     * If the function throws an exception while testing an item, the produced {@link Multi} emits that exception as
     * failure. No more items will be tested or emitted.
     * If the function produced a {@code null} Uni, the produced {@link Multi} emits an {@link NullPointerException} as
     * failure. No more items will be tested or emitted.
     * If the function produced a failing Uni, the produced {@link Multi} emits that failure. No more items will be
     * tested or emitted.
     * If the function produced a {@link Uni} emitting {@code null}, the produced {@link Multi} emits a failure.
     * No more items will be tested or emitted.
     * If the function accepts all the items from the upstream, all the items are selected.
     * The produced {@link Multi} completes when the upstream completes.
     * <p>
     * This method preserves the item orders.
     *
     * @param predicate the function to test the items, must not be {@code null}, must not produced {@code null}
     * @return the resulting {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> when(Function<? super T, Uni<Boolean>> predicate) {
        Function<? super T, Uni<Boolean>> actual = Infrastructure.decorate(nonNull(predicate, "predicate"));
        return upstream.onItem().transformToMultiAndConcatenate(res -> {
            Uni<Boolean> uni = actual.apply(res);
            return uni.map(pass -> pass ? res : null).toMulti();
        });
    }

    /**
     * Selects all the distinct items from the upstream.
     * This methods uses {@link Object#hashCode()} to compare items.
     * <p>
     * Do NOT call this method on unbounded upstream, as it would lead to an {@link OutOfMemoryError}.
     * <p>
     * If the comparison throws an exception, the produced {@link Multi} fails.
     * The produced {@link Multi} completes when the upstream sends the completion event.
     *
     * @return the resulting {@link Multi}.
     * @see MultiSkip#repetitions()
     * @see #distinct(Comparator)
     */
    @CheckReturnValue
    public Multi<T> distinct() {
        return Infrastructure.onMultiCreation(new MultiDistinctOp<>(upstream));
    }

    /**
     * Selects all the distinct items from the upstream.
     * This methods uses the given comparator to compare the items.
     * <p>
     * Do NOT call this method on unbounded upstream, as it would lead to an {@link OutOfMemoryError}.
     * <p>
     * If the comparison throws an exception, the produced {@link Multi} fails.
     * The produced {@link Multi} completes when the upstream sends the completion event.
     * <p>
     * Unlike {@link #distinct()} which uses a {@link java.util.HashSet} internally, this variant uses a
     * {@link java.util.TreeSet} initialized with the given comparator. If the comparator is {@code null}, it uses a
     * {@link java.util.HashSet} as backend.
     *
     * @param comparator the comparator used to compare items. If {@code null}, it will uses the item's {@code hashCode}
     *        method.
     * @return the resulting {@link Multi}.
     * @see MultiSkip#repetitions()
     */
    @CheckReturnValue
    public Multi<T> distinct(Comparator<? super T> comparator) {
        return Infrastructure.onMultiCreation(new MultiDistinctOp<>(upstream, comparator));
    }

}
