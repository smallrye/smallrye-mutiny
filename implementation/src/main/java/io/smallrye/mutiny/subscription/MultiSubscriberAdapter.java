package io.smallrye.mutiny.subscription;

import java.util.concurrent.Flow;

import io.smallrye.mutiny.Context;

/**
 * Thin adapter of a {@link Flow.Subscriber} to a Mutiny {@link MultiSubscriber}.
 *
 * @param <T> the elements type
 */
public class MultiSubscriberAdapter<T> implements MultiSubscriber<T>, ContextSupport {

    private final Flow.Subscriber<? super T> downstream;

    public MultiSubscriberAdapter(Flow.Subscriber<? super T> downstream) {
        this.downstream = downstream;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        downstream.onSubscribe(subscription);
    }

    @Override
    public void onItem(T item) {
        downstream.onNext(item);
    }

    @Override
    public void onFailure(Throwable failure) {
        downstream.onError(failure);
    }

    @Override
    public void onCompletion() {
        downstream.onComplete();
    }

    @Override
    public Context context() {
        if (downstream instanceof ContextSupport) {
            return ((ContextSupport) downstream).context();
        } else {
            return Context.empty();
        }
    }
}
