package io.smallrye.mutiny.operators.uni;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.Cancellable;

public class UniSubscribeToCompletionStage {

    public static <T> CompletableFuture<T> subscribe(Uni<T> uni, Context context) {
        final AtomicReference<Cancellable> cancellable = new AtomicReference<>();

        CompletableFuture<T> future = Infrastructure.wrapCompletableFuture(new CompletableFuture<T>());
        future.whenComplete((val, x) -> {
            if (x instanceof CancellationException) {
                // forward the cancellation to the uni
                if (future.isCancelled()) {
                    Cancellable c = cancellable.get();
                    if (c != null) {
                        c.cancel();
                    }
                }
            }
        });
        cancellable.set(uni.subscribe().with(context, future::complete, future::completeExceptionally));
        // We return future here and not whatever is returned from future.whenComplete, because that
        // new stage will wrap any exceptions into a CompletionException which we do not want, and
        // is exposed by UniOrTest (at least)
        return future;
    }
}
