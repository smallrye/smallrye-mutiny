package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.SerializedSubscriber;

/**
 * Skips items emitted by the upstream until the other publisher emits either an item or completes.
 *
 * @param <T> the type of items emitted by the upstream (and propagated downstream)
 * @param <U> the type of items emitted by the other publisher
 */
public final class MultiSkipUntilOtherOp<T, U> extends AbstractMultiOperator<T, T> {

    private final Publisher<U> other;

    public MultiSkipUntilOtherOp(Multi<? extends T> upstream, Publisher<U> other) {
        super(upstream);
        this.other = ParameterValidation.nonNull(other, "other");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        SkipUntilMainProcessor<T> main = new SkipUntilMainProcessor<>(actual);
        OtherStreamTracker<U> otherSubscriber = new OtherStreamTracker<>(main);
        other.subscribe(Infrastructure.onMultiSubscription(other, otherSubscriber));
        upstream.subscribe(Infrastructure.onMultiSubscription(upstream, main));
    }

    @SuppressWarnings("SubscriberImplementation")
    static final class OtherStreamTracker<U> implements MultiSubscriber<U>, ContextSupport {

        private final SkipUntilMainProcessor<?> main;

        OtherStreamTracker(SkipUntilMainProcessor<?> main) {
            this.main = main;
        }

        @Override
        public void onSubscribe(Subscription s) {
            main.setOtherSubscription(s);
        }

        @Override
        public void onItem(U t) {
            main.open();
        }

        @Override
        public void onFailure(Throwable t) {
            if (!main.isOpened()) {
                main.onFailure(t);
            }
        }

        @Override
        public void onCompletion() {
            main.open();
        }

        @Override
        public Context context() {
            return main.context();
        }
    }

    static final class SkipUntilMainProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final AtomicReference<Subscription> other = new AtomicReference<>();
        private final AtomicBoolean gate = new AtomicBoolean(false);

        SkipUntilMainProcessor(Subscriber<? super T> downstream) {
            super(new SerializedSubscriber<>(downstream));
        }

        void open() {
            if (gate.compareAndSet(false, true)) {
                Subscriptions.cancel(other);
            }
        }

        boolean isOpened() {
            return gate.get();
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
        public void onItem(T t) {
            if (gate.get()) {
                downstream.onItem(t);
            } else {
                request(1);
            }
        }

        @Override
        public void onFailure(Throwable t) {
            Subscriptions.cancel(other);
            super.onFailure(t);
        }

        @Override
        public void onCompletion() {
            Subscriptions.cancel(other);
            super.onCompletion();
        }
    }
}
