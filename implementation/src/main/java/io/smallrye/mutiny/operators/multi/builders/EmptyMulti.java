package io.smallrye.mutiny.operators.multi.builders;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

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
    public void subscribe(MultiSubscriber<? super Object> actual) {
        Subscriptions.complete(actual);
    }

}
