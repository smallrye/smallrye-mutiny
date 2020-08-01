package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.function.BiPredicate;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * {@link Uni}-API group for {@link UniCache} configuration
 */
public class UniCacheEvents<T> {

    @SuppressWarnings("rawtypes")
    private static final BiPredicate INDEFINITELY_VALIDATOR = (item, failure) -> true;

    @SuppressWarnings("rawtypes")
    private static final BiPredicate ITEM_ONLY_VALIDATOR = (item, failure) -> item != null;

    private final Uni<T> upstream;

    public UniCacheEvents(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Caches the events (item or failure) of this {@link Uni} and replays it for all further
     * {@link io.smallrye.mutiny.subscription.UniSubscriber}.
     *
     * @return the new {@link Uni}. Unlike regular {@link Uni}, re-subscribing to this {@link Uni} does not re-compute
     *         the outcome but replayed the cached events until the end of time.
     */
    public Uni<T> indefinitely() {
        return whilst(INDEFINITELY_VALIDATOR);
    }

    /**
     * Caches the events (item or failure) of this {@link Uni} as long as the validator assures them validity.
     * <p>
     * <b>Remark: the {@link BiPredicate} must not have any side-effects since it blocks subscriptions to the cached Uni</b>
     *
     * @param validator A {@link BiPredicate} consuming the item as well as the failure to check the cache validity.
     * @return the new {@link Uni}. Unlike regular {@link Uni}, re-subscribing to this {@link Uni} does re-compute
     *         the outcome only after the validator {@link BiPredicate} does not succeed, otherwise the cached events are
     *         replayed.
     */
    public Uni<T> whilst(BiPredicate<T, Throwable> validator) {
        return Infrastructure.onUniCreation(new UniCache<>(upstream, validator));
    }

    /**
     * Caches an item retrieved by this {@link Uni} indefinitely and re-subscribe to the upstream on failure.
     * <p>
     * Please note, that all subscriptions may retrieve a failure until an item was successful computed by the upstream.
     * As soon as there is an item present, it is replayed to all subsequent subscribers.
     *
     * @return the new {@link Uni}. Unlike regular {@link Uni}, re-subscribing to this {@link Uni} does re-compute items,
     *         but will re-compute failures on subsequent subscriptions.
     */
    public Uni<T> itemOnly() {
        return whilst(ITEM_ONLY_VALIDATOR);
    }

    /**
     * Caches an item retrieved by this {@link Uni} for a given validity duration.
     * Duration period starts at time a item is retrieved from the upstream, failures will not be cached but a
     * re-subscription to the upstream will be executed on next subscription after a failure was propagated.
     * <p>
     * The first subscription to the cached {@link Uni} after the duration is exceeded will cause a re-subscription
     * (with the potential to failure as is at the first subscription).
     *
     * @param validity a duration how long a computed item should be cached until re-computation.
     * @return the new {@link Uni}. Unlike regular {@link Uni}, re-subscribing to this {@link Uni} does only re-compute items
     *         if the last received item from the upstream is older than the given validity.
     */
    public Uni<T> atMost(Duration validity) {
        return Infrastructure.onUniCreation(new UniCacheNarrowed<>(upstream, validity));
    }
}
