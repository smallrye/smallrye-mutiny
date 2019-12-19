package io.smallrye.mutiny.operators.multi.builders;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.MultiSubscriber;

abstract class BaseMultiEmitter<T>
        implements MultiEmitter<T>, Subscription {

    protected final AtomicLong requested = new AtomicLong();
    protected final MultiSubscriber<? super T> downstream;

    private final AtomicReference<Runnable> onTermination;

    private static final Runnable CLEARED = () -> {
    };

    BaseMultiEmitter(MultiSubscriber<? super T> downstream) {
        this.downstream = downstream;
        this.onTermination = new AtomicReference<>();
    }

    @Override
    public void complete() {
        completion();
    }

    protected void completion() {
        if (isCancelled()) {
            return;
        }

        try {
            downstream.onCompletion();
        } finally {
            cleanup();
        }
    }

    protected boolean isCancelled() {
        return onTermination.get() == CLEARED;
    }

    private void cleanup() {
        Runnable action = onTermination.getAndSet(CLEARED);
        if (action != null && action != CLEARED) {
            action.run();
        }
    }

    @Override
    public final void fail(Throwable failure) {
        failed(failure);
    }

    protected void failed(Throwable e) {
        if (e == null) {
            e = new NullPointerException("onError called with null.");
        }
        if (isCancelled()) {
            return;
        }
        try {
            downstream.onFailure(e);
        } finally {
            cleanup();
        }
    }

    @Override
    public final void cancel() {
        cleanup();
        onUnsubscribed();
    }

    void onUnsubscribed() {
        // default is no-op
    }

    @Override
    public final void request(long n) {
        if (n > 0) {
            Subscriptions.add(requested, n);
            onRequested();
        }
    }

    void onRequested() {
        // default is no-op
    }

    @Override
    public MultiEmitter<T> onTermination(Runnable onTermination) {
        Runnable runnable = this.onTermination.getAndSet(onTermination);
        if (runnable != null) {
            runnable.run();
        }
        return this;
    }

    public MultiEmitter<T> serialize() {
        return new SerializedMultiEmitter<>(this);
    }
}
