package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.UniOnItemOrFailureConsume;
import io.smallrye.mutiny.operators.uni.UniOnItemOrFailureFlatMap;
import io.smallrye.mutiny.operators.uni.UniOnItemOrFailureMap;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.mutiny.tuples.Functions;

public class UniOnItemOrFailure<T> {

    private final Uni<T> upstream;

    public UniOnItemOrFailure(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Produces a new {@link Uni} invoking the given callback when the {@code item} or {@code failure} event is fired.
     * Note that the item can be {@code null}, so detecting failures must be done by checking whether the {@code failure}
     * parameter is {@code null}.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> invoke(BiConsumer<? super T, Throwable> callback) {
        BiConsumer<? super T, Throwable> actual = Infrastructure.decorate(nonNull(callback, "callback"));
        return Infrastructure.onUniCreation(
                new UniOnItemOrFailureConsume<>(upstream, actual));
    }

    /**
     * Produces a new {@link Uni} invoking the given callback when the {@code item} or {@code failure} event is fired.
     * The failure or item is being ignore by the callback.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> invoke(Runnable callback) {
        Runnable actual = nonNull(callback, "callback");
        // Decoration happens in `invoke`
        return invoke((ignoredItem, ignoredFailure) -> actual.run());
    }

    /**
     * Produces a new {@link Uni} invoking the given callback when the {@code item} or {@code failure} event is fired.
     * Note that the item can be {@code null}, so detecting failures must be done by checking whether the {@code failure}
     * parameter is {@code null}.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> call(BiFunction<? super T, Throwable, Uni<?>> callback) {
        BiFunction<? super T, Throwable, Uni<?>> actual = Infrastructure.decorate(nonNull(callback, "callback"));
        return transformToUni((res, fail) -> {
            Uni<?> uni = actual.apply(res, fail);
            if (uni == null) {
                throw new NullPointerException("The callback produced a `null` uni");
            }
            return uni
                    .onItemOrFailure().transformToUni((ignored, subFailure) -> {
                        if (fail != null && subFailure != null) {
                            return Uni.createFrom().failure(new CompositeException(fail, subFailure));
                        } else if (fail != null) {
                            return Uni.createFrom().failure(fail);
                        } else if (subFailure != null) {
                            return Uni.createFrom().failure(subFailure);
                        } else {
                            return Uni.createFrom().item(res);
                        }
                    });
        });
    }

    /**
     * Produces a new {@link Uni} invoking the given callback when the {@code item} or {@code failure} event is fired.
     * The failure or item is being ignore by the callback.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> call(Supplier<Uni<?>> callback) {
        Supplier<Uni<?>> actual = Infrastructure.decorate(nonNull(callback, "callback"));
        return call((ignoredItem, ignoredFailure) -> actual.get());
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} fires the {@code item} or
     * {@code failure} event. Note that the item can be {@code null}, so detecting failures must be done by checking
     * whether the {@code failure} parameter is {@code null}.
     * <p>
     * The function receives the item and failure as parameters, and can transform the item or recover from the failure.
     * The returned object is sent downstream as {@code item}.
     * <p>
     * For asynchronous composition, see {@link #transformToUni(BiFunction)}.
     *
     * @param mapper the mapper function, must not be {@code null}
     * @param <R> the type of Uni item
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public <R> Uni<R> transform(BiFunction<? super T, Throwable, ? extends R> mapper) {
        BiFunction<? super T, Throwable, ? extends R> actual = Infrastructure.decorate(nonNull(mapper, "mapper"));
        return Infrastructure.onUniCreation(new UniOnItemOrFailureMap<>(upstream, actual));
    }

    /**
     * Transforms the received item or failure asynchronously, forwarding the events emitted by another {@link Uni}
     * produced by the given {@code mapper}.
     * <p>
     * Note that the item can be {@code null}, so detecting failures must be done by checking whether the {@code failure}
     * parameter is {@code null}.
     * <p>
     * The mapper is called with the item produced by the upstream or the propagated failure. It produces an {@link Uni},
     * possibly using another type of item ({@code R}). It can be used to recover from a failure. The events fired by
     * the produced {@link Uni} are forwarded to the {@link Uni} returned by this method.
     * <p>
     * This operation is generally named {@code flatMap}.
     *
     * @param mapper the function called with the item and failure sent by the upstream {@link Uni} to produce another
     *        {@link Uni}, must not be {@code null}, must not return {@code null}.
     * @param <R> the type of item
     * @return a new {@link Uni} that would fire events from the uni produced by the mapper function, possibly
     *         in an asynchronous manner.
     */
    @CheckReturnValue
    public <R> Uni<R> transformToUni(BiFunction<? super T, Throwable, Uni<? extends R>> mapper) {
        BiFunction<? super T, Throwable, Uni<? extends R>> actual = Infrastructure.decorate(nonNull(mapper, "mapper"));
        return Infrastructure.onUniCreation(new UniOnItemOrFailureFlatMap<>(upstream, actual));
    }

    /**
     * Transforms the received item or failure asynchronously, forwarding the events emitted by the {@link UniEmitter}
     * provided to the given consumer.
     * <p>
     * The consumer is called with the item event or failure event emitted by the current {@link Uni} and an emitter
     * used to fire events downstream.
     * <p>
     * As the item received from upstream can be {@code null}, detecting a failure must be done by checking whether the
     * failure passed to the consumer is {@code null}.
     *
     * @param consumer the function called with the item of the this {@link Uni} and an {@link UniEmitter}.
     *        It must not be {@code null}.
     * @param <R> the type of item emitted by the emitter
     * @return a new {@link Uni} that would fire events from the emitter consumed by the mapper function, possibly
     *         in an asynchronous manner.
     */
    @CheckReturnValue
    public <R> Uni<R> transformToUni(Functions.TriConsumer<? super T, Throwable, UniEmitter<? super R>> consumer) {
        Functions.TriConsumer<? super T, Throwable, UniEmitter<? super R>> actual = Infrastructure
                .decorate(nonNull(consumer, "consumer"));
        return this.transformToUni((item, failure) -> Uni.createFrom().emitter(emitter -> {
            try {
                actual.accept(item, failure, emitter);
            } catch (Throwable e) {
                if (failure != null) {
                    emitter.fail(new CompositeException(failure, e));
                } else {
                    emitter.fail(e);
                }
            }
        }));
    }

}
