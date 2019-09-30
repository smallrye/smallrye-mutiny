package io.smallrye.reactive.unimulti.operators.flowable;

import static io.smallrye.reactive.unimulti.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.smallrye.reactive.unimulti.helpers.CancellationSubscriber;

public final class FlowableCollector<T, A, R> extends Flowable<R> {

    private final Publisher<T> upstream;

    private final Collector<? super T, A, ? extends R> collector;

    public FlowableCollector(Publisher<T> upstream, Collector<? super T, A, ? extends R> collector) {
        this.upstream = upstream;
        this.collector = collector;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        A initialValue;
        BiConsumer<A, ? super T> accumulator;
        Function<A, ? extends R> finisher;

        try {
            initialValue = collector.supplier().get();
            accumulator = collector.accumulator();
            finisher = collector.finisher();
        } catch (Exception ex) {
            upstream.subscribe(new CancellationSubscriber<>());
            s.onSubscribe(CANCELLED);
            s.onError(ex);
            return;
        }

        if (initialValue == null) {
            upstream.subscribe(new CancellationSubscriber<>());
            s.onSubscribe(CANCELLED);
            s.onError(new NullPointerException(SUPPLIER_PRODUCED_NULL));
            return;
        }

        if (accumulator == null) {
            upstream.subscribe(new CancellationSubscriber<>());
            s.onSubscribe(CANCELLED);
            s.onError(new NullPointerException("`accumulator` must not be `null`"));
            return;
        }

        upstream.subscribe(new CollectorSubscriber<>(s, initialValue, accumulator, finisher));
    }
}
