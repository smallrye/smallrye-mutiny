package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;
import static io.smallrye.mutiny.helpers.Subscriptions.empty;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Execute a given callback on subscription.
 * <p>
 * The subscription is only sent downstream once the action completes successfully. If the action fails, the failure is
 * propagated downstream.
 *
 * @param <T> the value type
 */
public final class MultiOnSubscribeCall<T> extends AbstractMultiOperator<T, T> {

    private final Function<? super Flow.Subscription, Uni<?>> onSubscribe;

    public MultiOnSubscribeCall(Multi<? extends T> upstream,
            Function<? super Flow.Subscription, Uni<?>> onSubscribe) {
        super(upstream);
        this.onSubscribe = onSubscribe;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        if (actual == null) {
            throw new NullPointerException("Subscriber must not be `null`");
        }
        upstream.subscribe().withSubscriber(new OnSubscribeSubscriber(actual));
    }

    private final class OnSubscribeSubscriber extends MultiOperatorProcessor<T, T> {

        private final ReentrantLock lock = new ReentrantLock();
        private Throwable failure;
        private boolean terminatedEarly;
        private boolean uniHasTerminated;

        OnSubscribeSubscriber(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Flow.Subscription s) {
            if (compareAndSetUpstreamSubscription(null, s)) {
                try {
                    Uni<?> uni = Objects.requireNonNull(onSubscribe.apply(s), "The produced Uni must not be `null`");
                    uni.subscribe().with(
                            context(),
                            ignored -> uniCompleted(),
                            err -> uniFailed(err));
                } catch (Throwable e) {
                    Subscriptions.fail(downstream, e);
                    getAndSetUpstreamSubscription(CANCELLED).cancel();
                }
            } else {
                s.cancel();
            }
        }

        /*
         * A note on locks.
         *
         * The methods below use a lock, but most don't use the idiomatic pattern:
         *
         * lock.lock();
         * try {
         * // -- Critical section here --
         * } finally {
         * lock.unlock();
         * }
         *
         * This is being done on purpose, and not just to make sure static analysis tools
         * have something to complain about. If all you do is updating fields, and you don't
         * call any method that might throw, then you can take more freedom.
         *
         * Most notably, we need to make sure that we don't dispatch signals (e.g., onFailure())
         * while we hold a lock.
         */

        @Override
        public void onFailure(Throwable throwable) {
            lock.lock();
            if (!uniHasTerminated) {
                terminatedEarly = true;
                this.failure = throwable;
                lock.unlock();
            } else {
                lock.unlock();
                super.onFailure(throwable);
            }
        }

        @Override
        public void onCompletion() {
            lock.lock();
            if (!uniHasTerminated) {
                terminatedEarly = true;
                lock.unlock();
            } else {
                lock.unlock();
                super.onCompletion();
            }
        }

        private void uniFailed(Throwable failure) {
            getAndSetUpstreamSubscription(CANCELLED).cancel();
            lock.lock();
            try {
                uniHasTerminated = true;
                if (this.failure == null) {
                    this.failure = failure;
                } else {
                    this.failure.addSuppressed(failure);
                }
            } finally {
                lock.unlock();
            }
            Subscriptions.fail(downstream, this.failure);
        }

        private void uniCompleted() {
            lock.lock();
            uniHasTerminated = true;
            lock.unlock();
            if (terminatedEarly) {
                getAndSetUpstreamSubscription(CANCELLED).cancel();
                downstream.onSubscribe(empty());
                if (this.failure != null) {
                    downstream.onFailure(failure);
                } else {
                    downstream.onComplete();
                }
            } else {
                downstream.onSubscribe(this);
            }
        }
    }

}
