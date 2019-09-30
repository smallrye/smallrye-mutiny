package io.smallrye.reactive.unimulti.groups;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import io.smallrye.reactive.unimulti.Multi;
import io.smallrye.reactive.unimulti.Uni;

/**
 * Configures a flatMap operator.
 *
 * @param <I> the type of item emitted by the upstream {@link Multi}.
 */
public class MultiFlatMap<I> {

    private final Multi<I> upstream;

    MultiFlatMap(Multi<I> upstream) {
        this.upstream = upstream;
    }

    /**
     * Configures the <em>mapper</em> of the <em>flatMap</em> operation.
     * The mapper returns a {@link Multi multi} and is called for each item emitted by the upstream {@link Multi}.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Multi} produced by the mapper.
     * @return the object to configure the flatten behavior.
     */
    public <O> MultiFlatten<I, O> multi(Function<? super I, ? extends Publisher<? extends O>> mapper) {
        return publisher(mapper);
    }

    /**
     * Configures the <em>mapper</em> of the <em>flatMap</em> operation.
     * The mapper returns a {@link Publisher publisher} and is called for each item emitted by the upstream {@link Multi}.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Publisher} produced by the mapper.
     * @return the object to configure the flatten behavior.
     */
    public <O> MultiFlatten<I, O> publisher(Function<? super I, ? extends Publisher<? extends O>> mapper) {
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
    public <O> MultiFlatten<I, O> iterable(Function<? super I, ? extends Iterable<? extends O>> mapper) {
        nonNull(mapper, "mapper");
        return publisher((x -> Multi.createFrom().iterable(mapper.apply(x))));
    }

    /**
     * Configures the <em>mapper</em> of the <em>flatMap</em> operation.
     * The mapper returns a {@link Uni uni} and is called for each item emitted by the upstream {@link Multi}.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of item emitted by the {@link Uni} produced by the mapper.
     * @return the object to configure the flatten behavior.
     */
    public <O> MultiFlatten<I, O> uni(Function<? super I, ? extends Uni<? extends O>> mapper) {
        nonNull(mapper, "mapper");
        Function<? super I, ? extends Publisher<? extends O>> wrapper = res -> mapper.apply(res).toMulti();
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
    public <O> MultiFlatten<I, O> completionStage(Function<? super I, ? extends CompletionStage<? extends O>> mapper) {
        nonNull(mapper, "mapper");
        Function<? super I, ? extends Publisher<? extends O>> wrapper = res -> Multi.createFrom()
                .completionStage(mapper.apply(res));
        return new MultiFlatten<>(upstream, wrapper, 1, false);
    }

}
