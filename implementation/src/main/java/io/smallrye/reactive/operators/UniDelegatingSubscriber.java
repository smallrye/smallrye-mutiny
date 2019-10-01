package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import io.smallrye.reactive.subscription.UniSubscriber;
import io.smallrye.reactive.subscription.UniSubscription;

public class UniDelegatingSubscriber<I, O> implements UniSubscriber<I> {

    private final UniSerializedSubscriber<? super O> delegate;

    public UniDelegatingSubscriber(UniSerializedSubscriber<? super O> subscriber) {
        this.delegate = nonNull(subscriber, "delegate");
    }

    @Override
    public void onSubscribe(UniSubscription subscription) {
        delegate.onSubscribe(subscription);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onItem(I item) {
        delegate.onItem((O) item);
    }

    @Override
    public void onFailure(Throwable failure) {
        delegate.onFailure(failure);
    }
}
