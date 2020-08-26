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
import io.smallrye.mutiny.subscription.Cancellable;

/**
 * Configures the failure handler.
 * <p>
 * The upstream multi has sent us a failure, this class lets you decide what need to be done in this case. Typically,
 * you can recover with a fallback item ({@link #recoverWithItem(Object)}), or with another Multi
 * ({@link #recoverWithMulti(Multi)}), or simply completes the stream ({@link #recoverWithCompletion()}). You can also
 * retry ({@link #retry()}). Maybe, you just want to look at the failure ({@link #invoke(Consumer)}).
 * <p>
 * You can configure the type of failure on which your handler is called using:
 * 
 * <pre>
 * {@code
 * multi.onFailure(IOException.class).recoverWithItem("boom")
 * multi.onFailure(IllegalStateException.class).recoverWithItem("kaboom")
 * multi.onFailure(NoMoreDataException.class).recoverWithCompletion()
 * multi.onFailure(t -> accept(t)).recoverWithItem("another boom")
 * }
 * </pre>
 *
 * @param <T> the type of item
 */
public class MultiOnFailure<T> {

    private final Multi<T> upstream;
    private final Predicate<? super Throwable> predicate;

    public MultiOnFailure(Multi<T> upstream, Predicate<? super Throwable> predicate) {
        this.upstream = upstream;
        this.predicate = predicate == null ? x -> true : predicate;
    }

    /**
     * Produces a new {@link Multi} invoking the given callback when the upstream {@link Multi} emits a failure
     * (matching the predicate if set).
     * <p>
     * If the callback throws an exception, a {@link io.smallrye.mutiny.CompositeException} is propagated downstream.
     * This exception is composed by the received failure and the thrown exception.
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
                failure -> {
                    if (predicate.test(failure)) {
                        callback.accept(failure);
                    }
                },
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
     * <p>
     * If the asynchronous action throws an exception, this exception is propagated downstream as a
     * {@link io.smallrye.mutiny.CompositeException} composed with the original failure and the caught exception.
     * <p>
     * This method preserves the order of the items, meaning that the downstream received the items in the same order
     * as the upstream has emitted them.
     *
     * @param action the function taking the failure and returning a {@link Uni}, must not be {@code null}
     * @return the new {@link Multi}
     */
    public Multi<T> invokeUni(Function<Throwable, Uni<?>> action) {
        ParameterValidation.nonNull(action, "action");
        return recoverWithMulti(failure -> {
            Uni<?> uni = action.apply(failure);
            if (uni == null) {
                throw new NullPointerException("The `action` produced a `null` Uni");
            }

            return Multi.createFrom().emitter(emitter -> {
                Cancellable cancellable = uni.subscribe()
                        .with(
                                success -> emitter.fail(failure),
                                subFailure -> emitter.fail(new CompositeException(failure, subFailure)));

                emitter.onTermination(cancellable::cancel);
            });

        });
    }

    /**
     * Produces a new {@link Multi} invoking the given function when the current {@link Multi} propagates a failure. The
     * function can transform the received failure into another exception that will be fired as failure downstream.
     *
     * @param mapper the mapper function, must not be {@code null}, must not return {@code null}
     * @return the new {@link Multi}
     * @deprecated use {@link #transform(Function)}
     */
    @Deprecated
    public Multi<T> apply(Function<? super Throwable, ? extends Throwable> mapper) {
        return transform(mapper);
    }

    /**
     * Produces a new {@link Multi} invoking the given function when the current {@link Multi} propagates a failure. The
     * function can transform the received failure into another exception that will be fired as failure downstream.
     *
     * @param mapper the mapper function, must not be {@code null}, must not return {@code null}
     * @return the new {@link Multi}
     */
    public Multi<T> transform(Function<? super Throwable, ? extends Throwable> mapper) {
        return Infrastructure.onMultiCreation(new MultiMapOnFailure<>(upstream, predicate, mapper));
    }

    /**
     * Recovers from the received failure (matching the predicate if set) by using a fallback item.
     *
     * @param fallback the fallback, can be {@code null}
     * @return the new {@link Multi} that would emit the given fallback in case the upstream sends us a failure.
     */
    public Multi<T> recoverWithItem(T fallback) {
        nonNull(fallback, "fallback");
        return recoverWithItem(() -> fallback);
    }

