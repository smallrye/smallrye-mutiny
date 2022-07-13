package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.*;

import java.util.Objects;
import java.util.concurrent.Flow.Publisher;
import java.util.function.*;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.*;

public class MultiOnItem<T> {

    private final Multi<T> upstream;

    public MultiOnItem(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Produces a new {@link Multi} invoking the given function for each item emitted by the upstream {@link Multi}.
     * <p>
     * The function receives the received item as parameter, and can transform it. The returned object is sent
     * downstream as {@code item} event.
     * <p>
     *
     * @param mapper the mapper function, must not be {@code null}
     * @param <R> the type of item produced by the mapper function
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public <R> Multi<R> transform(Function<? super T, ? extends R> mapper) {
        Function<? super T, ? extends R> actual = Infrastructure.decorate(nonNull(mapper, "mapper"));
        return Infrastructure.onMultiCreation(new MultiMapOp<>(upstream, actual));
    }

    /**
     * Produces a new {@link Multi} invoking the given callback when an {@code item} event is fired by the upstream.
     * Note that the received item cannot be {@code null}.
     * <p>
     * If the callback throws an exception, this exception is propagated to the downstream as failure. No more items
     * will be consumed.
     * <p>
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> invoke(Consumer<? super T> callback) {
        Consumer<? super T> actual = Infrastructure.decorate(nonNull(callback, "callback"));
        return Infrastructure.onMultiCreation(new MultiOnItemInvoke<>(upstream, actual));
    }

    /**
     * Produces a new {@link Multi} invoking the given callback when an {@code item} event is fired by the upstream.
     * <p>
     * If the callback throws an exception, this exception is propagated to the downstream as failure. No more items
     * will be consumed.
     * <p>
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> invoke(Runnable callback) {
        Runnable actual = nonNull(callback, "callback");
        // The decoration happens in invoke.
        return invoke(ignored -> actual.run());
    }

    /**
     * Produces a new {@link Multi} invoking the given @{code action} when an {@code item} event is received. Note that
     * the received item cannot be {@code null}.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its result, the result is discarded, and the original {@code item} is forwarded downstream. If the produced
     * {@code Uni} fails, the failure is propagated downstream.
     * <p>
     * If the asynchronous action throws an exception, this exception is propagated downstream.
     * <p>
     * This method preserves the order of the items, meaning that the downstream received the items in the same order
     * as the upstream has emitted them.
     *
     * @param action the function taking the item and returning a {@link Uni}, must not be {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> call(Function<? super T, Uni<?>> action) {
        Function<? super T, Uni<?>> actual = Infrastructure.decorate(nonNull(action, "action"));
        return transformToUni(i -> {
            Uni<?> uni = actual.apply(i);
            if (uni == null) {
                throw new NullPointerException("The `action` produced a `null` Uni");
            }
            return uni.onItemOrFailure().transformToUni((ignored, failure) -> {
                if (failure != null) {
                    return Uni.createFrom().failure(failure);
                } else {
                    return Uni.createFrom().item(i);
                }
            });
        }).concatenate();
    }

    /**
     * Produces a new {@link Multi} invoking the given @{code action} when an {@code item} event is received, but
     * ignoring it in the callback.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its result, the result is discarded, and the original {@code item} is forwarded downstream. If the produced
     * {@code Uni} fails, the failure is propagated downstream.
     * <p>
     * If the asynchronous action throws an exception, this exception is propagated downstream.
     * <p>
     * This method preserves the order of the items, meaning that the downstream received the items in the same order
     * as the upstream has emitted them.
     *
     * @param action the function taking the item and returning a {@link Uni}, must not be {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> call(Supplier<Uni<?>> action) {
        Supplier<Uni<?>> actual = Infrastructure.decorate(nonNull(action, "action"));
        return call(ignored -> actual.get());
    }

    /**
     * Takes the items from the upstream {@link Multi} that are either {@link Publisher Publisher&lt;O&gt;},
     * {@link java.lang.reflect.Array O[]}, {@link Iterable Iterable&lt;O&gt;} or {@link Multi Multi&lt;O&gt;} and
     * disjoint the items to obtain a {@link Multi Multi&lt;O&gt;}.
     * <p>
     * For example, {@code Multi<[A, B, C], [D, E, F]} is transformed into {@code Multi<A, B, C, D, E, F>}.
     * <p>
     * If the items from upstream are not instances of {@link Iterable}, {@link Publisher} or array, an
     * {@link IllegalArgumentException} is propagated downstream.
     *
     * @param <O> the type items contained in the upstream's items.
     * @return the resulting multi
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    public <O> Multi<O> disjoint() {
        return upstream.onItem().transformToMultiAndConcatenate(x -> {
            if (x instanceof Iterable) {
                return Multi.createFrom().iterable((Iterable<O>) x);
            } else if (x instanceof Multi) {
                return (Multi<O>) x;
            } else if (x instanceof Publisher) {
                return Multi.createFrom().publisher((Publisher<O>) x);
            } else if (x.getClass().isArray()) {
                O[] item = (O[]) x;
                return Multi.createFrom().items(item);
            } else {
                return Multi.createFrom().failure(new IllegalArgumentException(
                        "Invalid parameter - cannot disjoint instance of " + x.getClass().getName()));
            }
        });
    }

    /**
     * On each item received from upstream, invoke the given mapper. This mapper return a {@link Publisher} or
     * a {@link Multi}. The return object lets you configure the flattening process, i.e. how the items produced
     * by the returned {@link Publisher Publishers or Multis} are propagated to the downstream.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the object to configure the flatten behavior.
     */
    @CheckReturnValue
    public <O> MultiFlatten<T, O> transformToMulti(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        Function<? super T, ? extends Publisher<? extends O>> actual = Infrastructure
                .decorate(nonNull(mapper, "mapper"));
        return new MultiFlatten<>(upstream, actual, 1, false);
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link Publisher}. The events emitted by the returned {@link Publisher} are propagated downstream using a
     * {@code concatenation}, meaning that it does not interleaved the items produced by the different
     * {@link Publisher Publishers}.
     * <p>
     * For example, let's imagine an upstream multi {a, b, c} and a mapper emitting the 3 items with some delays
     * between them. For example a -&gt; {a1, a2, a3}, b -&gt; {b1, b2, b3} and c -&gt; {c1, c2, c3}. Using this method
     * on the multi {a, b c} with that mapper may produce the following multi {a1, a2, a3, b1, b2, b3, c1, c2, c3}.
     * So produced multis are concatenated.
     * <p>
     * This operation is often called <em>concatMap</em>.
     * <p>
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     * If one of the produced {@link Publisher} propagates a failure, the failure is propagated downstream and no
     * more items will be emitted.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the resulting multi
     */
    @CheckReturnValue
    public <O> Multi<O> transformToMultiAndConcatenate(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        // Decoration happens in `transformToMulti`
        return transformToMulti(mapper).concatenate();
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link Publisher}. The events emitted by the returned {@link Publisher} are propagated using a {@code merge},
     * meaning that it may interleave events produced by the different {@link Publisher Publishers}.
     * <p>
     * For example, let's imagine an upstream multi {a, b, c} and a mapper emitting the 3 items with some delays
     * between them. For example a -&gt; {a1, a2, a3}, b -&gt; {b1, b2, b3} and c -&gt; {c1, c2, c3}. Using this method
     * on the multi {a, b c} with that mapper may produce the following multi {a1, b1, a2, c1, b2, c2, a3, b3, c3}.
     * So the items from the produced multis are interleaved and are emitted as soon as they are emitted (respecting
     * the downstream request).
     * <p>
     * This operation is often called <em>flatMap</em>.
     * <p>
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     * If one of the produced {@link Publisher} propagates a failure, the failure is propagated downstream and no
     * more items will be emitted.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the resulting multi
     */
    @CheckReturnValue
    public <O> Multi<O> transformToMultiAndMerge(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        // Decoration happens in `transformToMulti`
        return transformToMulti(mapper).merge();
    }

    /**
     * On each item received from the upstream, call the given mapper. The mapper returns an {@link Iterable}.
     * The items from the returned {@link Iterable} are propagated downstream (one by one). As {@link Iterable} is
     * a synchronous construct, this method concatenates the items produced by the different returns iterables.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item contained by the {@link Iterable} produced by the mapper.
     * @return the object to configure the flatten behavior.
     */
    @CheckReturnValue
    public <O> Multi<O> transformToIterable(Function<? super T, ? extends Iterable<O>> mapper) {
        Function<? super T, ? extends Iterable<O>> actual = Infrastructure.decorate(nonNull(mapper, "mapper"));
        return transformToMultiAndConcatenate((x -> {
            Iterable<O> iterable = actual.apply(x);
            if (iterable == null) {
                return Multi.createFrom().failure(new NullPointerException(MAPPER_RETURNED_NULL));
            } else {
                return Multi.createFrom().iterable(iterable);
            }
        }));
    }

    /**
     * On each item received from upstream, invoke the given mapper. This mapper return {@link Uni Uni&lt;T&gt;}.
     * The return object lets you configure the flattening process, i.e. how the items produced
     * by the returned {@link Uni Unis} are propagated to the downstream.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the object to configure the flatten behavior.
     */
    @CheckReturnValue
    public <O> MultiFlatten<T, O> transformToUni(Function<? super T, Uni<? extends O>> mapper) {
        Function<? super T, Uni<? extends O>> actual = Infrastructure.decorate(nonNull(mapper, "mapper"));
        Function<? super T, ? extends Publisher<? extends O>> wrapper = res -> actual.apply(res).toMulti();
        return new MultiFlatten<>(upstream, wrapper, 1, false);
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link Uni}. The events emitted by the returned {@link Uni} are emitted downstream. Items emitted
     * by the returned {@link Uni Unis} are emitted downstream using a {@code concatenation}, meaning that the returned
     * {@link Multi} contains the items in the same order as the upstream.
     * <p>
     * For example, let's imagine an upstream multi {a, b, c} and a mapper emitting 1 items. This emission may be
     * delayed for various reasons. For example a -&gt; a1 without delay, b -&gt; b1 after some delay and c -&gt; c1 without
     * delay. Using this method on the multi {a, b c} with that mapper would produce the following multi {a1, b1, c1}.
     * Indeed, even if c1 could be emitted before b1, this method preserves the order. So the items from the produced
     * unis are concatenated.
     * <p>
     * This operation is often called <em>concatMapSingle</em>.
     * <p>
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     * If one of the produced {@link Uni} propagates a failure, the failure is propagated downstream and no
     * more items will be emitted.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the resulting multi
     */
    @CheckReturnValue
    public <O> Multi<O> transformToUniAndConcatenate(Function<? super T, Uni<? extends O>> mapper) {
        // Decoration happens in `transformToUni`
        return transformToUni(mapper).concatenate();
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link Uni}. The events emitted by the returned {@link Uni} are emitted downstream. Items emitted
     * by the returned {@link Uni Unis} are emitted downstream using a {@code merge}, meaning that it
     * may interleave events produced by the different {@link Uni Uni}.
     * <p>
     * For example, let's imagine an upstream multi {a, b, c} and a mapper emitting 1 items. This emission may be
     * delayed for various reasons. For example a -&gt; a1 without delay, b -&gt; b1 after some delay and c -&gt; c1 without
     * delay. Using this method on the multi {a, b c} with that mapper would produce the following multi {a1, c1, b1}.
     * Indeed, the b1 item is emitted after c1. So the items from the produced unis are interleaved and are emitted as
     * soon as they are emitted (respecting the downstream request).
     * <p>
     * This operation is often called <em>flatMapSingle</em>.
     * <p>
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     * If one of the produced {@link Uni} propagates a failure, the failure is propagated downstream and no
     * more items will be emitted.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the resulting multi
     */
    @CheckReturnValue
    public <O> Multi<O> transformToUniAndMerge(Function<? super T, Uni<? extends O>> mapper) {
        // Decoration happens in `transformToUni`
        return transformToUni(mapper).merge();
    }

    /**
     * Ignores the passed items. The resulting {@link Multi} will only be notified when the stream completes or fails.
     *
     * @return the new multi
     */
    @CheckReturnValue
    public Multi<Void> ignore() {
        return Infrastructure.onMultiCreation(new MultiIgnoreOp<>(upstream));
    }

    /**
     * Ignores the passed items. The resulting {@link Uni} will only be completed with {@code null} when the stream
     * completes or with a failure if the upstream emits a failure..
     *
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<Void> ignoreAsUni() {
        return ignore().toUni();
    }

    /**
     * Produces an {@link Multi} emitting the item events based on the upstream events but casted to the target class.
     *
     * @param target the target class
     * @param <O> the type of item emitted by the produced uni
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public <O> Multi<O> castTo(Class<O> target) {
        nonNull(target, "target");
        return transform(target::cast);
    }

    /**
     * Produces a {@link Multi} that fires items coming from the reduction of the item emitted by this current
     * {@link Multi} by the passed {@code accumulator} reduction function. The produced multi emits the intermediate
     * results.
     * <p>
     * Unlike {@link #scan(BinaryOperator)}, this operator uses the value produced by the {@code initialStateProducer} as
     * first value.
     *
     * @param initialStateProducer the producer called to provides the initial value passed to the accumulator operation.
     * @param accumulator the reduction {@link BiFunction}, the resulting {@link Multi} emits the results of
     *        this method. The method is called for every item emitted by this Multi.
     * @param <S> the type of item emitted by the produced {@link Multi}. It's the type returned by the
     *        {@code accumulator} operation.
     * @return the produced {@link Multi}
     */
    @CheckReturnValue
    public <S> Multi<S> scan(Supplier<S> initialStateProducer, BiFunction<S, ? super T, S> accumulator) {
        Supplier<S> actualSupplier = Infrastructure
                .decorate(nonNull(initialStateProducer, "initialStateProducer"));
        BiFunction<S, ? super T, S> actualAccumulator = Infrastructure.decorate(nonNull(accumulator, "accumulator"));
        return Infrastructure.onMultiCreation(new MultiScanWithSeedOp<>(upstream, actualSupplier, actualAccumulator));
    }

    /**
     * Produces a {@link Multi} that fires results coming from the reduction of the item emitted by this current
     * {@link Multi} by the passed {@code accumulator} reduction function. The produced multi emits the intermediate
     * results.
     * <p>
     * Unlike {@link #scan(Supplier, BiFunction)}, this operator doesn't take an initial value but takes the first
     * item emitted by this {@link Multi} as initial value.
     *
     * @param accumulator the reduction {@link BiFunction}, the resulting {@link Multi} emits the results of this method.
     *        The method is called for every item emitted by this Multi.
     * @return the produced {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> scan(BinaryOperator<T> accumulator) {
        BinaryOperator<T> actual = Infrastructure.decorate(nonNull(accumulator, "accumulator"));
        return Infrastructure.onMultiCreation(new MultiScanOp<>(upstream, actual));
    }

    /**
     * Produces a new {@link Multi} invoking the given function when the current {@link Multi} fires an item (only once).
     * The function transforms the received item into a failure that will be fired by the produced {@link Multi}.
     * For asynchronous composition, see {@link #transformToUni(Function)}}.
     * <p>
     * The upstream subscription is cancelled after the emission of the first item, as a failure is propagated downstream.
     *
     * @param mapper the mapper function, must not be {@code null}, must not return {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> failWith(Function<? super T, ? extends Throwable> mapper) {
        Function<? super T, ? extends Throwable> actual = Infrastructure.decorate(nonNull(mapper, "mapper"));
        return Infrastructure.onMultiCreation(transformToUniAndConcatenate(t -> {
            Throwable failure = Objects.requireNonNull(actual.apply(t), MAPPER_RETURNED_NULL);
            return Uni.createFrom().failure(failure);
        }));
    }

    /**
     * Produces a new {@link Multi} invoking the given supplier when the current {@link Uni} fires an item.
     * The supplier produce the received item into a failure that will be fired by the produced {@link Multi}.
     * <p>
     * The upstream subscription is cancelled after the emission of the first item, as a failure is propagated downstream.
     *
     * @param supplier the supplier to produce the failure, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> failWith(Supplier<? extends Throwable> supplier) {
        Supplier<? extends Throwable> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return Infrastructure.onMultiCreation(transformToUniAndConcatenate(ignored -> {
            Throwable failure = Objects.requireNonNull(actual.get(), SUPPLIER_PRODUCED_NULL);
            return Uni.createFrom().failure(failure);
        }));
    }

}
