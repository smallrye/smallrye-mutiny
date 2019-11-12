package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;

import org.reactivestreams.Publisher;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.smallrye.reactive.subscription.BackPressureStrategy;
import io.smallrye.reactive.subscription.MultiEmitter;

public class MultiCreateWithEmitter<T> extends MultiOperator<Void, T> {
    private final Consumer<MultiEmitter<? super T>> consumer;
    private final BackPressureStrategy backPressureStrategy;

    public MultiCreateWithEmitter(Consumer<MultiEmitter<? super T>> consumer, BackPressureStrategy strategy) {
        super(null);
        this.consumer = nonNull(consumer, "consumer");
        this.backPressureStrategy = strategy;
    }

    public static BackpressureStrategy convert(BackPressureStrategy strategy) {
        switch (strategy) {
            case BUFFER:
                return BackpressureStrategy.BUFFER;
            case DROP:
                return BackpressureStrategy.DROP;
            case IGNORE:
                return BackpressureStrategy.MISSING;
            case ERROR:
                return BackpressureStrategy.ERROR;
            case LATEST:
                return BackpressureStrategy.LATEST;
            default:
                throw new IllegalArgumentException("Unknown strategy " + strategy);
        }
    }

    @Override
    protected Publisher<T> publisher() {
        return Flowable.create(downstream -> {
            MultiEmitter<T> emitter = new MultiEmitter<T>() {
                @Override
                public MultiEmitter<T> emit(T item) {
                    try {
                        downstream.onNext(item);
                    } catch (Exception downstreamFailure) {
                        downstream.tryOnError(downstreamFailure);
                    }
                    return this;
                }

                @Override
                public void fail(Throwable failure) {
                    downstream.tryOnError(failure);
                }

                @Override
                public void complete() {
                    downstream.onComplete();
                }

                @Override
                public MultiEmitter<T> onTermination(Runnable callback) {
                    nonNull(callback, "callback");
                    downstream.setCancellable(callback::run);
                    return this;
                }
            };
            consumer.accept(emitter);
        }, convert(backPressureStrategy));
    }
}
