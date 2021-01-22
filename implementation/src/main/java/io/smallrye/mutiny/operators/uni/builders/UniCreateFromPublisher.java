package io.smallrye.mutiny.operators.uni.builders;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniCreateFromPublisher<T> extends AbstractUni<T> {
    private final Publisher<? extends T> publisher;

    public UniCreateFromPublisher(Publisher<? extends T> publisher) {
        this.publisher = nonNull(publisher, "publisher");
    }

    @SuppressWarnings("ReactiveStreamsSubscriberImplementation")
    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        AtomicReference<Subscription> reference = new AtomicReference<>();
        Subscriber<T> actual = new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                if (reference.compareAndSet(null, s)) {
                    subscriber.onSubscribe(() -> {
                        Subscription old = reference.getAndSet(CANCELLED);
                        if (old != null) {
                            old.cancel();
                        }
                    });
                    s.request(1);
                } else {
                    s.cancel();
                }
            }

            @Override
            public void onNext(T o) {
                Subscription sub = reference.getAndSet(CANCELLED);
                if (sub == CANCELLED) {
                    // Already cancelled, do nothing
                    return;
                }
                sub.cancel();
                subscriber.onItem(o);
            }

            @Override
            public void onError(Throwable t) {
                subscriber.onFailure(t);
            }

            @Override
            public void onComplete() {
                subscriber.onItem(null);
            }
        };
        Subscriber<? super T> sub = Infrastructure.onMultiSubscription(publisher, actual);
        publisher.subscribe(sub);
    }
}
