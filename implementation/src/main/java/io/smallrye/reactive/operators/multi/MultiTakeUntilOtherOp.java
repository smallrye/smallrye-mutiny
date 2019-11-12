package io.smallrye.reactive.operators.multi;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.helpers.Subscriptions;
import io.smallrye.reactive.subscription.SerializedSubscriber;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Emits items from upstream until another Publisher signals an event (items, failures or completion).
 * Once this signal is received, the subscription to upstream is cancelled.
 *
 * @param <T> the type of item from upstream
 * @param <U> the type of item from the other publisher
 */
public final class MultiTakeUntilOtherOp<T, U> extends AbstractMultiWithUpstream<T, T> {

    private final Publisher<U> other;

    public MultiTakeUntilOtherOp(Multi<? extends T> upstream, Publisher<U> other) {
        super(upstream);
        this.other = ParameterValidation.nonNull(other, "other");
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        TakeUntilMainSubscriber<T> mainSubscriber = new TakeUntilMainSubscriber<>(actual);
        TakeUntilOtherSubscriber<U> otherSubscriber = new TakeUntilOtherSubscriber<>(mainSubscriber);
        other.subscribe(otherSubscriber);
        upstream.subscribe(mainSubscriber);
    }

    static final class TakeUntilOtherSubscriber<U> implements Subscriber<U> {
        final TakeUntilMainSubscriber<?> main;
        boolean once;

        TakeUntilOtherSubscriber(TakeUntilMainSubscriber<?> main) {
            this.main = main;
        }

        @Override
        public void onSubscribe(Subscription s) {
            main.setOtherSubscription(s);
        }

        @Override
        public void onNext(U t) {
            onComplete();
        }

        @Override
        public void onError(Throwable t) {
            if (once) {
                return;
            }
            once = true;
            main.onError(t);
        }

        @Override
        public void onComplete() {
            if (once) {
                return;
            }
            once = true;
            main.onComplete();
        }
    }

    static final class TakeUntilMainSubscriber<T> extends MultiOperatorSubscriber<T, T> {

        private final AtomicReference<Subscription> other = new AtomicReference<>();

        TakeUntilMainSubscriber(Subscriber<? super T> downstream) {
            super(new SerializedSubscriber<>(downstream));
        }

        void setOtherSubscription(Subscription s) {
            if (other.compareAndSet(null, s)) {
                s.request(1);
            } else {
                s.cancel();
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            Subscriptions.cancel(other);
        }

        @Override
        public void onNext(T t) {
            if (!isDone()) {
                downstream.onNext(t);
            }
        }

        @Override
        public void onError(Throwable failure) {
            super.onError(failure);
            Subscriptions.cancel(other);
        }

        @Override
        public void onComplete() {
            super.onComplete();
            Subscriptions.cancel(other);
        }
    }
}
