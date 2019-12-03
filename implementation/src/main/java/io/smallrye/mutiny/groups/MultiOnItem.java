package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.*;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
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
    public <R> Multi<R> mapToItem(Function<? super T, ? extends R> mapper) {
        return new MultiMapOp<>(upstream, nonNull(mapper, "mapper"));
    }

    /**
     * Produces a new {@link Multi} invoking the given callback when an {@code item} event is fired by the upstrea.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Multi<T> invoke(Consumer<T> callback) {
        return new MultiSignalConsumerOp<>(
                upstream,
                null,
                nonNull(callback, "callback"),
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * Ignores the passed items. The resulting {@link Multi} will only be notified when the stream completes or fails.
     *
     * @return the new multi
     */
    public Multi<Void> ignore() {
        return new MultiIgnoreOp<>(upstream);
    }

    /**
     * Ignores the passed items. The resulting {@link Uni} will only be completed with {@code null} when the stream
     * completes or with a failure if the upstream emits a failure..
     *
     * @return the new multi
     */
    public Uni<Void> ignoreAsUni() {
        return new MultiIgnoreOp<>(upstream).toUni();
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
        return mapToItem(target::cast);
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
        return new MultiScanWithSeedOp<>(upstream, initialStateProducer, accumulator);
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
        return new MultiScanOp<>(upstream, accumulator);
    }

    /**
     * Produces a {@link Multi} containing the items from {@link Publisher} produced by the {@code mapper} for each
     * item emitted by this {@link Multi}.
     * <p>
     * The operation behaves as follows:
     * <ul>
     * <li>for each item emitted by this {@link Multi}, the mapper is called and produces a {@link Publisher}
     * (potentially a {@code Multi}). The mapper must not return {@code null}</li>
     * <li>The items emitted by each of the produced {@link Publisher} are then <strong>merged</strong> in the
     * produced {@link Multi}. The flatten process may interleaved items.</li>
     * </ul>
     *
     * @param mapper the {@link Function} producing {@link Publisher} / {@link Multi} for each items emitted by the
     *        upstream {@link Multi}
     * @param <O> the type of item emitted by the {@link Publisher} produced by the {@code mapper}
     * @return the produced {@link Multi}
     */
    public <O> Multi<O> flatMap(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        return flatMap().publisher(mapper).mergeResults();
    }

    /**
     * Produces a {@link Multi} containing the items from {@link Publisher} produced by the {@code mapper} for each
     * item emitted by this {@link Multi}.
     * <p>
     * The operation behaves as follows:
     * <ul>
     * <li>for each item emitted by this {@link Multi}, the mapper is called and produces a {@link Publisher}
     * (potentially a {@code Multi}). The mapper must not return {@code null}</li>
     * <li>The items emitted by each of the produced {@link Publisher} are then <strong>concatenated</strong> in the
     * produced {@link Multi}. The flatten process makes sure that the items are not interleaved.
     * </ul>
     *
     * @param mapper the {@link Function} producing {@link Publisher} / {@link Multi} for each items emitted by the
     *        upstream {@link Multi}
     * @param <O> the type of item emitted by the {@link Publisher} produced by the {@code mapper}
     * @return the produced {@link Multi}
     */
    public <O> Multi<O> concatMap(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        return flatMap().publisher(mapper).concatenateResults();
    }

    /**
     * Configures a <em>flatMap</em> operation that will item into a {@link Multi}.
     * <p>
     * The operations behave as follow:
     * <ul>
     * <li>for each item emitted by this {@link Multi}, a mapper is called and produces a {@link Publisher},
     * {@link Uni} or {@link Iterable}. The mapper must not return {@code null}</li>
     * <li>The items contained in each of the produced <em>sets</em> are then <strong>merged</strong> (flatMap) or
     * <strong>concatenated</strong> (concatMap) in the returned {@link Multi}.</li>
     * </ul>
     * <p>
     * The object returned by this method lets you configure the operation such as the <em>requests</em> asked to the
     * inner {@link Publisher} (produces by the mapper), concurrency (for flatMap)...
     *
     * @return the object to configure the {@code flatMap} operation.
     */
    public MultiFlatMap<T> flatMap() {
        return new MultiFlatMap<>(upstream);
    }

    /**
     * Produces a {@link Multi} containing the items from this {@link Multi} passing the {@code predicate} test.
     *
     * @param predicate the predicate, must not be {@code null}
     * @return the produced {@link Multi}
     */
    public Multi<T> filterWith(Predicate<? super T> predicate) {
        return new MultiFilterOp<>(upstream, nonNull(predicate, "predicate"));
    }

    /**
     * Produces a {@link Multi} containing the items from this {@link Multi} passing the {@code tester}
     * asynchronous test. Unlike {@link #filterWith(Predicate)}, the test is asynchronous. Note that this method
     * preserves ordering of the items, even if the test is asynchronous.
     *
     * @param tester the predicate, must not be {@code null}, must not produce {@code null}
     * @return the produced {@link Multi}
     */
    public Multi<T> testWith(Function<? super T, ? extends Uni<Boolean>> tester) {
        nonNull(tester, "tester");
        return flatMap().multi(res -> {
            Uni<Boolean> uni = tester.apply(res);
            return uni.map(pass -> {
                if (pass) {
                    return res;
                } else {
                    return null;
                }
            }).toMulti();
        }).concatenateResults();
    }

}
