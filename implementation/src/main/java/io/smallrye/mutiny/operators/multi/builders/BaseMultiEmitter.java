package io.smallrye.mutiny.operators.multi.builders;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongConsumer;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.MultiSubscriber;

abstract class BaseMultiEmitter<T>
        implements MultiEmitter<T>, Flow.Subscription, ContextSupport {

    protected final AtomicLong requested = new AtomicLong();
    protected final MultiSubscriber<? super T> downstream;

    // TODO: switch to volatile + atomic field updaters
    private final AtomicReference<Runnable> onTermination = new AtomicReference<>();
    private final AtomicReference<Runnable> onCancellation = new AtomicReference<>();
    private final AtomicReference<LongConsumer> onRequest = new AtomicReference<>();

    private final AtomicBoolean disposed = new AtomicBoolean();

    private static final Runnable CLEARED = () -> {
    };

    BaseMultiEmitter(MultiSubscriber<? super T> downstream) {
        this.downstream = downstream;
    }

    @Override
    public Context context() {
        if (downstream instanceof ContextSupport) {
            return ((ContextSupport) downstream).context();
        } else {
            return Context.empty();
        }
    }

    @Override
    public long requested() {
        return requested.get();
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

    @Override
    public boolean isCancelled() {
        return onTermination.get() == CLEARED;
    }

    protected void cleanup() {
        disposed.set(true);
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
        if (disposed.compareAndSet(false, true)) {
            cleanup();
            onUnsubscribed();
            Runnable callback = onCancellation.getAndSet(CLEARED);
            if (callback != null && callback != CLEARED) {
                callback.run();
            }
        }
    }

    void onUnsubscribed() {
        // default is no-op
    }

    @Override
    public final void request(long n) {
        if (n > 0) {
            if (disposed.get()) {
                return;
            }
            Subscriptions.add(requested, n);
            onRequested();
            LongConsumer callback = onRequest.get();
            if (callback != null) {
                callback.accept(n);
            }
        } else {
            cancel();
            downstream.onError(Subscriptions.getInvalidRequestException());
        }
    }

    void onRequested() {
        // default is no-op
    }

    @Override
    public MultiEmitter<T> onTermination(Runnable onTermination) {
        ParameterValidation.nonNull(onTermination, "onTermination");
        if (!disposed.get()) {
            this.onTermination.set(onTermination);
            // Re-check if the termination didn't happen in the meantime
            if (disposed.get()) {
                onTermination.run();
            }
        } else {
            onTermination.run();
        }
        return this;
    }

    public MultiEmitter<T> serialize() {
        return new SerializedMultiEmitter<>(this);
    }

    @Override
    public MultiEmitter<T> onRequest(LongConsumer consumer) {
        ParameterValidation.nonNull(consumer, "consumer");
        if (!disposed.get()) {
            this.onRequest.set(consumer);
        }
        return this;
    }

    @Override
    public MultiEmitter<T> onCancellation(Runnable onCancellation) {
        ParameterValidation.nonNull(onCancellation, "onCancellation");
        if (!disposed.get()) {
            this.onCancellation.set(onCancellation);
        }
        return this;
    }
}
