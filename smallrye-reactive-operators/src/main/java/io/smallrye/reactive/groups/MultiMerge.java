package io.smallrye.reactive.groups;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.operators.MultiCombine;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Creates new {@link Multi} by merging several {@link Multi} or {@link Publisher}.
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
     * {@link Publisher publishers}.
     *
     * @param publishers the publishers, must not be empty, must not contain {@code null}
     * @param <T>    the type of item
     * @return the new {@link Multi} emitting the items from the given set of {@link Multi}
     */
    public final <T> Multi<T> streams(Publisher<T>... publishers) {
        return MultiCombine.merge(Arrays.asList(publishers), collectFailures, requests, concurrency);
    }

    /**
     * Creates a new {@link Multi} merging the items emitted by the given {@link Publisher publishers} /
     * {@link Publisher publishers}.
     *
     * @param iterable the published, must not be empty, must not contain {@code null}, must not be {@code null}
     * @param <T>        the type of item
     * @return the new {@link Multi} emitting the items from the given set of {@link Publisher}
     */
    public <T> Multi<T> streams(Iterable<Publisher<T>> iterable) {
        List<Publisher<T>> list = new ArrayList<>();
        iterable.forEach(list::add);
        return MultiCombine.merge(list, collectFailures, requests, concurrency);
    }

    /**
     * Indicates the the merge process should not propagate the first receive failure, but collect them until
     * all the items from all (non-failing) participants have ben emitted. Then, the failures are propagated downstream
     * (as a {@link io.smallrye.reactive.CompositeException} if several failures have been received).
     *
     * @return a new {@link MultiMerge} collecting failures
     */
    public MultiMerge collectFailures() {
        return new MultiMerge(true, this.requests, this.concurrency);
    }

    /**
     * Indicates the the merge process should consumes the different streams using the given {@code request}.
     *
     * @return a new {@link MultiMerge} configured with the given requests
     */
    public MultiMerge withRequests(int requests) {
        return new MultiMerge(this.collectFailures, requests, this.concurrency);
    }

    /**
     * Indicates the the merge process can consume up to {@code concurrency} streams in parallel. Items emitted by these
     * streams may be interleaved in the resulting stream.
     *
     * @return a new {@link MultiMerge} configured with the given concurrency
     */
    public MultiMerge withConcurrency(int concurrency) {
        return new MultiMerge(this.collectFailures, this.requests, concurrency);
    }

}
