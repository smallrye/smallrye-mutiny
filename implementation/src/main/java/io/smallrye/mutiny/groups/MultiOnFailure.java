package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.MultiFlatMapOnFailure;
import io.smallrye.mutiny.operators.MultiMapOnFailure;
import io.smallrye.mutiny.operators.multi.MultiSignalConsumerOp;

public class MultiOnFailure<T> {

    private final Multi<T> upstream;
    private final Predicate<? super Throwable> predicate;

    public MultiOnFailure(Multi<T> upstream, Predicate<? super Throwable> predicate) {
        this.upstream = upstream;
        this.predicate = predicate == null ? x -> true : predicate;
    }

    /**
     * Produces a new {@link Multi} invoking the given callback when the upstream {@link Multi} emits a failure.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Multi}
     */
    public Multi<T> invoke(Consumer<Throwable> callback) {
        nonNull(callback, "callback");
        nonNull(predicate, "predicate");
        return Infrastructure.onMultiCreation(new MultiSignalConsumerOp<>(
                upstream,
                null,
                null,
                failure -> {
                    if (predicate.test(failure)) {
                        callback.accept(failure);
                    }
                },
                null,
                null,
                null,
                null));
    }

    /**
     * Produces a new {@link Multi} invoking the given function when the current {@link Multi} propagates a failure. The
     * function can transform the received failure into another exception that will be fired as failure downstream.
     *
     * @param mapper the mapper function, must not be {@code null}, must not return {@code null}
     * @return the new {@link Multi}
     */
    public Multi<T> apply(Function<? super Throwable, ? extends Throwable> mapper) {
        return Infrastructure.onMultiCreation(new MultiMapOnFailure<>(upstream, predicate, mapper));
    }

    public Multi<T> recoverWithItem(T fallback) {
        nonNull(fallback, "fallback");
        return recoverWithItem(() -> fallback);
    }

    public Multi<T> recoverWithItem(Supplier<T> supplier) {
        nonNull(supplier, "supplier");
        return recoverWithItem(ignored -> {
            T t = supplier.get();
            if (t == null) {
                throw new NullPointerException(SUPPLIER_PRODUCED_NULL);
            } else {
                return t;
            }
        });
    }

    public Multi<T> recoverWithCompletion() {
        return recoverWithMulti(Multi.createFrom().empty());
    }

    public Multi<T> recoverWithItem(Function<? super Throwable, ? extends T> fallback) {
        nonNull(fallback, "fallback");
        return Infrastructure.onMultiCreation(new MultiFlatMapOnFailure<>(upstream, predicate, failure -> {
            T newResult = fallback.apply(failure);
            return Multi.createFrom().item(newResult);
        }));
    }

    public Multi<T> recoverWithMulti(Function<? super Throwable, ? extends Multi<? extends T>> fallback) {
        return Infrastructure.onMultiCreation(new MultiFlatMapOnFailure<>(upstream, predicate, nonNull(fallback, "fallback")));
    }

    public Multi<T> recoverWithMulti(Supplier<? extends Multi<? extends T>> supplier) {
        return recoverWithMulti(ignored -> supplier.get());
    }

    public Multi<T> recoverWithMulti(Multi<? extends T> fallback) {
        return recoverWithMulti(() -> fallback);
    }

    public MultiRetry<T> retry() {
        return new MultiRetry<>(upstream);
    }

}
