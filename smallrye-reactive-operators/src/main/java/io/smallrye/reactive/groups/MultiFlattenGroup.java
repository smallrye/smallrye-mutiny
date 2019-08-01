package io.smallrye.reactive.groups;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.operators.MultiFlatMap;
import org.reactivestreams.Publisher;

import java.util.function.Function;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;
import static io.smallrye.reactive.helpers.ParameterValidation.positive;

public class MultiFlattenGroup<I> {

    private final Multi<I> upstream;
    private final int concurrency;
    private final int prefetch;
    private final boolean delayFailure;
    private final boolean preserveOrdering;

    public MultiFlattenGroup(Multi<I> upstream) {
        this(upstream, 1, 2, false, false);
    }

    public MultiFlattenGroup(Multi<I> upstream,
                             int concurrency, int prefetch, boolean delayFailure, boolean preserveOrdering) {
        this.upstream = upstream;
        this.concurrency = concurrency;
        this.prefetch = prefetch;
        this.delayFailure = delayFailure;
        this.preserveOrdering = preserveOrdering;
    }

    public MultiFlattenGroup<I> withConcurrency(int desiredConcurrency) {
        return new MultiFlattenGroup<>(upstream, positive(desiredConcurrency, "desiredConcurrency"),
                prefetch, delayFailure, preserveOrdering);
    }

    public MultiFlattenGroup<I> delayFailureUntilCompletion() {
        return new MultiFlattenGroup<>(upstream, concurrency,
                prefetch, true, preserveOrdering);
    }

    public MultiFlattenGroup<I> withPrefetch(int prefetch) {
        return new MultiFlattenGroup<>(upstream, concurrency,
                positive(prefetch, "prefetch"), delayFailure, preserveOrdering);
    }

    public <O> Multi<O> mapToMulti(Function<? super I, ? extends Publisher<? extends O>> mapper) {
        return new MultiFlatMap<>(upstream, nonNull(mapper, "mapper"), concurrency, prefetch, delayFailure, preserveOrdering);
    }

    public <O> Multi<O> mapToUni(Function<? super I, ? extends Uni<? extends O>> mapper) {
        nonNull(mapper, "mapper");
        Function<? super I, ? extends Publisher<? extends O>> wrapper = res -> mapper.apply(res).toMulti();
        return new MultiFlatMap<>(upstream, wrapper, concurrency, prefetch, delayFailure, preserveOrdering);
    }


    public MultiFlattenGroup<I> preserveOrdering() {
        return new MultiFlattenGroup<>(upstream, concurrency, prefetch, delayFailure, true);
    }
}
