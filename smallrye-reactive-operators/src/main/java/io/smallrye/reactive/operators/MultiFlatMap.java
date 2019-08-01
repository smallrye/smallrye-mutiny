package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;
import org.reactivestreams.Publisher;

import java.util.function.Function;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;
import static io.smallrye.reactive.helpers.ParameterValidation.positive;

public class MultiFlatMap<I, O> extends MultiOperator<I, O> {
    private final Function<? super I, ? extends Publisher<? extends O>> mapper;
    private final int concurrency;
    private final int prefetch;
    private final boolean delayFailurePropagation;
    private final boolean preserveOrdering;

    public MultiFlatMap(Multi<I> upstream, Function<? super I, ? extends Publisher<? extends O>> mapper, int concurrency,
                        int prefetch, boolean delayFailure, boolean preserveOrdering) {
        super(nonNull(upstream, "upstream"));
        this.mapper = nonNull(mapper, "mapper");
        this.concurrency = positive(concurrency, "concurrency");
        this.prefetch = positive(prefetch, "prefetch");
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
    protected Flowable<O> flowable() {
        Flowable<I> flowable = upstreamAsFlowable();

        if (preserveOrdering) {
            if (delayFailurePropagation) {
                return flowable.flatMap(mapper::apply, true, 1, prefetch);
            } else {
                return flowable.concatMap(mapper::apply, prefetch);
            }
        } else {
            return flowable.flatMap(mapper::apply, delayFailurePropagation, concurrency, prefetch);
        }
    }
}
