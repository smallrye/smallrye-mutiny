package io.smallrye.mutiny.subscription;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import io.smallrye.mutiny.Context;

public class UniDelegatingSubscriber<I, O> implements UniSubscriber<I> {

    private final UniSubscriber<? super O> delegate;

    public UniDelegatingSubscriber(UniSubscriber<? super O> subscriber) {
        this.delegate = nonNull(subscriber, "delegate");
    }

    @Override
    public Context context() {
        return delegate.context();
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
