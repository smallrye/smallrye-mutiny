package io.smallrye.mutiny.operators.uni.builders;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.DONE;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniCreateFromFuture<T> extends AbstractUni<T> {

    private final Supplier<? extends Future<? extends T>> supplier;

    public UniCreateFromFuture(Supplier<? extends Future<? extends T>> supplier) {
        this.supplier = supplier; // Already checked
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
        } catch (Exception err) {
            downstream.onSubscribe(DONE);
            downstream.onFailure(err);
            return;
        }
        downstream.onSubscribe(DONE);
        downstream.onItem(item);
    }

    private void dispatchDeferredResult(Future<? extends T> future, UniSubscriber<? super T> downstream) {
        downstream.onSubscribe(() -> future.cancel(false));
        // Because future.get is blocking, we must use a separated thread.
        Infrastructure.getDefaultExecutor().execute(() -> {
            try {
                T item = future.get();
                downstream.onItem(item);
            } catch (ExecutionException e) {
                downstream.onFailure(e.getCause());
            } catch (Exception e) {
                downstream.onFailure(e);
            }
        });
    }
}
