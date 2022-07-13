package io.smallrye.mutiny.operators.multi.builders;

import java.util.concurrent.Flow;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Implements a {@link Flow.Publisher} which only calls {@code onComplete} immediately after subscription.
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
    public void subscribe(MultiSubscriber<? super Object> downstream) {
        ParameterValidation.nonNullNpe(downstream, "downstream");
        Subscriptions.complete(downstream);
    }

}
