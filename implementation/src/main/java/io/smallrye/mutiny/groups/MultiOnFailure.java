package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
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
     * Produces a new {@link Multi} invoking the given @{code action} when a {@code failure} event is received.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its result, the result is discarded, and the original {@code failure} is forwarded downstream. If the produced
     * {@code Uni} fails, a {@link io.smallrye.mutiny.CompositeException} composed by the original failure and the
     * caught failure is propagated downstream.
     *
     * If the asynchronous action throws an exception, this exception is propagated downstream as a
     * {@link io.smallrye.mutiny.CompositeException} composed with the original failure and the caught exception.
     *
     * This method preserves the order of the items, meaning that the downstream received the items in the same order
     * as the upstream has emitted them.
     *
     * @param action the function taking the failure and returning a {@link Uni}, must not be {@code null}
     * @return the new {@link Multi}
     */
    @SuppressWarnings("unchecked")
    public Multi<T> invokeUni(Function<Throwable, ? extends Uni<?>> action) {
        ParameterValidation.nonNull(action, "action");
        return recoverWithMulti(failure -> {
            Uni<?> uni = action.apply(failure);
            if (uni == null) {
                throw new NullPointerException("The `action` produced a `null` Uni");
            }
            return (Multi<? extends T>) uni.onItemOrFailure().applyUni((ignored, subFailure) -> {
                if (subFailure != null) {
                    return Uni.createFrom().failure(new CompositeException(failure, subFailure));
                } else {
                    return Uni.createFrom().failure(failure);
                }
            }).toMulti();
        });
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
