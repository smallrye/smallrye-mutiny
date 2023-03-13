package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.UniOnFailureFlatMap;
import io.smallrye.mutiny.operators.uni.UniOnFailureTransform;
import io.smallrye.mutiny.operators.uni.UniOnItemConsume;

/**
 * Configures the failure handler.
 * <p>
 * The upstream uni has sent us a failure, this class lets you decide what need to be done in this case. Typically,
 * you can recover with a fallback item ({@link #recoverWithItem(Object)}), or with another Uni
 * ({@link #recoverWithUni(Uni)}). You can also retry ({@link #retry()}). Maybe, you just want to look at the failure
 * ({@link #invoke(Consumer)}).
 * <p>
 * You can configure the type of failure on which your handler is called using:
 *
 * <pre>
 * {@code
 * uni.onFailure(IOException.class).recoverWithItem("boom")
 * uni.onFailure(IllegalStateException.class).recoverWithItem("kaboom")
 * uni.onFailure(t -> accept(t)).recoverWithItem("another boom")
 * }
 * </pre>
 *
 * @param <T> the type of item
 */
public class UniOnFailure<T> {

    private final Uni<T> upstream;
    private final Predicate<? super Throwable> predicate;

    public UniOnFailure(Uni<T> upstream, Predicate<? super Throwable> predicate) {
        this.upstream = upstream;
        this.predicate = predicate == null ? x -> true : predicate;
    }

    /**
     * Produces a new {@link Uni} invoking the given callback when this {@link Uni} emits a failure (matching the
     * predicate if set).
     * <p>
     * If the callback throws an exception, a {@link io.smallrye.mutiny.CompositeException} is propagated downstream.
     * This exception is composed by the received failure and the thrown exception.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> invoke(Consumer<Throwable> callback) {
        Consumer<Throwable> actual = Infrastructure.decorate(nonNull(callback, "callback"));
        return Infrastructure.onUniCreation(
                new UniOnItemConsume<>(upstream, null, actual, predicate));
    }

    /**
     * Produces a new {@link Uni} invoking the given callback when this {@link Uni} emits a failure (matching the
     * predicate if set).
     * <p>
     * If the callback throws an exception, a {@link io.smallrye.mutiny.CompositeException} is propagated downstream.
     * This exception is composed by the received failure and the thrown exception.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> invoke(Runnable callback) {
        Runnable actual = nonNull(callback, "callback");
        // Decoration happens in `invoke`
        return invoke(ignored -> actual.run());
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} propagates a failure
     * (matching the predicate if set). The function can transform the received failure into another exception that will
     * be fired as failure downstream.
     * <p>
     * Produces a new {@link Uni} invoking the given @{code action} when the {@code failure} event is received.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its item, this item is discarded, and the original {@code failure} is forwarded downstream. If the produced
     * {@code Uni} fails, a composite failure containing both the original failure and the failure from the executed
     * action is propagated downstream.
     *
     * @param action the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> call(Function<Throwable, Uni<?>> action) {
        Function<Throwable, Uni<?>> actual = Infrastructure.decorate(nonNull(action, "action"));
        return recoverWithUni(failure -> {
            Uni<?> uni = Objects.requireNonNull(actual.apply(failure), "The `action` produced a `null` uni");
            //noinspection unchecked
            return (Uni<T>) uni
                    .onItem().failWith(ignored -> failure)
                    .onFailure().transform(subFailure -> {
                        if (subFailure != failure) {
                            return new CompositeException(failure, subFailure);
                        } else {
                            return subFailure;
                        }
                    });
        });
    }

    /**
     * Produces a new {@link Uni} invoking the given supplier when the current {@link Uni} propagates a failure
     * (matching the predicate if set). The supplier ignores the failure.
     * <p>
     * Produces a new {@link Uni} invoking the given @{code supplier} when the {@code failure} event is received.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its item, this item is discarded, and the original {@code failure} is forwarded downstream. If the produced
     * {@code Uni} fails, a composite failure containing both the original failure and the failure from the executed
     * supplier is propagated downstream.
     *
     * @param supplier the supplier, must not be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> call(Supplier<Uni<?>> supplier) {
        Supplier<Uni<?>> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return call(ignored -> actual.get());
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} propagates a failure. The
     * function can transform the received failure into another exception that will be fired as failure downstream.
     *
     * @param mapper the mapper function, must not be {@code null}, must not return {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> transform(Function<? super Throwable, ? extends Throwable> mapper) {
        Function<? super Throwable, ? extends Throwable> actual = Infrastructure.decorate(nonNull(mapper, "mapper"));
        return Infrastructure.onUniCreation(new UniOnFailureTransform<>(upstream, predicate, actual));
    }

    /**
     * Recovers from the received failure (matching the predicate if set) by using a fallback item.
     *
     * @param fallback the fallback, can be {@code null}
     * @return the new {@link Uni} that would emit the given fallback in case the upstream sends us a failure.
     */
    @CheckReturnValue
    public Uni<T> recoverWithItem(T fallback) {
        return recoverWithItem(() -> fallback);
    }

