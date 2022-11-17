package io.smallrye.mutiny.groups;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Flow;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.MultiCombine;

/**
 * Creates new {@link Multi} by merging several {@link Multi} or {@link Flow.Publisher}.
 * <p>
 * This class allows configuring how the merge is executed. Unlike a concatenation, a merge emits the items as they
 * come, so the items may be interleaved.
 */
public class MultiMerge {

    private final boolean collectFailures;
    private final int requests;
    private final int concurrency;

    MultiMerge(boolean collectFailures, int requests, int concurrency) {
        this.collectFailures = collectFailures;
        this.requests = requests;
        this.concurrency = concurrency;
    }

    /**
     * Creates a new {@link Multi} merging the items emitted by the given {@link Multi multis} /
     * {@link Flow.Publisher publishers}.
     * <p>
     * If you pass no {@code publishers}, the resulting {@link Multi} emits the completion event immediately after subscription.
     * If you pass a single {@code publisher}, the resulting {@link Multi} emits the events from that {@code publisher}.
     * If you pass multiple {@code publishers}, the resulting {@link Multi} emits the events from the {@code publishers},
     * until all the {@code publishers} completes.
     * When the last {@code publisher} completes, it sends the completion event.
     * <p>
     * If any of the {@code publisher} emits a failure, the failure is passed downstream and the merge stops.
     * This behavior can be changed using {@link #collectFailures()}. In this case, the failures are accumulated and
     * would be propagated instead of the final completion event. If multiple failures have been collected, the
     * downstream receives a {@link CompositeException}, otherwise it receives the collected failure.
     * <p>
     * <strong>IMPORTANT:</strong> Unlike concatenation, the order of the {@code publisher} does not matter and items
     * from several upstream {@code publishers} can be interleaved in the resulting {@link Multi}.
     *
     * @param publishers the publishers, can be empty, must not contain {@code null}
     * @param <T> the type of item
     * @return the new {@link Multi} merging the passed {@code publisher}, so emitting the items from these
     *         {@code publishers}.
     */
    @SafeVarargs
    @CheckReturnValue
    public final <T> Multi<T> streams(Flow.Publisher<T>... publishers) {
        return MultiCombine.merge(Arrays.asList(publishers), collectFailures, requests, concurrency);
    }

    /**
     * Creates a new {@link Multi} merging the items emitted by the given {@link Flow.Publisher publishers} /
     * {@link Flow.Publisher publishers}.
     * <p>
     * If you pass no {@code publishers}, the resulting {@link Multi} emits the completion event immediately after subscription.
     * If you pass a single {@code publisher}, the resulting {@link Multi} emits the events from that {@code publisher}.
     * If you pass multiple {@code publishers}, the resulting {@link Multi} emits the events from the {@code publishers},
     * until all the {@code publishers} completes.
     * When the last {@code publisher} completes, it sends the completion event.
     * <p>
     * If any of the {@code publisher} emits a failure, the failure is passed downstream and the merge stops.
     * This behavior can be changed using {@link #collectFailures()}. In this case, the failures are accumulated and
     * would be propagated instead of the final completion event. If multiple failures have been collected, the
     * downstream receives a {@link CompositeException}, otherwise it receives the collected failure.
     * <p>
     * <strong>IMPORTANT:</strong> Unlike concatenation, the order of the {@code publisher} does not matter and items
     * from several upstream {@code publishers} can be interleaved in the resulting {@link Multi}.
     *
     * @param iterable the published, must not be empty, must not contain {@code null}, must not be {@code null}
     * @param <T> the type of item
     * @return the new {@link Multi} emitting the items from the given set of {@link Flow.Publisher}
     */
    @CheckReturnValue
    public <T> Multi<T> streams(Iterable<? extends Flow.Publisher<T>> iterable) {
        List<Flow.Publisher<T>> list = new ArrayList<>();
        iterable.forEach(list::add);
        return MultiCombine.merge(list, collectFailures, requests, concurrency);
    }

    /**
     * Indicates that the merge process should not propagate the first received failure, but collect them until
     * all the items from all (non-failing) participants have been emitted. Then, the failures are propagated downstream
     * (as a {@link CompositeException} if several failures have been received).
     *
     * @return a new {@link MultiMerge} collecting failures
     */
    @CheckReturnValue
    public MultiMerge collectFailures() {
        return new MultiMerge(true, this.requests, this.concurrency);
    }

    /**
     * Indicates that the merge process should consume the different streams using the given {@code request}.
     *
     * @param requests the request
     * @return a new {@link MultiMerge} configured with the given requests
     */
    @CheckReturnValue
    public MultiMerge withRequests(int requests) {
        return new MultiMerge(this.collectFailures, requests, this.concurrency);
    }

    /**
     * Indicates that the merge process can consume up to {@code concurrency} streams concurrently. Items emitted by these
     * streams may be interleaved in the resulting stream.
     *
     * @param concurrency the concurrency
     * @return a new {@link MultiMerge} configured with the given concurrency
     */
    @CheckReturnValue
    public MultiMerge withConcurrency(int concurrency) {
        return new MultiMerge(this.collectFailures, this.requests, concurrency);
    }

}
