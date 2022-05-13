package io.smallrye.mutiny.operators.uni.builders;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.DONE;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniCreateFromFuture<T> extends AbstractUni<T> {

    private final Supplier<? extends Future<? extends T>> supplier;
    private final Duration timeout;

    public UniCreateFromFuture(Supplier<? extends Future<? extends T>> supplier) {
        this(supplier, null);
    }

    public UniCreateFromFuture(Supplier<? extends Future<? extends T>> supplier, Duration timeout) {
        this.supplier = supplier; // Already checked
        this.timeout = timeout; // Already checked
    }

    @Override
    public void subscribe(UniSubscriber<? super T> downstream) {
        Future<? extends T> future = obtainFuture(downstream);
        if (future == null) {
            return;
        }
        if (future.isDone()) {
            dispatchImmediateResult(future, downstream);
        } else {
            dispatchDeferredResult(future, downstream);
        }
    }

    private Future<? extends T> obtainFuture(UniSubscriber<? super T> downstream) {
        Future<? extends T> future;
        try {
            future = supplier.get();
        } catch (Throwable err) {
            downstream.onSubscribe(DONE);
            downstream.onFailure(err);
            return null;
        }
        if (future == null) {
            downstream.onSubscribe(DONE);
            downstream.onFailure(new NullPointerException("The produced Future is `null`"));
            return null;
        }
        return future;
    }

    private void dispatchImmediateResult(Future<? extends T> future, UniSubscriber<? super T> downstream) {
        T item;
        try {
            item = future.get();
        } catch (ExecutionException e) {
            downstream.onSubscribe(DONE);
            downstream.onFailure(e.getCause());
            return;
        } catch (Throwable err) {
            if (err instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            downstream.onSubscribe(DONE);
            downstream.onFailure(err);
            return;
        }
        downstream.onSubscribe(DONE);
        downstream.onItem(item);
    }

    private void dispatchDeferredResult(Future<? extends T> future, UniSubscriber<? super T> downstream) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        downstream.onSubscribe(() -> {
            cancelled.set(true);
            future.cancel(false);
        });
        // Because future.get is blocking, we must use a separated thread.
        Infrastructure.getDefaultExecutor().execute(() -> {
            try {
                T item;
                if (this.timeout != null) {
                    item = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                } else {
                    item = future.get();
                }
                if (!cancelled.get()) {
                    downstream.onItem(item);
                }
            } catch (ExecutionException e) {
                if (!cancelled.get()) {
                    downstream.onFailure(e.getCause());
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                if (!cancelled.get()) {
                    downstream.onFailure(e);
                }
            }
        });
    }
}
