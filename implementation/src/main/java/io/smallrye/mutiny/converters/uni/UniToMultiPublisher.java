package io.smallrye.mutiny.converters.uni;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniToMultiPublisher<T> implements Publisher<T> {

    private final Uni<T> uni;

    public UniToMultiPublisher(Uni<T> uni) {
        this.uni = uni;
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
        downstream.onSubscribe(new UniToMultiSubscription<>(uni, downstream));
    }

    private static class UniToMultiSubscription<T> implements UniSubscription, Subscription, UniSubscriber<T> {

        private final Uni<T> uni;
        private final Subscriber<? super T> downstream;
        private final AtomicReference<UniSubscription> upstream = new AtomicReference<>();

        private UniToMultiSubscription(Uni<T> uni, Subscriber<? super T> downstream) {
            this.uni = uni;
            this.downstream = downstream;
        }

        @Override
        public void cancel() {
            UniSubscription sub;
            synchronized (upstream) {
                sub = upstream.getAndSet(CANCELLED);
            }
            if (sub != null) {
                sub.cancel();
            }
        }

        @Override
        public void request(long n) {
            synchronized (upstream) {
                if (n <= 0L) {
                    downstream.onError(new IllegalArgumentException("Invalid request"));
                    return;
                }
                if (upstream.get() == CANCELLED) {
                    return;
                }
                uni.subscribe().withSubscriber(this);
            }
        }

        @Override
        public void onSubscribe(UniSubscription subscription) {
            if (!upstream.compareAndSet(null, subscription)) {
                downstream.onError(new IllegalStateException(
                        "Invalid subscription state - already have a subscription for upstream"));
            }
        }

        @Override
        public void onItem(T item) {
            if (upstream.getAndSet(CANCELLED) != CANCELLED) {
                if (item != null) {
                    try {
                        downstream.onNext(item);
                    } catch (Throwable failure) {
                        downstream.onError(failure);
                        return;
                    }
                }
                downstream.onComplete();
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (upstream.getAndSet(CANCELLED) != CANCELLED) {
                downstream.onError(failure);
            }
        }
    }
}