    /**
     * Recovers from the received failure (matching the predicate if set) by using a item generated by the given
     * supplier. The supplier is called when the failure is received.
     * <p>
     * If the supplier throws an exception, a {@link io.smallrye.mutiny.CompositeException} containing both the received
     * failure and the thrown exception is propagated downstream.
     *
     * @param supplier the supplier providing the fallback item. Must not be {@code null}, must not return {@code null}.
     * @return the new {@link Multi} that would emit the produced item in case the upstream sends a failure.
     */
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

    /**
     * Recovers from the received failure (matching the predicate if set) by using a item generated by the given
     * function. The function is called when the failure is received.
     * <p>
     * If the function throws an exception, a {@link io.smallrye.mutiny.CompositeException} containing both the received
     * failure and the thrown exception is propagated downstream.
     *
     * @param function the function providing the fallback item. Must not be {@code null}, must not return {@code null}.
     * @return the new {@link Multi} that would emit the produced item in case the upstream sends a failure.
     */
    public Multi<T> recoverWithItem(Function<? super Throwable, ? extends T> function) {
        nonNull(function, "function");
        return Infrastructure.onMultiCreation(new MultiFlatMapOnFailure<>(upstream, predicate, failure -> {
            T newResult = function.apply(failure);
            return Multi.createFrom().item(newResult);
        }));
    }

    /**
     * Recovers from the received failure (matching the predicate if set) by completing the stream. Upon failure, it
     * sends the completion event downstream.
     *
     * @return the new {@link Multi} that would complete in case the upstream sends a failure.
     */
    public Multi<T> recoverWithCompletion() {
        return recoverWithMulti(Multi.createFrom().empty());
    }

    /**
     * Recovers from the received failure (matching the predicate if set) with another {@link Multi}. This {@code multi}
     * is produced by the given function. The function must not return {@code null}.
     * <p>
     * In case of failure, the downstream switches to the produced {@link Multi}.
     * <p>
     * If the function throws an exception, a {@link io.smallrye.mutiny.CompositeException} containing both the received
     * failure and the thrown exception is propagated downstream.
     *
     * @param function the function providing the fallback Multi. Must not be {@code null}, must not return {@code null}.
     * @return the new {@link Multi} that would emit events from the multi produced by the given function in case the
     *         upstream sends a failure.
     */
    public Multi<T> recoverWithMulti(Function<? super Throwable, Multi<? extends T>> function) {
        return Infrastructure.onMultiCreation(new MultiFlatMapOnFailure<>(upstream, predicate,
                nonNull(function, "function")));
    }

    /**
     * Recovers from the received failure (matching the predicate if set) with another {@link Multi}. This {@code multi}
     * is produced by the given function. The supplier must not return {@code null}.
     * <p>
     * In case of failure, the downstream switches to the produced {@link Multi}.
     * <p>
     * If the supplier throws an exception, a {@link io.smallrye.mutiny.CompositeException} containing both the received
     * failure and the thrown exception is propagated downstream.
     *
     * @param supplier the function supplier the fallback Multi. Must not be {@code null}, must not return {@code null}.
     * @return the new {@link Multi} that would emit events from the multi produced by the given supplier in case the
     *         upstream sends a failure.
     */
    public Multi<T> recoverWithMulti(Supplier<Multi<? extends T>> supplier) {
        return recoverWithMulti(ignored -> supplier.get());
    }

    /**
     * Recovers from the received failure (matching the predicate if set) with another given {@link Multi}.
     * <p>
     * In case of failure, the downstream switches to the given {@link Multi}.
     * <p>
     *
     * @param fallback the fallback Multi. Must not be {@code null}.
     * @return the new {@link Multi} that would emit events from the passed multi in case the upstream sends a failure.
     */
    public Multi<T> recoverWithMulti(Multi<? extends T> fallback) {
        return recoverWithMulti(() -> fallback);
    }

    /**
     * Configures the retry strategy.
     *
     * @return the object to configure the retry.
     */
    public MultiRetry<T> retry() {
        return new MultiRetry<>(upstream);
    }

}
