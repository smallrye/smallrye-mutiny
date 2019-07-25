package io.smallrye.reactive.operators;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.smallrye.reactive.subscription.BackPressureStrategy;
import io.smallrye.reactive.subscription.MultiEmitter;

import java.util.function.Consumer;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class MultiCreateWithEmitter<T> extends MultiOperator<Void, T> {
    private final Consumer<MultiEmitter<? super T>> consumer;
    private final BackPressureStrategy backPressureStrategy;

    public MultiCreateWithEmitter(Consumer<MultiEmitter<? super T>> consumer, BackPressureStrategy strategy) {
        super(null);
        this.consumer = nonNull(consumer, "consumer");
        this.backPressureStrategy = strategy;
    }

    @Override
    protected Flowable<T> flowable() {
        return Flowable.create(downstream -> {
            MultiEmitter<T> emitter = new MultiEmitter<T>() {
                @Override
                public MultiEmitter<T> result(T result) {
                    try {
                        downstream.onNext(result);
                    } catch (Exception downstreamFailure) {
                        downstream.tryOnError(downstreamFailure);
                    }
                    return this;
                }

                @Override
                public void failure(Throwable failure) {
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
}
