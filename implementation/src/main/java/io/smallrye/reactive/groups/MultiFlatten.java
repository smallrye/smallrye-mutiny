package io.smallrye.reactive.groups;

import static io.smallrye.reactive.helpers.ParameterValidation.positive;

import java.util.function.Function;

import org.reactivestreams.Publisher;

import io.smallrye.reactive.CompositeException;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.queues.SpscArrayQueue;
import io.smallrye.reactive.operators.multi.MultiFlatMapOp;

/**
 * The object to tune the <em>flatMap</em> operation
 *
 * @param <I> the type of item emitted by the upstream {@link Multi}
 * @param <O> the type of item emitted by the returned {@link Multi}
 */
public class MultiFlatten<I, O> {

    private final Function<? super I, ? extends Publisher<? extends O>> mapper;
    private final Multi<I> upstream;

    private final int requests;
    private final boolean collectFailureUntilCompletion;

    MultiFlatten(Multi<I> upstream,
            Function<? super I, ? extends Publisher<? extends O>> mapper,
            int requests, boolean collectFailures) {
        this.upstream = upstream;
        this.mapper = mapper;
        this.requests = requests;
        this.collectFailureUntilCompletion = collectFailures;
    }

    /**
     * Instructs the <em>flatMap</em> operation to consume all the <em>streams</em> returned by the mapper before
     * propagating a failure if any of the <em>stream</em> has produced a failure.
     * <p>
     * If more than one failure is collected, the propagated failure is a
     * {@link CompositeException}.
     *
     * @return this {@link MultiFlatten}
     */
    public MultiFlatten<I, O> collectFailures() {
        return new MultiFlatten<>(upstream, mapper, requests, true);
    }

    /**
     * Configures the number the items requested to the <em>streams</em> produced by the mapper.
     *
     * @param req the request, must be strictly positive
     * @return this {@link MultiFlatten}
     */
    public MultiFlatten<I, O> withRequests(int req) {
        return new MultiFlatten<>(upstream, mapper, positive(req, "req"), collectFailureUntilCompletion);
    }

    /**
     * Produces a {@link Multi} containing the items from {@link Publisher} produced by the {@code mapper} for each
     * item emitted by this {@link Multi}.
     * <p>
     * The operators behaves as follows:
     * <ul>
     * <li>for each item emitted by this {@link Multi}, the mapper is called and produces a {@link Publisher}
     * (potentially a {@code Multi}). The mapper must not return {@code null}</li>
     * <li>The items contained in each of the produced {@link Publisher} are then <strong>merged</strong> in the
     * produced {@link Multi}. The returned object lets you configure the flattening process.</li>
     * </ul>
     *
     * @return the object to configure the {@code flatMap} operation.
     */
    public Multi<O> mergeResults() {
        return new MultiFlatMapOp<>(upstream, mapper, collectFailureUntilCompletion, requests,
                2, () -> new SpscArrayQueue<>(256),
                () -> new SpscArrayQueue<>(256));
    }

    /**
     * Produces a {@link Multi} containing the items from {@link Publisher} produced by the {@code mapper} for each
     * item emitted by this {@link Multi}.
     * <p>
     * The operators behaves as follows:
     * <ul>
     * <li>for each item emitted by this {@link Multi}, the mapper is called and produces a {@link Publisher}
     * (potentially a {@code Multi}). The mapper must not return {@code null}</li>
     * <li>The items contained in each of the produced {@link Publisher} are then <strong>merged</strong> in the
     * produced {@link Multi}. The returned object lets you configure the flattening process.</li>
     * </ul>
     * <p>
     * This method allows configuring the concurrency, i.e. the maximum number of in-flight/subscribed inner streams
     *
     * @param concurrency the concurrency
     * @return the object to configure the {@code flatMap} operation.
     */
    public Multi<O> mergeResults(int concurrency) {
        return new MultiFlatMapOp<>(upstream, mapper, collectFailureUntilCompletion, concurrency,
                2, () -> new SpscArrayQueue<>(256),
                () -> new SpscArrayQueue<>(256));
    }

    /**
     * Produces a {@link Multi} containing the items from {@link Publisher} produced by the {@code mapper} for each
     * item emitted by this {@link Multi}.
     * <p>
     * The operators behaves as follows:
     * <ul>
     * <li>for each item emitted by this {@link Multi}, the mapper is called and produces a {@link Publisher}
     * (potentially a {@code Multi}). The mapper must not return {@code null}</li>
     * <li>The items contained in each of the produced {@link Publisher} are then <strong>concatenated</strong> in the
     * produced {@link Multi}. The returned object lets you configure the flattening process.</li>
     * </ul>
     *
     * @return the object to configure the {@code concatMap} operation.
     */
    public Multi<O> concatenateResults() {
        return new MultiFlatMapOp<>(upstream, mapper, collectFailureUntilCompletion, 1,
                2, () -> new SpscArrayQueue<>(256),
                () -> new SpscArrayQueue<>(256));
    }
}
