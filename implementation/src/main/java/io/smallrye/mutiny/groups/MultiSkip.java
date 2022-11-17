package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positiveOrZero;

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
 * Skips items from the upstream {@link Multi}.
 *
 * @param <T> the type of item
 * @see MultiSelect
 */
public class MultiSkip<T> {

    private final Multi<T> upstream;

    public MultiSkip(Multi<T> upstream) {
        this.upstream = upstream;
    }

    /**
     * Skips the first {@code n} items from the upstream and emits all the other items.
     * <p>
     * If the n is 0, all the items from the upstreams are emitted.
     * If the upstream completes, before emitting n items, the produced {@link Multi} is empty.
     * If the upstream fails, before emitting n items, the produced {@link Multi} fails with the same failure.
     * If the upstream fails, after having emitted n+ items, the produced {@link Multi} would emit the items
     * followed by the failure.
     *
     * @param n the number of item to skip, must be positive.
     * @return the resulting {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> first(long n) {
        return Infrastructure.onMultiCreation(new MultiSkipFirstOp<>(upstream, positiveOrZero(n, "n")));
    }

    /**
     * Skips the first item from the upstream and emits all the other items.
     * <p>
     * If the upstream completes, before emitting an item, the produced {@link Multi} is empty.
     * If the upstream fails, before emitting an item, the produced {@link Multi} fails with the same failure.
     * If the upstream fails, after having emitted an item, the produced {@link Multi} would emit the items
     * followed by the failure.
     *
     * @return the resulting {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> first() {
        return first(1);
    }

    /**
     * Skips the first items passing the given {@code predicate} from the upstream.
     * It calls the predicates for the first items from the upstream until the predicate returns {@code false}.
     * Then, that items and all the remaining items are propagated downstream.
     * <p>
     * If the predicate always returns {@code true}, the produced {@link Multi} is empty.
     * If the predicate always returns {@code false}, the produced {@link Multi} emits all the items from the upstream,
     * and the predicates is called only once on the first item.
     * If the upstream completes, and the predicate didn't return {@code false} yet, the produced {@link Multi} is empty.
     * If the upstream fails, and the predicate didn't return {@code false} yet, the produced {@link Multi} fails with
     * the same failure.
     * If the predicate throws an exception while testing an item, the produced {@link Multi} emits the exception as
     * failure.
     *
     * @param predicate the predicate to test the item. Once the predicate returns {@code false} for an item, this item
     *        and all the remaining items are propagated downstream.
     * @return the resulting {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> first(Predicate<? super T> predicate) {
        Predicate<? super T> actual = Infrastructure.decorate(nonNull(predicate, "predicate"));
        return Infrastructure.onMultiCreation(new MultiSkipFirstUntilOp<>(upstream, actual));
    }

    /**
     * Skips the the items from the upstream emitted during the the given {@code duration}.
     * The duration is computed from the subscription time.
     * <p>
     * If the upstream completes before the given {@code duration}, the produced {@link Multi} is empty.
     * If the upstream fails before the given {@code duration}, the produced {@link Multi} fails with the same failure.
     * If the upstream didn't emit any items before the delay expired, the produced {@link Multi} emits the same events
     * as the upstream.
     *
     * @param duration the duration for which the items from upstream are skipped. Must be strictly positive.
     * @return the resulting {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> first(Duration duration) {
        Multi<Long> ticks = Multi.createFrom().ticks().startingAfter(duration).every(duration);
        return Infrastructure.onMultiCreation(new MultiSkipUntilOtherOp<>(upstream, ticks));
    }

    /**
     * Skips the last {@code n} items from the upstream. All the previous items are emitted by the produced
     * {@link Multi}.
     * <p>
     * If the n is 0, all the items from the upstreams are emitted.
     * If the upstream completes, before emitting n items, the produced {@link Multi} is empty.
     * If the upstream fails, before emitting n items, the produced {@link Multi} fails with the same failure.
     * the produced {@link Multi} would not emit any items.
     * If the upstream fails, after having emitted n items, the produced {@link Multi} would emit the items
     * followed by the failure.
     *
     * @param n the number of item to skip, must be positive.
     * @return the resulting {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> last(int n) {
        return Infrastructure.onMultiCreation(new MultiSkipLastOp<>(upstream, n));
    }

    /**
     * Skips the first items from the upstream. All the previous items are emitted by the produced {@link Multi}.
     * <p>
     * If the upstream completes, before emitting an item, the produced {@link Multi} is empty.
     * If the upstream fails, before emitting an item, the produced {@link Multi} fails with the same failure.
     * the produced {@link Multi} would not emit any items.
     *
     * @return the resulting {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> last() {
        return last(1);
    }

    /**
     * Skips repetitions from the upstream.
     * So, if the upstream emits consecutively the same item twice, it drops the second occurrence.
     * <p>
     * The items are compared using the {@link Object#equals(Object)} method.
     * <p>
     * If the upstream emits a failure, the produced {@link Multi} emits a failure.
     * If the comparison throws an exception, the produced {@link Multi} emits that exception as failure.
     * The produces {@link Multi} completes when the upstream completes.
     * <p>
     * Unlike {@link MultiSelect#distinct()}, this method can be called on unbounded upstream, as it only keeps a
     * reference on the last item.
     *
     * @return the resulting {@link Multi}
     * @see MultiSelect#distinct()
     * @see MultiSkip#repetitions(Comparator)
     */
    @CheckReturnValue
    public Multi<T> repetitions() {
        return Infrastructure.onMultiCreation(new MultiSkipRepetitionsOp<>(upstream));
    }

