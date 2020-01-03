package io.smallrye.mutiny.groups;

import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.operators.multi.MultiRepeatOp;

/**
 * Repeatedly subscribes to a given {@link Uni} to generate a {@link Multi}.
 *
 * @param <T> the type of item
 */
public class UniRepeat<T> {

    private final Uni<T> upstream;

    public UniRepeat(Uni<T> upstream) {
        this.upstream = upstream;
    }

    /**
     * Generates an unbounded stream, indefinitely resubscribing to the {@link Uni}.
     * Note that this enforces:
     * <ul>
     * <li>the number of requests coming from the subscriber</li>
     * <li>cancellation</li>
     * <li>failures, that are propagated downstream</li>
     * </ul>
     * <p>
     * The produced {@link Multi} contains the items emitted by the upstream {@link Uni}. After every emission,
     * another subscription is performed on the {@link Uni}, and the item is then propagated. If the {@link Uni}
     * fires a failure, the failure is propagated. If the {@link Uni} fires an empty items, it resubscribes.
     *
     * @return the {@link Multi} containing the items from the upstream {@link Uni}, resubscribed indefinitely.
     */
    public Multi<T> indefinitely() {
        return atMost(Long.MAX_VALUE);
    }

    /**
     * Generates a stream, containing the items from the upstream {@link Uni}, resubscribed at most {@code times} times.
     * <p>
     * Note that this enforces:
     * <ul>
     * <li>the number of requests coming from the subscriber</li>
     * <li>cancellation</li>
     * <li>failures, that are propagated downstream</li>
     * </ul>
     * <p>
     * The produced {@link Multi} contains the items emitted by the upstream {@link Uni}. After every emission,
     * another subscription is performed on the {@link Uni}, and the item is then propagated. If the {@link Uni}
     * fires a failure, the failure is propagated. If the {@link Uni} fires an empty items, it resubscribes.
     * <p>
     * This method is named {@code atMost} because the repeating re-subscription can be stopped if the subscriber
     * cancels its subscription to the produced {@link Multi}.
     *
     * @param times the number of re-subscription, must be strictly positive, 1 is equivalent to {@link Uni#toMulti()}
     * @return the {@link Multi} containing the items from the upstream {@link Uni}, resubscribed at most {@code times}
     *         times.
     */
    public Multi<T> atMost(long times) {
        long actual = ParameterValidation.positive(times, "times");
        return new MultiRepeatOp<>(upstream.toMulti(), actual);
    }

    /**
     * Generates a stream, containing the items from the upstream {@link Uni}, resubscribed until the given predicate
     * returns {@code true}. The predicate is not called on {@code null} item. If you want to intercept this case,
     * use a sentinel value.
     *
     * @param predicate the predicate, must not be {@code null}
     * @return the {@link Multi} containing the items from the upstream {@link Uni}, resubscribed until the predicate
     *         returns {@code true}.
     */
    public Multi<T> until(Predicate<T> predicate) {
        return new MultiRepeatOp<>(upstream.toMulti(), ParameterValidation.nonNull(predicate, "predicated"));
    }

}
