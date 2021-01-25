package io.smallrye.mutiny.operators.uni.builders;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.propagateFailureEvent;

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

    static <O> void forwardFromFuture(Future<? extends O> future,
            UniSubscriber<? super O> subscriber) {
        subscriber.onSubscribe(() -> future.cancel(false));
        if (future.isDone()) {
            O item;
            try {
                item = future.get();
            } catch (ExecutionException e) {
                subscriber.onFailure(e.getCause());
                return;
            } catch (Exception e) {
                subscriber.onFailure(e);
                return;
            }
            subscriber.onItem(item);
        } else {
            // Because future.get is blocking, we must use a separated thread.
            Infrastructure.getDefaultExecutor().execute(() -> {
                try {
                    O item = future.get();
                    subscriber.onItem(item);
                } catch (ExecutionException e) {
                    subscriber.onFailure(e.getCause());
                } catch (Exception e) {
                    subscriber.onFailure(e);
                }
            });
        }
    }

    @Override
    protected void subscribing(UniSubscriber<? super T> subscriber) {
        Future<? extends T> future;
        try {
            future = supplier.get();
        } catch (Throwable e) {
            propagateFailureEvent(subscriber, e);
            return;
        }

        if (future == null) {
            propagateFailureEvent(subscriber, new NullPointerException("The produced Future is `null`"));
            return;
        }

        forwardFromFuture(future, subscriber);
    }
}
