package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniOnItemOrFailureConsume;
import io.smallrye.mutiny.operators.UniOnItemOrFailureFlatMap;
import io.smallrye.mutiny.operators.UniOnItemOrFailureMap;
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
    public Uni<T> invoke(BiConsumer<? super T, Throwable> callback) {
        return Infrastructure.onUniCreation(
                new UniOnItemOrFailureConsume<>(upstream, nonNull(callback, "callback")));
    }

    /**
     * Produces a new {@link Uni} invoking the given callback when the {@code item} or {@code failure} event is fired.
     * Note that the item can be {@code null}, so detecting failures must be done by checking whether the {@code failure}
     * parameter is {@code null}.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> invokeUni(BiFunction<? super T, Throwable, ? extends Uni<?>> callback) {
        ParameterValidation.nonNull(callback, "callback");
        return produceUni((res, fail) -> {
            Uni<?> uni = callback.apply(res, fail);
            if (uni == null) {
                throw new NullPointerException("The callback produced a `null` uni");
            }
            return uni
                    .onItemOrFailure().produceUni((ignored, subFailure) -> {
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
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} fires the {@code item} or
     * {@code failure} event. Note that the item can be {@code null}, so detecting failures must be done by checking
     * whether the {@code failure} parameter is {@code null}.
     * <p>
     * The function receives the item and failure as parameters, and can transform the item or recover from the failure.
     * The returned object is sent downstream as {@code item}.
     * <p>
     * For asynchronous composition, see {@link #produceUni(BiFunction)}.
     *
     * @param mapper the mapper function, must not be {@code null}
     * @param <R> the type of Uni item
     * @return the new {@link Uni}
     */
    public <R> Uni<R> apply(BiFunction<? super T, Throwable, ? extends R> mapper) {
        return Infrastructure.onUniCreation(new UniOnItemOrFailureMap<>(upstream, mapper));
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
    public <R> Uni<R> produceUni(BiFunction<? super T, Throwable, ? extends Uni<? extends R>> mapper) {
        return Infrastructure.onUniCreation(new UniOnItemOrFailureFlatMap<>(upstream, mapper));
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
    public <R> Uni<R> produceUni(Functions.TriConsumer<? super T, Throwable, UniEmitter<? super R>> consumer) {
        nonNull(consumer, "consumer");
        return this.produceUni((item, failure) -> Uni.createFrom().emitter(emitter -> {
            try {
                consumer.accept(item, failure, emitter);
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
