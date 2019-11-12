package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;
import static io.smallrye.reactive.helpers.ParameterValidation.positive;

import java.util.function.Function;

import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.queues.SpscArrayQueue;
import io.smallrye.reactive.helpers.queues.SpscLinkedArrayQueue;
import io.smallrye.reactive.operators.multi.MultiFlatMapOp;

public class MultiFlatMap<I, O> extends MultiOperator<I, O> {
    private final Function<? super I, ? extends Publisher<? extends O>> mapper;
    private final int concurrency;
    private final int prefetch;
    private final boolean delayFailurePropagation;
    private final boolean preserveOrdering;

    public MultiFlatMap(Multi<I> upstream, Function<? super I, ? extends Publisher<? extends O>> mapper,
            int concurrency,
            int requests, boolean delayFailure, boolean preserveOrdering) {
        super(nonNull(upstream, "upstream"));
        this.mapper = nonNull(mapper, "mapper");
        this.concurrency = positive(concurrency, "concurrency");
        this.prefetch = positive(requests, "requests");
        this.delayFailurePropagation = delayFailure;
        this.preserveOrdering = preserveOrdering;

        if (preserveOrdering) {
            if (this.concurrency > 1) {
                throw new IllegalArgumentException("`preserveOrdering` cannot be enabled when `concurrency` is " +
                        "more than 1");
            }
        }
    }

    @Override
    protected Publisher<O> publisher() {
        Flowable<I> flowable = upstreamAsFlowable();

        // TODO Compute queue size based on concurrency, and requests
        if (preserveOrdering) {
            if (delayFailurePropagation) {
                return new MultiFlatMapOp<>(upstream(), mapper, true, 1, prefetch,
                        () -> new SpscArrayQueue<>(8),
                        () -> new SpscLinkedArrayQueue<>(32));
            } else {
                return flowable.concatMap(mapper::apply, prefetch);
            }
        } else {
            return new MultiFlatMapOp<>(upstream(), mapper, delayFailurePropagation, concurrency, prefetch,
                    () -> new SpscArrayQueue<>(32),
                    () -> new SpscLinkedArrayQueue<>(32));
        }
    }
}
