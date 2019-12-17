package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.*;

import java.util.concurrent.CompletionStage;
import java.util.function.*;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
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
     * Produces a new {@link Multi} invoking the given callback when an {@code item} event is fired by the upstrea.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
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
    public <O> Multi<O> disjoint() {
        return upstream.onItem().produceMulti(x -> {
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
     * Configures the <em>mapper</em> of the <em>flatMap</em> operation.
     * The mapper returns a {@link Multi multi} and is called for each item emitted by the upstream {@link Multi}.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the object to configure the flatten behavior.
     */
    public <O> MultiFlatten<T, O> produceMulti(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        return producePublisher(mapper);
    }

    /**
     * Configures the <em>mapper</em> of the <em>flatMap</em> operation.
     * The mapper returns a {@link Publisher publisher} and is called for each item emitted by the upstream {@link Multi}.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Publisher} produced by the mapper.
     * @return the object to configure the flatten behavior.
     */
    public <O> MultiFlatten<T, O> producePublisher(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        return new MultiFlatten<>(upstream, nonNull(mapper, "mapper"), 1, false);
    }

    /**
     * Configures the <em>mapper</em> of the <em>flatMap</em> operation.
     * The mapper returns a {@link Iterable iterable} and is called for each item emitted by the upstream {@link Multi}.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item contained by the {@link Iterable} produced by the mapper.
     * @return the object to configure the flatten behavior.
     */
    public <O> MultiFlatten<T, O> produceIterable(Function<? super T, ? extends Iterable<? extends O>> mapper) {
        nonNull(mapper, "mapper");
        return producePublisher((x -> {
            Iterable<? extends O> iterable = mapper.apply(x);
            if (iterable == null) {
                return Multi.createFrom().failure(new NullPointerException(MAPPER_RETURNED_NULL));
            } else {
                return Multi.createFrom().iterable(iterable);
            }
        }));
    }

    /**
     * Configures the <em>mapper</em> of the <em>flatMap</em> operation.
     * The mapper returns a {@link Uni uni} and is called for each item emitted by the upstream {@link Multi}.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Uni} produced by the mapper.
     * @return the object to configure the flatten behavior.
     */
    public <O> MultiFlatten<T, O> produceUni(Function<? super T, ? extends Uni<? extends O>> mapper) {
        nonNull(mapper, "mapper");
        Function<? super T, ? extends Publisher<? extends O>> wrapper = res -> mapper.apply(res).toMulti();
        return new MultiFlatten<>(upstream, wrapper, 1, false);
    }

    /**
     * Configures the <em>mapper</em> of the <em>flatMap</em> operation.
     * The mapper returns a {@link CompletionStage} and is called for each item emitted by the upstream {@link Multi}.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link CompletionStage} produced by the mapper.
     * @return the object to configure the flatten behavior.
     */
    public <O> MultiFlatten<T, O> produceCompletionStage(
            Function<? super T, ? extends CompletionStage<? extends O>> mapper) {
        nonNull(mapper, "mapper");
        Function<? super T, ? extends Publisher<? extends O>> wrapper = res -> Multi.createFrom().emitter(emitter -> {
            CompletionStage<? extends O> stage;
            try {
                stage = mapper.apply(res);
            } catch (Exception e) {
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
                    // failure on `null`
                    emitter.fail(new NullPointerException("The completion stage redeemed `null`"));
                }
            });
        }, BackPressureStrategy.LATEST);

        return new MultiFlatten<>(upstream, wrapper, 1, false);
    }

    /**
     * Ignores the passed items. The resulting {@link Multi} will only be notified when the stream completes or fails.
     *
     * @return the new multi
     */
    public Multi<Void> ignore() {
        return Infrastructure.onMultiCreation(new MultiIgnoreOp<>(upstream));
    }

    /**
     * Ignores the passed items. The resulting {@link Uni} will only be completed with {@code null} when the stream
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
