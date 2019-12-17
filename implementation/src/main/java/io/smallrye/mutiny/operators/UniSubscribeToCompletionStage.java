package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniSubscribeToCompletionStage {

    public static <T> CompletableFuture<T> subscribe(Uni<T> uni) {
        final AtomicReference<UniSubscription> ref = new AtomicReference<>();

        CompletableFuture<T> future = new CompletableFuture<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean cancelled = super.cancel(mayInterruptIfRunning);
                if (cancelled) {
                    UniSubscription s = ref.getAndSet(CANCELLED);
                    if (s != null) {
                        s.cancel();
                    }
                }
                return cancelled;
            }
        };

        uni.subscribe().withSubscriber(new UniSubscriber<T>() {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                if (!ref.compareAndSet(null, subscription)) {
                    future.completeExceptionally(new IllegalStateException(
                            "Invalid subscription state - Already having an upstream subscription"));
                }
            }

            @Override
            public void onItem(T item) {
                if (ref.getAndSet(CANCELLED) != CANCELLED) {
                    future.complete(item);
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                if (ref.getAndSet(CANCELLED) != CANCELLED) {
                    future.completeExceptionally(failure);
                }
            }
        });
        return Infrastructure.wrapCompletableFuture(future);
    }

}
