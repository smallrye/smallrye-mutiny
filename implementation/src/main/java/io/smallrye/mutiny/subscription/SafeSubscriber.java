package io.smallrye.mutiny.subscription;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * Wraps another Subscriber and ensures all onXXX methods conform the protocol
 * (except the requirement for serialized access).
 *
 * @param <T> the value type
 */
@SuppressWarnings("SubscriberImplementation")
public final class SafeSubscriber<T> implements Subscriber<T>, Subscription {
    /**
     * The actual Subscriber.
     */
    private final Subscriber<? super T> downstream;
    /**
     * The subscription.
     */
    private final AtomicReference<Subscription> upstream = new AtomicReference<>();
    /**
     * Indicates a terminal state.
     */
    private boolean done;

    /**
     * Constructs a SafeSubscriber by wrapping the given actual Subscriber.
     *
     * @param downstream the actual Subscriber to wrap, not null (not validated)
     */
    public SafeSubscriber(Subscriber<? super T> downstream) {
        this.downstream = downstream;
    }

    /**
     * For testing purpose only.
     * 
     * @return whether the subscriber is in a terminal state.
     */
    boolean isDone() {
        return done;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (upstream.compareAndSet(null, s)) {
            try {
                downstream.onSubscribe(this);
            } catch (Throwable e) {
                done = true;
                // can't call onError because the actual's state may be corrupt at this point
                try {
                    s.cancel();
                } catch (Throwable e1) {
                    // ignore it, nothing we can do.
                    Infrastructure.handleDroppedException(e1);
                }
            }
        } else {
            s.cancel();
        }
    }

    @Override
    public void onNext(T t) {
        if (done) {
            return;
        }
        if (upstream.get() == null) {
            onNextNoSubscription();
            return;
        }

        if (t == null) {
            NullPointerException ex = new NullPointerException("onNext called with null.");
            cancelAndDispatch(ex);
            throw ex;
        }

        try {
            downstream.onNext(t);
        } catch (Throwable e) {
            cancelAndDispatch(e);
        }
    }

    private void cancelAndDispatch(Throwable ex) {
        try {
            upstream.get().cancel();
        } catch (Throwable e1) {
            onError(new CompositeException(ex, e1));
            return;
        }
        onError(ex);
    }

    private void onNextNoSubscription() {
        done = true;
        manageViolationProtocol();
    }

    @Override
    public void onError(Throwable t) {
        Objects.requireNonNull(t);
        if (done) {
            return;
        }
        done = true;

        if (upstream.get() == null) {
            Throwable npe = new NullPointerException("Subscription not set!");

            try {
                downstream.onSubscribe(Subscriptions.empty());
            } catch (Throwable e) {
                // can't call onError because the actual's state may be corrupt at this point
                Infrastructure.handleDroppedException(e);
                return;
            }
            try {
                downstream.onError(new CompositeException(t, npe));
            } catch (Throwable e) {
                // nothing we can do.
                Infrastructure.handleDroppedException(e);
            }
            return;
        }

        try {
            downstream.onError(t);
        } catch (Throwable ex) {
            // nothing we can do.
            Infrastructure.handleDroppedException(ex);
        }
    }

    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        done = true;

        if (upstream.get() == null) {
            onCompleteNoSubscription();
            return;
        }

        try {
            downstream.onComplete();
        } catch (Throwable e) {
            // nothing we can do.
            Infrastructure.handleDroppedException(e);
        }
    }

    private void onCompleteNoSubscription() {

        manageViolationProtocol();
    }

    private void manageViolationProtocol() {
        Throwable ex = new NullPointerException("Subscription not set!");

        try {
            downstream.onSubscribe(Subscriptions.empty());
        } catch (Throwable e) {
            // can't call onError because the actual's state may be corrupt at this point
            Infrastructure.handleDroppedException(e);
            return;
        }
        try {
            downstream.onError(ex);
        } catch (Throwable e) {
            // nothing we can do.
            Infrastructure.handleDroppedException(e);
        }
    }

    @Override
    public void request(long n) {
        try {
            upstream.get().request(n);
        } catch (Throwable e) {
            try {
                upstream.get().cancel();
            } catch (Throwable ignored) {
                // nothing we can do.
                Infrastructure.handleDroppedException(ignored);
            }
        }
    }

    @Override
    public void cancel() {
        try {
            upstream.get().cancel();
        } catch (Throwable ignored) {
            // nothing we can do.
            Infrastructure.handleDroppedException(ignored);
        }
    }
}
