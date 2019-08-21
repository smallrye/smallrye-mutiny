package io.smallrye.reactive.groups;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.operators.AbstractMulti;
import io.smallrye.reactive.subscription.UniSubscriber;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class MultiSubscribe<T> {


    private final AbstractMulti<T> upstream;

    public MultiSubscribe(AbstractMulti<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Subscribes to the {@link io.smallrye.reactive.Multi} to get a subscription and then start receiving items (
     * based on the passed requests).
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link Subscription}.
     * Each {@link Subscription} will work for only a single {@link Subscriber}. A {@link Subscriber} should
     * only subscribe once to a single {@link Uni}.
     * <p>
     * If the {@link Uni} rejects the subscription attempt or otherwise fails it will fire a {@code failure} event
     * receiving by {@link UniSubscriber#onFailure(Throwable)}.
     *
     * @param subscriber the subscriber, must not be {@code null}
     * @param <S>        the subscriber type
     * @return the passed subscriber
     */
    @SuppressWarnings("SubscriberImplementation")
    public <S extends Subscriber<? super T>> S withSubscriber(S subscriber) {
        upstream.subscribe(subscriber);
        return subscriber;
    }

}
