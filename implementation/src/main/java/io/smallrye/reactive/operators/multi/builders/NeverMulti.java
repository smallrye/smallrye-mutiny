package io.smallrye.reactive.operators.multi.builders;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.Subscriptions;
import io.smallrye.reactive.operators.AbstractMulti;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

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
    @SuppressWarnings("unchecked")
    public static <T> Multi<T> never() {
        return (Multi<T>) NEVER;
    }

    private NeverMulti() {
        // avoid direct instantiation
    }

    @Override
    protected Publisher<Object> publisher() {
        return this;
    }

    @Override
    public void subscribe(Subscriber<? super Object> actual) {
        actual.onSubscribe(Subscriptions.CANCELLED);
    }

}