    /**
     * Skips repetitions from the upstream.
     * So, if the upstream emits consecutively the same item twice, it drops the second occurrence.
     * <p>
     * The items are compared using the given comparator.
     * <p>
     * If the upstream emits a failure, the produced {@link Multi} emits a failure.
     * If the comparison throws an exception, the produced {@link Multi} emits that exception as failure.
     * The produces {@link Multi} completes when the upstream completes.
     * <p>
     * Unlike {@link MultiSelect#distinct()}, this method can be called on unbounded upstream, as it only keeps a
     * reference on the last item.
     *
     * @param comparator the comparator, must not be {@code null}
     * @return the resulting {@link Multi}
     * @see MultiSelect#distinct()
     * @see MultiSkip#repetitions()
     */
    @CheckReturnValue
    public Multi<T> repetitions(Comparator<? super T> comparator) {
        return Infrastructure.onMultiCreation(new MultiSkipRepetitionsOp<>(upstream, comparator));
    }

    /**
     * Skips the items where the given predicate returns {@code true}.
     * It calls the predicates for each items.
     * Each item for which the predicates returned {@code false} is emitted by the produced {@link Multi}.
     * Others are dropped.
     * <p>
     * If the upstream {@link Multi} is empty, the produced {@link Multi} is empty.
     * If the upstream {@link Multi} is emitting a failure, the failure is emitted by the produced {@link Multi}.
     * If the predicates throws an exception while testing an item, the produced {@link Multi} emits that exception as
     * failure. No more items will be tested or emitted.
     * If the predicates returns {@code false} for each items from upstream, all the items are propagated downstream.
     * If the predicates returns {@code true} for each items from upstream, the produce {@link Multi} is empty.
     * The produced {@link Multi} completes when the upstream completes.
     * <p>
     * This function is the opposite of {@link MultiSelect#where(Predicate)} which selects the passing items.
     *
     * @param predicate the predicate to test the items, must not be {@code null}
     * @return the resulting {@link Multi}
     * @see #when(Function)
     * @see MultiSelect#where(Predicate)
     */
    @CheckReturnValue
    public Multi<T> where(Predicate<? super T> predicate) {
        Predicate<? super T> actual = Infrastructure.decorate(nonNull(predicate, "predicate"));
        return upstream.select().where(actual.negate());
    }

    /**
     * Skips the items where the given function produced a {@link Uni} emitting {@code true}.
     * This method is the asynchronous version of {@link #where(Predicate)}.
     * Instead of a synchronous predicate, it accepts a function producing {@link Uni}.
     * It calls the function for every item, and depending of the produced {@link Uni}, it emits the item downstream
     * (when the uni produces {@code false}) or drops it (when the uni produces {@code true}).
     * The item is only emitted when {@link Uni} produced for that item emits {@code false}.
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
            return uni.map(pass -> pass ? null : res).toMulti();
        });
    }
}
