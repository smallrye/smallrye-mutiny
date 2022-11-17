package io.smallrye.mutiny.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow.Publisher;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiConcatOp;

/**
 * Creates new {@link Multi} by concatenating several {@link Multi} or {@link Publisher}.
 * This class allows configuring how the concatenation is executed. Unlike a merge, a concatenation emits the items in
 * order. Streams are read one-by-one and the items are emitted in this order.
 */
public class MultiConcat {

    private boolean collectFailures;

    public MultiConcat(boolean collectFailures) {
        this.collectFailures = collectFailures;
    }

    /**
     * Creates a new {@link Multi} concatenating the items emitted by the given {@link Multi multis} /
     * {@link Publisher publishers}.
     * <p>
     * If you pass no {@code publishers}, the resulting {@link Multi} emits the completion event immediately after subscription.
     * If you pass a single {@code publisher}, the resulting {@link Multi} emits the events from that {@code publisher}.
     * If you pass multiple {@code publishers}, the resulting {@link Multi} emits the events from the first {@code publisher}.
     * When this one emits the completion event, it drops that event and emits the events from the next {@code publisher}
     * and so on until it reaches the last {@code publisher}. When the last {@code publisher} is consumed, it sends the
     * completion event.
     * <p>
     * If any of the {@code publisher} emits a failure, the failure is passed downstream and the concatenation stops.
     * This behavior can be changed using {@link #collectFailures()}. In this case, the failures are accumulated and
     * would be propagated instead of the final completion event. If multiple failures have been collected, the
     * downstream receives a {@link CompositeException}, otherwise it receives the collected failure.
     * <p>
     * <strong>IMPORTANT:</strong> The order of the {@code publisher} matters.
     *
     * @param publishers the publishers, can be empty, must not contain {@code null}
     * @param <T> the type of item
     * @return the new {@link Multi} emitting the items from the given set of {@link Multi} concatenating the
     *         passed {@code publishers}.
     */
    @SafeVarargs
    @CheckReturnValue
    public final <T> Multi<T> streams(Publisher<T>... publishers) {
        return Infrastructure.onMultiCreation(new MultiConcatOp<>(collectFailures, publishers));
    }

    /**
     * Creates a new {@link Multi} concatenating the items emitted by the given {@link Multi multis} /
     * {@link Publisher publishers}.
     * <p>
     * If you pass no {@code publishers}, the resulting {@link Multi} emits the completion event immediately after subscription.
     * If you pass a single {@code publisher}, the resulting {@link Multi} emits the events from that {@code publisher}.
     * If you pass multiple {@code publishers}, the resulting {@link Multi} emits the events from the first {@code publisher}.
     * When this one emits the completion event, it drops that event and emits the events from the second {@code publisher}
     * and so on until it reaches the last {@code publisher}.
     * When the last {@code publisher} is consumed, it sends the completion event.
     * <p>
     * If any of the {@code publisher} emits a failure, the failure is passed downstream and the concatenation stops.
     * This behavior can be changed using {@link #collectFailures()}. In this case, the failures are accumulated and
     * would be propagated instead of the final completion event. If multiple failures have been collected, the
     * downstream receives a {@link CompositeException}, otherwise it receives the collected failure.
     * <p>
     * <strong>IMPORTANT:</strong> The order of the {@code publisher} matters.
     *
     * @param iterable the publishers, can be empty, must not contain {@code null}, must not be {@code null}
     * @param <T> the type of item
     * @return the new {@link Multi} emitting the items from the given set of {@link Multi} concatenating the
     *         passed {@code publishers}.
     */
    @CheckReturnValue
    public <T> Multi<T> streams(Iterable<? extends Publisher<T>> iterable) {
        List<Publisher<T>> list = new ArrayList<>();
        iterable.forEach(list::add);
        //noinspection unchecked
        return Infrastructure.onMultiCreation(new MultiConcatOp<>(collectFailures, list.toArray(new Publisher[0])));
    }

    /**
     * Indicates that the concatenation process should not propagate the first received failure, but collect them until
     * all the items from all (non-failing) participants have been emitted. Then, the failures are propagated downstream
     * (as a {@link CompositeException} if several failures have been received).
     *
     * @return this {@link MultiConcat} configured to collect the failures.
     */
    @CheckReturnValue
    public MultiConcat collectFailures() {
        this.collectFailures = true;
        return this;
    }

}
