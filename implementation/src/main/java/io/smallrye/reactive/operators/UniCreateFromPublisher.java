package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class UniCreateFromPublisher<O> extends UniOperator<Void, O> {
    private final Publisher<? extends O> publisher;

    public UniCreateFromPublisher(Publisher<? extends O> publisher) {
        super(null);
        this.publisher = nonNull(publisher, "publisher");
    }

    @SuppressWarnings("SubscriberImplementation")
    @Override
    protected void subscribing(UniSerializedSubscriber<? super O> subscriber) {
        AtomicReference<Subscription> reference = new AtomicReference<>();
        publisher.subscribe(new Subscriber<O>() {
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
            public void onNext(O o) {
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
                Subscription sub = reference.getAndSet(CANCELLED);
                if (sub == CANCELLED) {
                    // Already cancelled, do nothing
                    return;
                }
                sub.cancel();
                subscriber.onFailure(t);
            }

            @Override
            public void onComplete() {
                Subscription sub = reference.getAndSet(CANCELLED);
                if (sub == CANCELLED) {
                    // Already cancelled, do nothing
                    return;
                }
                sub.cancel();
                subscriber.onItem(null);
            }
        });
    }
}
