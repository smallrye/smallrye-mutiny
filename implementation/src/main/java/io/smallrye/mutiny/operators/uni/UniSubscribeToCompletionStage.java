package io.smallrye.mutiny.operators.uni;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.Cancellable;

public class UniSubscribeToCompletionStage {

    public static <T> CompletableFuture<T> subscribe(Uni<T> uni, Context context) {
        final AtomicReference<Cancellable> cancellable = new AtomicReference<>();

        CompletableFuture<T> future = new CompletableFuture<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean cancelled = super.cancel(mayInterruptIfRunning);
                if (cancelled) {
                    Cancellable c = cancellable.get();
                    if (c != null) {
                        c.cancel();
                    }
                }
                return cancelled;
            }
        };

        cancellable.set(uni.subscribe().with(context, future::complete, future::completeExceptionally));
        return Infrastructure.wrapCompletableFuture(future);
    }
}
