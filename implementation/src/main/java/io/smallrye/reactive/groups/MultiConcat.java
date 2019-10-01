package io.smallrye.reactive.groups;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.CompositeException;
import io.smallrye.reactive.operators.MultiCombine;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Creates new {@link Multi} by concatenating several {@link Multi} or {@link Publisher}.
 * This class allows configuring how the concatenation is executed. Unlike a merge, a concatenation emits the items in
 * order. Streams are read one-by-one and the items are emitted in this order.
 */
public class MultiConcat {

    private boolean collectFailures;
    private int requests;

    public MultiConcat(boolean collectFailures, int requests) {
        this.collectFailures = collectFailures;
        this.requests = requests;
    }

    /**
     * Creates a new {@link Multi} concatenating the items emitted by the given {@link Multi multis} /
     * {@link Publisher publishers}.
     *
     * @param publishers the publishers, must not be empty, must not contain {@code null}
     * @param <T>        the type of item
     * @return the new {@link Multi} emitting the items from the given set of {@link Multi} using a concatenation
     */
    public <T> Multi<T> streams(Publisher<T>... publishers) {
        return MultiCombine.concatenate(Arrays.asList(publishers), collectFailures, requests);
    }

    /**
     * Creates a new {@link Multi} concatenating the items emitted by the given {@link Multi multis} /
     * {@link Publisher publishers}..
     *
     * @param iterable the publishers, must not be empty, must not contain {@code null}, must not be {@code null}
     * @param <T>      the type of item
     * @return the new {@link Multi} emitting the items from the given set of {@link Multi} using a concatenation
     */
    public <T> Multi<T> streams(Iterable<? extends Publisher<T>> iterable) {
        List<Publisher<T>> list = new ArrayList<>();
        iterable.forEach(list::add);
        return MultiCombine.concatenate(list, collectFailures, requests);
    }

    /**
     * Indicates the the concatenation process should not propagate the first receive failure, but collect them until
     * all the items from all (non-failing) participants have ben emitted. Then, the failures are propagated downstream
     * (as a {@link CompositeException} if several failures have been received).
     *
     * @return this {@link MultiConcat} configured to collect the failures.
     */
    public MultiConcat collectFailures() {
        this.collectFailures = true;
        return this;
    }

    /**
     * Indicates that the merge process should consumes the different streams using the given {@code request}.
     *
     * @param requests the requests
     * @return this {@link MultiMerge} configured with the given requests
     */
    public MultiConcat withRequests(int requests) {
        this.requests = requests;
        return this;
    }

}