    /**
     * Recovers from the received failure (matching the predicate if set) by using a item generated by the given
     * supplier. The supplier is called when the failure is received.
     * <p>
     * If the supplier throws an exception, a {@link io.smallrye.mutiny.CompositeException} containing both the received
     * failure and the thrown exception is propagated downstream.
     *
     * @param supplier the supplier providing the fallback item. Must not be {@code null}, can return {@code null}.
     * @return the new {@link Uni} that would emit the produced item in case the upstream sends a failure.
     */
    @CheckReturnValue
    public Uni<T> recoverWithItem(Supplier<T> supplier) {
        Supplier<T> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return recoverWithItem(ignored -> actual.get());
    }

    /**
     * Recovers from the received failure (matching the predicate if set) by using a item generated by the given
     * function. The function is called when the failure is received.
     * <p>
     * If the function throws an exception, a {@link io.smallrye.mutiny.CompositeException} containing both the received
     * failure and the thrown exception is propagated downstream.
     *
     * @param function the function providing the fallback item. Must not be {@code null}, can return {@code null}.
     * @return the new {@link Uni} that would emit the produced item in case the upstream sends a failure.
     */
    @CheckReturnValue
    public Uni<T> recoverWithItem(Function<? super Throwable, ? extends T> function) {
        Function<? super Throwable, ? extends T> actual = Infrastructure.decorate(nonNull(function, "function"));
        return Infrastructure.onUniCreation(new UniOnFailureFlatMap<>(upstream, predicate, failure -> {
            T newResult = actual.apply(failure);
            return Uni.createFrom().item(newResult);
        }));
    }

    /**
     * Recovers from the received failure (matching the predicate if set) with another {@link Uni}. This {@code uni} is
     * produced by the given function. This {@code uni} can emit an item (potentially {@code null} or a failure. The
     * function must not return {@code null}.
     * <p>
     * If the function throws an exception, a {@link io.smallrye.mutiny.CompositeException} containing both the received
     * failure and the thrown exception is propagated downstream.
     *
     * @param function the function providing the fallback uni. Must not be {@code null}, must not return {@code null}.
     * @return the new {@link Uni} that would emit events from the uni produced by the given function in case the
     *         upstream sends a failure.
     */
    @CheckReturnValue
    public Uni<T> recoverWithUni(Function<? super Throwable, Uni<? extends T>> function) {
        Function<? super Throwable, Uni<? extends T>> actual = Infrastructure.decorate(nonNull(function, "function"));
        return Infrastructure.onUniCreation(
                new UniOnFailureFlatMap<>(upstream, predicate, actual));
    }

    /**
     * Recovers from the received failure (matching the predicate if set) with another {@link Uni}. This {@code uni} is
     * produced by the given supplier. This {@code uni} can emit an item (potentially {@code null} or a failure. The
     * supplier must not return {@code null}.
     * <p>
     * If the supplier throws an exception, a {@link io.smallrye.mutiny.CompositeException} containing both the received
     * failure and the thrown exception is propagated downstream.
     *
     * @param supplier the supplier providing the fallback uni. Must not be {@code null}, must not return {@code null}.
     * @return the new {@link Uni} that would emits events from the uni produced by the given supplier in case the
     *         upstream sends a failure.
     */
    @CheckReturnValue
    public Uni<T> recoverWithUni(Supplier<Uni<? extends T>> supplier) {
        Supplier<Uni<? extends T>> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return recoverWithUni(ignored -> actual.get());
    }

    /**
     * Recovers from the received failure (matching the predicate if set) with another {@link Uni}. This {@code uni}
     * can emit an item (potentially {@code null} or a failure.
     *
     * @param fallback the fallbakc uni, must not be {@code null}
     * @return the new {@link Uni} that would emit events from the uni in case the upstream sends a failure.
     */
    @CheckReturnValue
    public Uni<T> recoverWithUni(Uni<? extends T> fallback) {
        return recoverWithUni(() -> fallback);
    }

    /**
     * Configures the retry strategy.
     *
     * @return the object to configure the retry.
     */
    @CheckReturnValue
    public UniRetry<T> retry() {
        return new UniRetry<>(upstream, predicate);
    }

    /**
     * Recovers from the received failure by ignoring it and emitting a {@code null} item in the resulting {@link Uni}.
     *
     * @return the new {@link Uni} that emits {@code null} on failure
     */
    @CheckReturnValue
    public Uni<T> recoverWithNull() {
        return recoverWithItem(failure -> null);
    }
}
