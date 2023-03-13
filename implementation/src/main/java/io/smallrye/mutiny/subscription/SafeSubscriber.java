package io.smallrye.mutiny.subscription;

import java.util.Objects;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * Wraps another Subscriber and ensures all onXXX methods conform the protocol
 * (except the requirement for serialized access).
 *
 * @param <T> the value type
 */
public final class SafeSubscriber<T> implements Subscriber<T>, Subscription, ContextSupport {

    /**
     * The actual Subscriber.
     */
    private final Subscriber<? super T> downstream;

    /**
     * The subscription.
     */
    private Subscription upstream;

    /**
     * Flag to check if we have already subscribed.
     */
    private final AtomicBoolean subscribed = new AtomicBoolean(false);

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
    public void onSubscribe(Subscription subscription) {
        if (subscribed.compareAndSet(false, true)) {
            this.upstream = subscription;
            try {
                downstream.onSubscribe(this);
            } catch (Throwable e) {
                done = true;
                // can't call onError because the actual's state may be corrupt at this point
                try {
                    subscription.cancel();
                } catch (Throwable e1) {
                    // ignore it, nothing we can do.
                    Infrastructure.handleDroppedException(e1);
                }
            }
        } else {
            subscription.cancel();
        }
    }

    @Override
    public void onNext(T t) {
        if (done) {
            return;
        }
        if (upstream == null) {
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
            upstream.cancel();
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

        if (upstream == null) {
            Throwable npe = new NullPointerException("Subscription not set!");

            try {
                downstream.onSubscribe(Subscriptions.empty());
            } catch (Throwable e) {
                // can't call onError because the actual's state may be corrupt at this point
                Infrastructure.handleDroppedException(new CompositeException(t, e));
                return;
            }
            try {
                downstream.onError(new CompositeException(t, npe));
            } catch (Throwable e) {
                // nothing we can do.
                Infrastructure.handleDroppedException(new CompositeException(t, npe, e));
            }
            return;
        }

        try {
            downstream.onError(t);
        } catch (Throwable ex) {
            // nothing we can do.
            Infrastructure.handleDroppedException(new CompositeException(t, ex));
        }
    }

    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        done = true;

        if (upstream == null) {
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
            Infrastructure.handleDroppedException(new CompositeException(ex, e));
            return;
        }
        try {
            downstream.onError(ex);
        } catch (Throwable e) {
            // nothing we can do.
            Infrastructure.handleDroppedException(new CompositeException(ex, e));
        }
    }

    @Override
    public void request(long n) {
        try {
            upstream.request(n);
        } catch (Throwable e) {
            try {
                upstream.cancel();
            } catch (Throwable ex) {
                // nothing we can do.
                Infrastructure.handleDroppedException(new CompositeException(e, ex));
            }
        }
    }

    @Override
    public void cancel() {
        try {
            upstream.cancel();
        } catch (Throwable e) {
            // nothing we can do.
            Infrastructure.handleDroppedException(e);
        }
    }

    @Override
    public Context context() {
        if (downstream instanceof ContextSupport) {
            return ((ContextSupport) downstream).context();
        } else {
            return Context.empty();
        }
    }
}
