package io.smallrye.reactive.operators.multi.builders;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.Subscriptions;
import io.smallrye.reactive.operators.AbstractMulti;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * Implements a {@link org.reactivestreams.Publisher} which only calls {@code onComplete} immediately after subscription.
 */
public final class EmptyMulti extends AbstractMulti<Object> {

    private static final Multi<Object> EMPTY = new EmptyMulti();

    private EmptyMulti() {
        // avoid direct instantiation
    }

    @SuppressWarnings("unchecked")
    public static <T> Multi<T> empty() {
        return (Multi<T>) EMPTY;
    }

    @Override
    public void subscribe(Subscriber<? super Object> actual) {
        Subscriptions.complete(actual);
    }

    @Override
    protected Publisher<Object> publisher() {
        return this;
    }
}
