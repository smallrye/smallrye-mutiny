package io.smallrye.reactive.unimulti.operators;

import static io.smallrye.reactive.unimulti.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Publisher;

import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.subscription.UniSubscriber;
import io.smallrye.reactive.unimulti.subscription.UniSubscription;

public class UniToPublisher {

    private UniToPublisher() {
        // avoid direct instantiation.
    }

    public static <T> Publisher<T> adapt(Uni<T> uni) {
        nonNull(uni, "uni");

        // Several important points to note here
        // 1. The subscription on this Uni must be done when we receive a request, not on the subscription
        // 2. The request parameter must be checked to be compliant with Reactive Streams
        // 3. Cancellation can happen 1) before the request (and so the uni subscription); 2) after the request but
        // before the emission; 3) after the emission. In (1) the uni subscription must not happen. In (2), the emission
        // must not happen. In (3), the emission could happen.
        // 4. If the uni item is `null` the stream is completed. If the uni item is not `null`, the stream contains
        // the item and the end of stream signal. In the case of error, the stream propagates the error.
        return subscriber -> {
            AtomicReference<UniSubscription> upstreamSubscription = new AtomicReference<>();

            UniSubscription downstreamSubscription = new UniSubscription() {
                @Override
                public synchronized void request(long n) {
                    if (n <= 0) {
                        subscriber.onError(new IllegalArgumentException("Invalid request"));
                        return;
                    }

                    if (upstreamSubscription.get() == CANCELLED) {
                        return;
                    }

                    // We received a request, we subscribe to the uni
                    uni.subscribe().withSubscriber(new UniSubscriber<T>() {
                        @Override
                        public void onSubscribe(UniSubscription subscription) {
                            if (!upstreamSubscription.compareAndSet(null, subscription)) {
                                subscriber.onError(new IllegalStateException(
                                        "Invalid subscription state - already have a subscription for upstream"));
                            }
                        }

                        @Override
                        public void onItem(T item) {
                            if (upstreamSubscription.getAndSet(CANCELLED) != CANCELLED) {
                                if (item != null) {
                                    subscriber.onNext(item);
                                }
                                subscriber.onComplete();
                            }
                        }

                        @Override
                        public void onFailure(Throwable failure) {
                            if (upstreamSubscription.getAndSet(CANCELLED) != CANCELLED) {
                                subscriber.onError(failure);
                            }
                        }
                    });
                }

                @Override
                public void cancel() {
                    UniSubscription upstream;
                    synchronized (this) {
                        upstream = upstreamSubscription.getAndSet(CANCELLED);
                    }

                    if (upstream != null) {
                        upstream.cancel();
                    }
                }
            };

            subscriber.onSubscribe(downstreamSubscription);
        };
    }
}
