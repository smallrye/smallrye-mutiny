package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.*;

import java.util.concurrent.CompletionStage;
import java.util.function.*;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.*;
import io.smallrye.mutiny.subscription.BackPressureStrategy;

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
    public <R> Multi<R> apply(Function<? super T, ? extends R> mapper) {
        return Infrastructure.onMultiCreation(new MultiMapOp<>(upstream, nonNull(mapper, "mapper")));
    }

    /**
     * Produces a new {@link Multi} invoking the given callback when an {@code item} event is fired by the upstream.
     *
     * If the callback throws an exception, this exception is propagated downstream.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Multi}
     */
    public Multi<T> invoke(Consumer<T> callback) {
        return Infrastructure.onMultiCreation(new MultiSignalConsumerOp<>(
                upstream,
                null,
                nonNull(callback, "callback"),
                null,
                null,
                null,
                null,
                null));
    }

    /**
     * Produces a new {@link Multi} invoking the given @{code action} when an {@code item} event is received. Note that
     * the received item cannot be {@code null}.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its result, the result is discarded, and the original {@code item} is forwarded downstream. If the produced
     * {@code Uni} fails, the failure is propagated downstream.
     *
     * If the asynchronous action throws an exception, this exception is propagated downstream.
     *
     * This method preserves the order of the items, meaning that the downstream received the items in the same order
     * as the upstream has emitted them.
     *
     * @param action the function taking the item and returning a {@link Uni}, must not be {@code null}
     * @return the new {@link Multi}
     */
    public Multi<T> invokeUni(Function<? super T, ? extends Uni<?>> action) {
        ParameterValidation.nonNull(action, "action");
        return applyUniAndConcatenate(i -> {
            Uni<?> uni = action.apply(i);
            if (uni == null) {
                throw new NullPointerException("The `action` produced a `null` Uni");
            }
            return uni.onItemOrFailure().applyUni((ignored, failure) -> {
                if (failure != null) {
                    return Uni.createFrom().failure(failure);
                } else {
                    return Uni.createFrom().item(i);
                }
            });
        });
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
     * @return the resulting {@link Multi}
     */
    @SuppressWarnings("unchecked")
    public <O> Multi<O> disjoint() {
        return upstream.onItem().applyMulti(x -> {
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
        }).concatenate();
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link Publisher}. The events emitted by the returned {@link Publisher} are propagated downstream. The returned
     * {@link MultiFlatten} object allows configuring how these events are propagated downstream such as failure
     * collection and ordering.
     *
     * This operation is often called <em>flatMap</em> (if you pick {@link MultiFlatten#merge()} or <em>concatMap</em>
     * (if you pick {@link MultiFlatten#concatenate()}).
     *
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the object to configure the flatten behavior.
     */
    public <O> MultiFlatten<T, O> applyMulti(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        return new MultiFlatten<>(upstream, nonNull(mapper, "mapper"), 1, false);
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link Publisher}. The events emitted by the returned {@link Publisher} are propagated using a {@code merge},
     * meaning that it may interleave events produced by the different {@link Publisher Publishers}.
     *
     * For example, let's imagine an upstream multi {a, b, c} and a mapper emitting the 3 items with some delays
     * between them. For example a -&gt; {a1, a2, a3}, b -&gt; {b1, b2, b3} and c -&gt; {c1, c2, c3}. Using this method
     * on the multi {a, b c} with that mapper may produce the following multi {a1, b1, a2, c1, b2, c2, a3, b3, c3}.
     * So the items from the produced multis are interleaved and are emitted as soon as they are emitted (respecting
     * the downstream request).
     *
     * This operation is often called <em>flatMap</em>.
     *
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     * If one of the produced {@link Publisher} propagates a failure, the failure is propagated downstream and no
     * more items will be emitted.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the resulting multi
     */
    public <O> Multi<O> applyMultiAndMerge(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        return applyMulti(mapper).merge();
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link Publisher}. The events emitted by the returned {@link Publisher} are propagated downstream using a
     * {@code concatenation}, meaning that it does not interleaved the items produced by the different
     * {@link Publisher Publishers}.
     *
     * For example, let's imagine an upstream multi {a, b, c} and a mapper emitting the 3 items with some delays
     * between them. For example a -&gt; {a1, a2, a3}, b -&gt; {b1, b2, b3} and c -&gt; {c1, c2, c3}. Using this method
     * on the multi {a, b c} with that mapper may produce the following multi {a1, a2, a3, b1, b2, b3, c1, c2, c3}.
     * So produced multis are concatenated.
     *
     * This operation is often called <em>concatMap</em>.
     *
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     * If one of the produced {@link Publisher} propagates a failure, the failure is propagated downstream and no
     * more items will be emitted.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the resulting multi
     */
    public <O> Multi<O> applyMultiAndConcatenate(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        return applyMulti(mapper).concatenate();
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link Publisher}. The events emitted by the returned {@link Publisher} are emitted downstream. The returned
     * {@link MultiFlatten} object allows configuring how these events are propagated downstream such as failure
     * collection and ordering.
     *
     * This operation is often called <em>flatMap</em> (if you pick {@link MultiFlatten#merge()} or <em>concatMap</em>
     * (if you pick {@link MultiFlatten#concatenate()}).
     *
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the object to configure the flatten behavior.
     * @deprecated Use {@link #applyMulti(Function)} instead
     */
    @Deprecated
    public <O> MultiFlatten<T, O> produceMulti(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        return applyMulti(mapper);
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link Publisher}. The events emitted by the returned {@link Publisher} are emitted downstream. The returned
     * {@link MultiFlatten} object allows configuring how these events are propagated downstream such as failure
     * collection and ordering.
     *
     * This operation is often called <em>flatMap</em> (if you pick {@link MultiFlatten#merge()} or <em>concatMap</em>
     * (if you pick {@link MultiFlatten#concatenate()}).
     *
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the object to configure the flatten behavior.
     * @deprecated Use {@link #applyMulti(Function)} instead
     */
    @Deprecated
    public <O> MultiFlatten<T, O> producePublisher(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        return applyMulti(mapper);
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link Iterable}. The items contained in these iterable are emitted downstream. The items are emitted downstream
     * by preserving the order. In other words, the different iterables are concatenated.
     *
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the new multi
     */
    public <O> Multi<O> applyIterable(Function<? super T, ? extends Iterable<? extends O>> mapper) {
        nonNull(mapper, "mapper");
        return applyMultiAndConcatenate((x -> {
            Iterable<? extends O> iterable = mapper.apply(x);
            if (iterable == null) {
                return Multi.createFrom().failure(new NullPointerException(MAPPER_RETURNED_NULL));
            } else {
                return Multi.createFrom().iterable(iterable);
            }
        }));
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link Iterable}. The items contained in these iterable are emitted downstream.
     *
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the new multi
     * @deprecated Use {@link #applyIterable(Function)}
     */
    @Deprecated
    public <O> MultiFlatten<T, O> produceIterable(Function<? super T, ? extends Iterable<? extends O>> mapper) {
        nonNull(mapper, "mapper");
        return applyMulti((x -> {
            Iterable<? extends O> iterable = mapper.apply(x);
            if (iterable == null) {
                return Multi.createFrom().failure(new NullPointerException(MAPPER_RETURNED_NULL));
            } else {
                return Multi.createFrom().iterable(iterable);
            }
        }));
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link Uni}. The events emitted by the returned {@link Uni} are emitted downstream. The returned
     * {@link MultiFlatten} object allows configuring how these events are propagated downstream such as failure
     * collection and ordering.
     *
     * This operation is often called <em>flatMap</em> (if you pick {@link MultiFlatten#merge()} or <em>concatMap</em>
     * (if you pick {@link MultiFlatten#concatenate()}).
     *
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     * If a produced uni propagates a failure, the failure is propagated downstream and more items will be emitted.
     * If a produced uni propagates {@code null}, no item would be propagated downstream.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the object to configure the flatten behavior.
     */
    public <O> MultiFlatten<T, O> applyUni(Function<? super T, ? extends Uni<? extends O>> mapper) {
        nonNull(mapper, "mapper");
        Function<? super T, ? extends Publisher<? extends O>> wrapper = res -> mapper.apply(res).toMulti();
        return new MultiFlatten<>(upstream, wrapper, 1, false);
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link Uni}. The events emitted by the returned {@link Uni} are emitted downstream. Items emitted
     * by the returned {@link Uni Unis} are emitted downstream using a {@code merge}, meaning that it
     * may interleave events produced by the different {@link Uni Uni}.
     *
     * For example, let's imagine an upstream multi {a, b, c} and a mapper emitting 1 items. This emission may be
     * delayed for various reasons. For example a -&gt; a1 without delay, b -&gt; b1 after some delay and c -&gt; c1 without
     * delay. Using this method on the multi {a, b c} with that mapper would produce the following multi {a1, c1, b1}.
     * Indeed, the b1 item is emitted after c1. So the items from the produced unis are interleaved and are emitted as
     * soon as they are emitted (respecting the downstream request).
     *
     * This operation is often called <em>flatMapSingle</em>.
     *
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     * If one of the produced {@link Uni} propagates a failure, the failure is propagated downstream and no
     * more items will be emitted.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the resulting multi
     */
    public <O> Multi<O> applyUniAndConcatenate(Function<? super T, ? extends Uni<? extends O>> mapper) {
        return applyUni(mapper).concatenate();
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link Uni}. The events emitted by the returned {@link Uni} are emitted downstream. Items emitted
     * by the returned {@link Uni Unis} are emitted downstream using a {@code concatenation}, meaning the the returned
     * {@link Multi} contains the items in the same order as the upstream.
     *
     * For example, let's imagine an upstream multi {a, b, c} and a mapper emitting 1 items. This emission may be
     * delayed for various reasons. For example a -&gt; a1 without delay, b -&gt; b1 after some delay and c -&gt; c1 without
     * delay. Using this method on the multi {a, b c} with that mapper would produce the following multi {a1, b1, c1}.
     * Indeed, even if c1 could be emitted before b1, this method preserves the order. So the items from the produced
     * unis are concatenated.
     *
     * This operation is often called <em>concatMapSingle</em>.
     *
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     * If one of the produced {@link Uni} propagates a failure, the failure is propagated downstream and no
     * more items will be emitted.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the resulting multi
     */
    public <O> Multi<O> applyUniAndMerge(Function<? super T, ? extends Uni<? extends O>> mapper) {
        return applyUni(mapper).merge();
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link Uni}. The events emitted by the returned {@link Uni} are emitted downstream. The returned
     * {@link MultiFlatten} object allows configuring how these events are propagated downstream such as failure
     * collection and ordering.
     *
     * This operation is often called <em>flatMap</em> (if you pick {@link MultiFlatten#merge()} or <em>concatMap</em>
     * (if you pick {@link MultiFlatten#concatenate()}).
     *
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the object to configure the flatten behavior.
     * @deprecated Use {@link #applyUni(Function)} instead
     */
    @Deprecated
    public <O> MultiFlatten<T, O> produceUni(Function<? super T, ? extends Uni<? extends O>> mapper) {
        nonNull(mapper, "mapper");
        Function<? super T, ? extends Publisher<? extends O>> wrapper = res -> mapper.apply(res).toMulti();
        return new MultiFlatten<>(upstream, wrapper, 1, false);
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link CompletionStage}. The events emitted by the returned {@link Uni} are emitted downstream. The returned
     * {@link MultiFlatten} object allows configuring how these events are propagated downstream such as failure
     * collection and ordering.
     *
     * This operation is often called <em>flatMap</em> (if you pick {@link MultiFlatten#merge()} or <em>concatMap</em>
     * (if you pick {@link MultiFlatten#concatenate()}).
     *
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     * If a produced completion stage is completed <em>exceptionally</em>, the failure is propagated downstream and no
     * more items will be emitted.
     * If a produced completion state is completed with {@code null}, no item is propagated downstream.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the object to configure the flatten behavior.
     */
    public <O> MultiFlatten<T, O> applyCompletionStage(
            Function<? super T, ? extends CompletionStage<? extends O>> mapper) {
        nonNull(mapper, "mapper");
        Function<? super T, ? extends Publisher<? extends O>> wrapper = res -> Multi.createFrom().emitter(emitter -> {
            CompletionStage<? extends O> stage;
            try {
                stage = mapper.apply(res);
            } catch (Throwable e) {
                emitter.fail(e);
                return;
            }
            if (stage == null) {
                throw new NullPointerException(SUPPLIER_PRODUCED_NULL);
            }

            emitter.onTermination(() -> stage.toCompletableFuture().cancel(false));
            stage.whenComplete((r, f) -> {
                if (f != null) {
                    emitter.fail(f);
                } else if (r != null) {
                    emitter.emit(r);
                    emitter.complete();
                } else {
                    emitter.complete();
                }
            });
        }, BackPressureStrategy.LATEST);

        return new MultiFlatten<>(upstream, wrapper, 1, false);
    }

    /**
     * For each items emitted by the upstream, the given {@code mapper} is invoked. This {@code mapper} returns a
     * {@link CompletionStage}. The events emitted by the returned {@link Uni} are emitted downstream. The returned
     * {@link MultiFlatten} object allows configuring how these events are propagated downstream such as failure
     * collection and ordering.
     *
     * This operation is often called <em>flatMap</em> (if you pick {@link MultiFlatten#merge()} or <em>concatMap</em>
     * (if you pick {@link MultiFlatten#concatenate()}).
     *
     * If the mapper throws an exception, the failure is propagated downstream. No more items will be emitted.
     * If a produced completion stage is completed <em>exceptionally</em> the failure is propagated downstream and no
     * more items will be emitted.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the object to configure the flatten behavior.
     * @deprecated Use {@link #applyCompletionStage(Function)} instead
     */
    @Deprecated
    public <O> MultiFlatten<T, O> produceCompletionStage(
            Function<? super T, ? extends CompletionStage<? extends O>> mapper) {
        return applyCompletionStage(mapper);
    }

    /**
     * Ignores the passed items. The resulting {@link Multi} will only be notified when the upstream completes or fails.
     *
     * @return the new multi
     */
    public Multi<Void> ignore() {
        return Infrastructure.onMultiCreation(new MultiIgnoreOp<>(upstream));
    }

    /**
     * Ignores the passed items. The resulting {@link Uni} will only be completed with {@code null} when the upstream
     * completes or with a failure if the upstream emits a failure..
     *
     * @return the new multi
     */
    public Uni<Void> ignoreAsUni() {
        return ignore().toUni();
    }

    /**
     * Produces an {@link Multi} emitting the item events based on the upstream events but casted to the target class.
     *
     * @param target the target class
     * @param <O> the type of item emitted by the produced uni
     * @return the new Uni
     */
    public <O> Multi<O> castTo(Class<O> target) {
        nonNull(target, "target");
        return apply(target::cast);
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
    public <S> Multi<S> scan(Supplier<S> initialStateProducer, BiFunction<S, ? super T, S> accumulator) {
        return Infrastructure.onMultiCreation(new MultiScanWithSeedOp<>(upstream, initialStateProducer, accumulator));
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
    public Multi<T> scan(BinaryOperator<T> accumulator) {
        return Infrastructure.onMultiCreation(new MultiScanOp<>(upstream, accumulator));
    }

}
