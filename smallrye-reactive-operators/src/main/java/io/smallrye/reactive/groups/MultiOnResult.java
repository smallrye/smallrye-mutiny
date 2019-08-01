package io.smallrye.reactive.groups;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.operators.MultiMapOnResult;
import io.smallrye.reactive.operators.MultiOnResultPeek;
import io.smallrye.reactive.operators.MultiScan;
import io.smallrye.reactive.operators.MultiScanWithInitialState;
import org.reactivestreams.Publisher;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class MultiOnResult<T> {

    private final Multi<T> upstream;

    public MultiOnResult(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }


    /**
     * Produces a new {@link Multi} invoking the given function for each result emitted by the upstream {@link Multi}.
     * <p>
     * The function receives the received result as parameter, and can transform it. The returned object is sent
     * downstream as {@code result} event.
     * <p>
     *
     * @param mapper the mapper function, must not be {@code null}
     * @param <R>    the type of result produced by the mapper function
     * @return the new {@link Multi}
     */
    public <R> Multi<R> mapToResult(Function<? super T, ? extends R> mapper) {
        return new MultiMapOnResult<>(upstream, nonNull(mapper, "mapper"));
    }

    /**
     * Produces a new {@link Multi} invoking the given callback when a {@code result}  event is fired by the upstrea.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Multi<T> consume(Consumer<T> callback) {
        return new MultiOnResultPeek<>(upstream, nonNull(callback, "callback"));
    }

    /**
     * Produces an {@link Multi} emitting the result events based on the upstream events but casted to the target class.
     *
     * @param target the target class
     * @param <O>    the type of result emitted by the produced uni
     * @return the new Uni
     */
    public <O> Multi<O> castTo(Class<O> target) {
        nonNull(target, "target");
        return mapToResult(target::cast);
    }

    /**
     * Produces a {@link Multi} that fires results coming from the reduction of the result emitted by this current
     * {@link Multi} by the passed {@code scanner} reduction function. The produced multi emits the intermediate
     * results.
     * <p>
     * Unlike {@link #scan(BiFunction)}, this operator uses the value produced by the {@code initialStateProducer} as
     * first value.
     *
     * @param scanner the reduction {@link BiFunction}, the resulting {@link Multi} emits the results of this method.
     *                The method is called for every result emitted by this Multi.
     * @return the produced {@link Multi}
     */
    public <S> Multi<S> scan(Supplier<S> initialStateProducer, BiFunction<S, ? super T, S> scanner) {
        nonNull(scanner, "scanner");
        return new MultiScanWithInitialState<>(upstream,
                nonNull(initialStateProducer, "initialStateProducer"),
                nonNull(scanner, "scanner"));
    }

    /**
     * Produces a {@link Multi} that fires results coming from the reduction of the result emitted by this current
     * {@link Multi} by the passed {@code scanner} reduction function. The produced multi emits the intermediate
     * results.
     * <p>
     * Unlike {@link #scan(Supplier, BiFunction)}, this operator doesn't take an initial value but takes the first
     * result emitted by this {@link Multi} as initial value.
     *
     * @param scanner the reduction {@link BiFunction}, the resulting {@link Multi} emits the results of this method.
     *                The method is called for every result emitted by this Multi.
     * @return the produced {@link Multi}
     */
    public Multi<T> scan(BiFunction<T, T, T> scanner) {
        return new MultiScan<>(upstream, nonNull(scanner, "scanner"));
    }

    public <O> Multi<O> flatMap(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        return new MultiFlattenGroup<>(upstream).mapToMulti(mapper);
    }

    public <O> Multi<O> concatMap(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        return new MultiFlattenGroup<>(upstream).preserveOrdering().mapToMulti(mapper);
    }

    public MultiFlattenGroup<T> flatMap() {
        return new MultiFlattenGroup<>(upstream);
    }

    public MultiFlattenGroup<T> concatMap() {
        return new MultiFlattenGroup<>(upstream).preserveOrdering();
    }

    public <O> Multi<O> flatMapUni(Function<? super T, ? extends Uni<? extends O>> mapper) {
        return new MultiFlattenGroup<>(upstream).mapToUni(mapper);
    }

    public <O> Multi<O> concatMapUni(Function<? super T, ? extends Uni<? extends O>> mapper) {
        return new MultiFlattenGroup<>(upstream).preserveOrdering().mapToUni(mapper);
    }


}
