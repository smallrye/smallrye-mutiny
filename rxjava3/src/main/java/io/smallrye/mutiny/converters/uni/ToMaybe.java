package io.smallrye.mutiny.converters.uni;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.reactivex.rxjava3.core.Maybe;
import io.smallrye.mutiny.Uni;

public class ToMaybe<T> implements Function<Uni<T>, Maybe<T>> {
    @SuppressWarnings("rawtypes")
    public static final ToMaybe INSTANCE = new ToMaybe();

    private ToMaybe() {
        // Avoid direct instantiation
    }

    @Override
    public Maybe<T> apply(Uni<T> uni) {
        return Maybe.create(emitter -> {
            CompletableFuture<T> future = uni.subscribe().asCompletionStage();
            emitter.setCancellable(() -> future.cancel(false));
            future.whenComplete((res, fail) -> {
                if (future.isCancelled()) {
                    return;
                }

                if (fail != null) {
                    emitter.onError(fail);
                } else if (res != null) {
                    emitter.onSuccess(res);
                    emitter.onComplete();
                } else {
                    emitter.onComplete();
                }

            });
        });
    }
}
