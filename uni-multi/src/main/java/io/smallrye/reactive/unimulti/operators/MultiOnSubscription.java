package io.smallrye.reactive.unimulti.operators;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;

import org.reactivestreams.Subscription;

import io.reactivex.Flowable;
import io.smallrye.reactive.unimulti.Multi;

public class MultiOnSubscription<T> extends MultiOperator<T, T> {
    private final Consumer<? super Subscription> consumer;

    public MultiOnSubscription(Multi<T> upstream, Consumer<? super Subscription> consumer) {
        super(nonNull(upstream, "upstream"));
        this.consumer = nonNull(consumer, "consumer");
    }

    @Override
    protected Flowable<T> flowable() {
        return upstreamAsFlowable().doOnSubscribe(consumer::accept);
    }
}
