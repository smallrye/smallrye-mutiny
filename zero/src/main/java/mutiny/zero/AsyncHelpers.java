package mutiny.zero;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface AsyncHelpers {

    /**
     * Creates a failed completion stage.
     *
     * @param t the failure
     * @param <T> the emitted type
     * @return the failed completion stage.
     */
    static <T> CompletionStage<T> failedFuture(Throwable t) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(t);
        return future;
    }

    /**
     * Applies a function on failure to produce another failure.
     *
     * @param upstream the upstream stage.
     * @param mapper the mapper
     * @param <T> the type of emitted item
     * @return the mapped completion stage.
     */
    static <T> CompletionStage<T> applyExceptionally(CompletionStage<T> upstream, Function<Throwable, Throwable> mapper) {
        Objects.requireNonNull(mapper, "The mapper cannot be null");
        CompletableFuture<T> future = new CompletableFuture<>();
        upstream.whenComplete((res, failure) -> {
            if (failure == null) {
                future.complete(res);
            } else {
                Throwable throwable;
                try {
                    throwable = Objects.requireNonNull(mapper.apply(failure));
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    return;
                }
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    /**
     * Composes the given completion stage on failure.
     *
     * @param upstream the upstream stage
     * @param mapper the mapper
     * @param <T> the type of emitted item
     * @return the composed completion stage.
     */
    static <T> CompletionStage<T> composeExceptionally(CompletionStage<T> upstream,
            Function<Throwable, CompletionStage<T>> mapper) {
        Objects.requireNonNull(mapper, "The mapper cannot be null");
        CompletableFuture<T> future = new CompletableFuture<>();
        upstream.whenComplete((res, failure) -> {
            if (failure == null) {
                future.complete(res);
            } else {
                CompletionStage<T> cs;
                try {
                    cs = Objects.requireNonNull(mapper.apply(failure));
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    return;
                }
                cs.whenComplete((result, err) -> {
                    if (err != null) {
                        future.completeExceptionally(err);
                    } else {
                        future.complete(result);
                    }
                });
            }
        });
        return future;
    }
}
