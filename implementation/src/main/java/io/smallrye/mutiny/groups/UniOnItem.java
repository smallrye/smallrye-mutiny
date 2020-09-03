package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniOnItemConsume;
import io.smallrye.mutiny.operators.UniOnItemTransform;
import io.smallrye.mutiny.operators.UniOnItemTransformToMulti;
import io.smallrye.mutiny.operators.UniOnItemTransformToUni;
import io.smallrye.mutiny.subscription.UniEmitter;

public class UniOnItem<T> {

    private final Uni<T> upstream;

    public UniOnItem(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Produces a new {@link Uni} invoking the given callback when the {@code item} event is fired. Note that the
     * item can be {@code null}.
     * <p>
     * If the callback throws an exception, this exception is propagated to the downstream as failure.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> invoke(Consumer<? super T> callback) {
        return Infrastructure.onUniCreation(
                new UniOnItemConsume<>(upstream, nonNull(callback, "callback"), null, null));
    }

    /**
     * Produces a new {@link Uni} invoking the given callback when the {@code item} event is fired, ignoring its value.
     * <p>
     * If the callback throws an exception, this exception is propagated to the downstream as failure.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> invoke(Runnable callback) {
        Runnable actual = nonNull(callback, "callback");
        return invoke(ignored -> actual.run());
    }

    /**
     * Produces a new {@link Uni} invoking the given @{code action} when the {@code item} event is received. Note that
     * the received item can be {@code null}.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its item, this item is discarded, and the original {@code item} is forwarded downstream. If the produced
     * {@code Uni} fails, the failure is propagated downstream.
     * <p>
     *
     * @param action the function taking the item and returning a {@link Uni}, must not be {@code null}, must not return
     *        {@code null}
     * @return the new {@link Uni}
     * @deprecated Use {@link #call(Function)}
     */
    @Deprecated
    public Uni<T> invokeUni(Function<? super T, Uni<?>> action) {
        return call(action);
    }

    /**
     * Produces a new {@link Uni} invoking the given @{code action} when the {@code item} event is received. Note that
     * the received item can be {@code null}.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its item, this item is discarded, and the original {@code item} is forwarded downstream. If the produced
     * {@code Uni} fails, the failure is propagated downstream.
     * <p>
     *
     * @param action the function taking the item and returning a {@link Uni}, must not be {@code null}, must not return
     *        {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> call(Function<? super T, Uni<?>> action) {
        ParameterValidation.nonNull(action, "action");
        return transformToUni(item -> {
            Uni<?> uni = Objects.requireNonNull(action.apply(item), "The callback produced a `null` uni");
            return uni
                    .onItem().transform(ignored -> item);
        });
    }

    /**
     * Produces a new {@link Uni} invoking the given @{code action} when the {@code item} event is received, ignoring it.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its item, this item is discarded, and the original {@code item} is forwarded downstream. If the produced
     * {@code Uni} fails, the failure is propagated downstream.
     * <p>
     *
     * @param action the action returning a {@link Uni}, must not be {@code null}, must not return {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> call(Supplier<Uni<?>> action) {
        Supplier<Uni<?>> actual = nonNull(action, "action");
        return call(ignored -> actual.get());
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} fires the {@code item} event.
     * The function receives the item as parameter, and can transform it. The returned object is sent downstream
     * as {@code item}.
     * <p>
     * For asynchronous composition, see {@link #transformToUni(Function)}.
     *
     * @param mapper the mapper function, must not be {@code null}
     * @param <R> the type of Uni item
     * @return the new {@link Uni}
     * @deprecated use {@link #transform(Function)}
     */
    @Deprecated
    public <R> Uni<R> apply(Function<? super T, ? extends R> mapper) {
        return transform(mapper);
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} fires the {@code item} event.
     * The function receives the item as parameter, and can transform it. The returned object is sent downstream
     * as {@code item}.
     * <p>
     * For asynchronous composition, see {@link #transformToUni(Function)}.
     *
     * @param mapper the mapper function, must not be {@code null}
     * @param <R> the type of Uni item
     * @return the new {@link Uni}
     */
    public <R> Uni<R> transform(Function<? super T, ? extends R> mapper) {
        return Infrastructure.onUniCreation(new UniOnItemTransform<>(upstream, mapper));
    }

    /**
     * Transforms the received item asynchronously, forwarding the events emitted by another {@link Uni} produced by
     * the given {@code mapper}.
     * <p>
     * The mapper is called with the item event of the current {@link Uni} and produces an {@link Uni}, possibly
     * using another type of item ({@code R}). The events fired by produced {@link Uni} are forwarded to the
     * {@link Uni} returned by this method.
     * <p>
     * This operation is generally named {@code flatMap}.
     *
     * @param mapper the function called with the item of this {@link Uni} and producing the {@link Uni},
     *        must not be {@code null}, must not return {@code null}.
     * @param <R> the type of item
     * @return a new {@link Uni} that would fire events from the uni produced by the mapper function, possibly
     *         in an asynchronous manner.
     */
    public <R> Uni<R> transformToUni(Function<? super T, Uni<? extends R>> mapper) {
        return Infrastructure.onUniCreation(new UniOnItemTransformToUni<>(upstream, mapper));
    }

    /**
     * Transforms the received item asynchronously, forwarding the events emitted by another {@link Uni} produced by
     * the given {@code mapper}.
     * <p>
     * The mapper is called with the item event of the current {@link Uni} and produces an {@link Uni}, possibly
     * using another type of item ({@code R}). The events fired by produced {@link Uni} are forwarded to the
     * {@link Uni} returned by this method.
     * <p>
     * This operation is generally named {@code flatMap}.
     *
     * @param mapper the function called with the item of this {@link Uni} and producing the {@link Uni},
     *        must not be {@code null}, must not return {@code null}.
     * @param <R> the type of item
     * @return a new {@link Uni} that would fire events from the uni produced by the mapper function, possibly
     *         in an asynchronous manner.
     * @deprecated Use {@link #transformToUni(Function)} instead
     */
    @Deprecated
    public <R> Uni<R> produceUni(Function<? super T, Uni<? extends R>> mapper) {
        return transformToUni(mapper);
    }

    /**
     * When this {@code Uni} produces its item (maybe {@code null}), call the given {@code mapper} to produce
     * a {@link Publisher}. Continue the pipeline with this publisher (as a {@link Multi}).
     * <p>
     * The mapper is called with the item event of the current {@link Uni} and produces a {@link Publisher}, possibly
     * using another type of item ({@code R}). Events fired by the produced {@link Publisher} are forwarded to the
     * {@link Multi} returned by this method.
     * <p>
     * This operation is generally named {@code flatMapPublisher}.
     *
     * @param mapper the mapper, must not be {@code null}, may expect to receive {@code null} as item.
     * @param <R> the type of item produced by the resulting {@link Multi}
     * @return the multi
     */
    public <R> Multi<R> transformToMulti(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return Infrastructure.onMultiCreation(new UniOnItemTransformToMulti<>(upstream, mapper));
    }

    /**
     * When this {@code Uni} produces its item (maybe {@code null}), call the given {@code mapper} to produce
     * a {@link Publisher}. Continue the pipeline with this publisher (as a {@link Multi}).
     * <p>
     * The mapper is called with the item event of the current {@link Uni} and produces a {@link Publisher}, possibly
     * using another type of item ({@code R}). Events fired by the produced {@link Publisher} are forwarded to the
     * {@link Multi} returned by this method.
     * <p>
     * This operation is generally named {@code flatMapPublisher}.
     *
     * @param mapper the mapper, must not be {@code null}, may expect to receive {@code null} as item.
     * @param <R> the type of item produced by the resulting {@link Multi}
     * @return the multi
     * @deprecated Use {@link #transformToMulti(Function)}
     */
    @Deprecated
    public <R> Multi<R> produceMulti(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return transformToMulti(mapper);
    }

    /**
     * Transforms the received item asynchronously, forwarding the events emitted by another {@link CompletionStage}
     * produced by the given {@code mapper}.
     * <p>
     * The mapper is called with the item event of the current {@link Uni} and produces an {@link CompletionStage},
     * possibly using another type of item ({@code R}). The events fired by produced {@link CompletionStage} are
     * forwarded to the {@link Uni} returned by this method.
     * <p>
     * *
     *
     * @param mapper the function called with the item of this {@link Uni} and producing the {@link CompletionStage},
     *        must not be {@code null}, must not return {@code null}.
     * @param <R> the type of item
     * @return a new {@link Uni} that would fire events from the uni produced by the mapper function, possibly
     *         in an asynchronous manner.
     * @deprecated Use {@link #transformToUni(Function)} and produce the Uni using
     *             {@code Uni.createFrom().completionStage(...)}
     */
    @Deprecated
    public <R> Uni<R> produceCompletionStage(Function<? super T, ? extends CompletionStage<? extends R>> mapper) {
        nonNull(mapper, "mapper");
        return transformToUni(item -> {
            CompletionStage<? extends R> stage = Objects.requireNonNull(mapper.apply(item),
                    "The mapper produces a `null` completion stage");
            return Uni.createFrom().completionStage(stage);
        });
    }

    /**
     * Transforms the received item asynchronously, forwarding the events emitted by an {@link UniEmitter} provided to
     * the given consumer.
     * <p>
     * The consumer is called with the item event of the current {@link Uni} and an emitter used to fire events.
     * These events are these propagated by the produced {@link Uni}.
     *
     * @param consumer the function called with the item of the this {@link Uni} and an {@link UniEmitter}.
     *        It must not be {@code null}.
     * @param <R> the type of item emitted by the emitter
     * @return a new {@link Uni} that would fire events from the emitter consumed by the mapper function, possibly
     *         in an asynchronous manner.
     */
    public <R> Uni<R> transformToUni(BiConsumer<? super T, UniEmitter<? super R>> consumer) {
        nonNull(consumer, "consumer");
        return this.transformToUni(x -> Uni.createFrom().emitter(emitter -> consumer.accept(x, emitter)));
    }

    /**
     * Transforms the received item asynchronously, forwarding the events emitted by an {@link UniEmitter} provided to
     * the given consumer.
     * <p>
     * The consumer is called with the item event of the current {@link Uni} and an emitter used to fire events.
     * These events are these propagated by the produced {@link Uni}.
     *
     * @param consumer the function called with the item of the this {@link Uni} and an {@link UniEmitter}.
     *        It must not be {@code null}.
     * @param <R> the type of item emitted by the emitter
     * @return a new {@link Uni} that would fire events from the emitter consumed by the mapper function, possibly
     *         in an asynchronous manner.
     * @deprecated Use {@link #transformToUni(BiConsumer)}
     */
    @Deprecated
    public <R> Uni<R> produceUni(BiConsumer<? super T, UniEmitter<? super R>> consumer) {
        return transformToUni(consumer);
    }

    /**
     * Produces a new {@link Uni} receiving an item from upstream and delaying the emission on the {@code item}
     * event (with the same item).
     *
     * @return the object to configure the delay.
     */
    public UniOnItemDelay<T> delayIt() {
        return new UniOnItemDelay<>(upstream, null);
    }

    /**
     * Produces a {@link Uni} ignoring the item of the current {@link Uni} and continuing with either
     * {@link UniOnItemIgnore#andContinueWith(Object) another item}, {@link UniOnItemIgnore#andFail() a failure},
     * or {@link UniOnItemIgnore#andSwitchTo(Uni) another Uni}. The produced {@link Uni} propagates the failure
     * event if the upstream uni fires a failure.
     *
     * <p>
     * Examples:
     * </p>
     *
     * <pre>
     * <code>
     *     Uni&lt;T&gt; upstream = ...;
     *     uni = upstream.onItem().ignore().andSwitchTo(other) // Ignore the item from upstream and switch to another uni
     *     uni = upstream.onItem().ignore().andContinueWith(newResult) // Ignore the item from upstream, and fire newResult
     * </code>
     * </pre>
     *
     * @return the object to configure the continuation logic.
     */
    public UniOnItemIgnore<T> ignore() {
        return new UniOnItemIgnore<>(this);
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} fires an item. The
     * function transforms the received item into a failure that will be fired by the produced {@link Uni}.
     * For asynchronous composition, see {@link #transformToUni(Function)}}.
     *
     * @param mapper the mapper function, must not be {@code null}, must not return {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> failWith(Function<? super T, ? extends Throwable> mapper) {
        nonNull(mapper, "mapper");
        return Infrastructure.onUniCreation(transformToUni(t -> Uni.createFrom().failure(mapper.apply(t))));
    }

    /**
     * Produces an {@link Uni} emitting an item based on the upstream item but casted to the target class.
     *
     * @param target the target class
     * @param <O> the type of item emitted by the produced uni
     * @return the new Uni
     */
    public <O> Uni<O> castTo(Class<O> target) {
        nonNull(target, "target");
        return apply(target::cast);
    }

    /**
     * Adds specific behavior when the observed {@link Uni} fires {@code null} as item. While {@code null} is a valid
     * value, it may require specific processing. This group of operators allows implementing this specific behavior.
     *
     * <p>
     * Examples:
     * </p>
     *
     * <pre>
     * <code>
     *     Uni&lt;T&gt; upstream = ...;
     *     Uni&lt;T&gt; uni = ...;
     *     uni = upstream.onItem().ifNull().continueWith(anotherValue) // use the fallback value if upstream emits null
     *     uni = upstream.onItem().ifNull().fail() // propagate a NullPointerException if upstream emits null
     *     uni = upstream.onItem().ifNull().failWith(exception) // propagate the given exception if upstream emits null
     *     uni = upstream.onItem().ifNull().switchTo(another) // switch to another uni if upstream emits null
     * </code>
     * </pre>
     *
     * @return the object to configure the behavior when receiving {@code null}
     */
    public UniOnNull<T> ifNull() {
        return new UniOnNull<>(upstream);
    }

    /**
     * Adds specific behavior when the observed {@link Uni} fies a {@code non-null} item. If the item is {@code null},
     * default fallbacks are used.
     *
     * @return the object to configure the behavior when receiving a {@code non-null} item
     */
    public UniOnNotNull<T> ifNotNull() {
        return new UniOnNotNull<>(upstream);
    }

    /**
     * Takes the items from the upstream {@link Uni} that is either a {@link Publisher Publisher&lt;O&gt;},
     * an {@link java.lang.reflect.Array O[]}, an {@link Iterable Iterable&lt;O&gt;} or a {@link Multi Multi&lt;O&gt;} and
     * disjoint the items to obtain a {@link Multi Multi&lt;O&gt;}.
     * <p>
     * For examples, {@code Uni<[A, B, C]>} is transformed into {@code Multi<A, B, C>}, {@code Uni<[]>} is transformed
     * into an empty {@code Multi}.
     * <p>
     * If the item from the upstream are not instances of {@link Iterable}, {@link Publisher} or array, an
     * {@link IllegalArgumentException} is propagated downstream.
     * <p>
     * If the item is {@code null}, an empty {@code Multi} is produced.
     * If the upstream propagates a failure, the failure is propagated downstream.
     *
     * @param <O> the type of the upstream item.
     * @return the resulting multi
     */
    public <O> Multi<O> disjoint() {
        return upstream.toMulti()
                .onItem().disjoint();
    }
}
