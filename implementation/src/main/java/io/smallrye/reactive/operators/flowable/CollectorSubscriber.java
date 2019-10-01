package io.smallrye.reactive.operators.flowable;

import static io.smallrye.reactive.helpers.EmptyUniSubscription.CANCELLED;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@SuppressWarnings("SubscriberImplementation")
public class CollectorSubscriber<T, A, R> implements Subscriber<T>, Subscription {

    private final BiConsumer<A, T> accumulator;

    private final Function<A, R> finisher;
    private final Subscriber<? super R> downstream;
    private final AtomicReference<Subscription> subscription = new AtomicReference<>();
    // Only accessed in the serialized callbacks
    private A intermediate;

    CollectorSubscriber(Subscriber<? super R> downstream,
            A initialValue, BiConsumer<A, T> accumulator, Function<A, R> finisher) {
        this.downstream = downstream;
        this.intermediate = initialValue;
        this.accumulator = accumulator;
        this.finisher = finisher;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (subscription.compareAndSet(null, s)) {
            downstream.onSubscribe(this);
        } else {
            downstream.onSubscribe(CANCELLED);
            downstream.onError(new IllegalStateException("Invalid state, already got a subscriber"));
        }
    }

    @Override
    public void onNext(T t) {
        if (subscription.get() != CANCELLED) {
            try {
                accumulator.accept(intermediate, t);
            } catch (Exception ex) {
                subscription.getAndSet(CANCELLED).cancel();
                downstream.onError(ex);
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        if (subscription.getAndSet(CANCELLED) != CANCELLED) {
            intermediate = null;
            downstream.onError(t);
        }
    }

    @Override
    public void onComplete() {
        if (subscription.getAndSet(CANCELLED) != CANCELLED) {
            R r;

            try {
                r = finisher.apply(intermediate);
            } catch (Exception ex) {
                onError(ex);
                return;
            }

            intermediate = null;
            downstream.onNext(r);
            downstream.onComplete();
        }
    }

    @Override
    public void request(long n) {
        // The subscriber may request only 1 but as we don't know how much we get, we request MAX.
        // This could we changed with call to request in the OnNext
        subscription.get().request(Long.MAX_VALUE);
    }

    @Override
    public void cancel() {
        subscription.getAndSet(CANCELLED).cancel();
    }
}
