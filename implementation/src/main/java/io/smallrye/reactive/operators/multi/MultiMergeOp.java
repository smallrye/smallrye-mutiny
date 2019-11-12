package io.smallrye.reactive.operators.multi;

import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.operators.AbstractMulti;
import io.smallrye.reactive.operators.multi.builders.CollectionBasedMulti;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Creates a {@link io.smallrye.reactive.Multi} merging the items from an array of Publishers.
 *
 * @param <T> the type of item
 */
public final class MultiMergeOp<T> extends AbstractMulti<T> {

    private final List<Publisher<? extends T>> upstreams;

    private final boolean postponeFailurePropagation;
    private final int maxConcurrency;
    private final int prefetch;

    private final Supplier<? extends Queue<T>> mainQueueSupplier;
    private final Supplier<? extends Queue<T>> innerQueueSupplier;

    public MultiMergeOp(
            List<Publisher<? extends T>> upstreams,
            boolean postponeFailurePropagation,
            int maxConcurrency,
            int prefetch,
            Supplier<? extends Queue<T>> mainQueueSupplier,
            Supplier<? extends Queue<T>> innerQueueSupplier) {
        this.upstreams = Collections.unmodifiableList(
                ParameterValidation.doesNotContainNull(upstreams, "upstreams"));
        this.postponeFailurePropagation = postponeFailurePropagation;
        this.prefetch = ParameterValidation.positive(prefetch, "prefetch");
        this.maxConcurrency = ParameterValidation.positive(maxConcurrency, "maxConcurrency");
        this.mainQueueSupplier =
                ParameterValidation.nonNull(mainQueueSupplier, "mainQueueSupplier");
        this.innerQueueSupplier =
                ParameterValidation.nonNull(innerQueueSupplier, "innerQueueSupplier");
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        MultiFlatMapOp.FlatMapMainSubscriber<Publisher<? extends T>, T> merger = new MultiFlatMapOp.FlatMapMainSubscriber<>(
                actual, Function.identity(), postponeFailurePropagation, maxConcurrency, prefetch, mainQueueSupplier,
                innerQueueSupplier);
        merger.onSubscribe(new CollectionBasedMulti.CollectionSubscription<>(merger, upstreams));
    }

    @Override
    protected Publisher<T> publisher() {
        return this;
    }
}