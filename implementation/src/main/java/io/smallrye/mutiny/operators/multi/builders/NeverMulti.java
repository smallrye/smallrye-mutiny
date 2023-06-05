package io.smallrye.mutiny.operators.multi.builders;

import java.util.concurrent.Flow.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Represents a publisher that does not emits any signals (items, failures or completion).
 */
public final class NeverMulti extends AbstractMulti<Object> {

    private static final Publisher<Object> NEVER = new NeverMulti();

    /**
     * Returns a parameterized never of this never Publisher.
     *
     * @param <T> the value type
     * @return the {@code multi}
     */
    public static <T> Multi<T> never() {
        return (Multi<T>) NEVER;
    }

    private NeverMulti() {
        // avoid direct instantiation
    }

    @Override
    public void subscribe(MultiSubscriber<? super Object> actual) {
        actual.onSubscribe(Subscriptions.empty());
    }

}
