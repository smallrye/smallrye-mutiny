package io.smallrye.reactive.groups;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.operators.MultiMapOnResult;
import io.smallrye.reactive.operators.MultiOnResultPeek;

import java.util.function.Consumer;
import java.util.function.Function;

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
    public Multi<T> peek(Consumer<T> callback) {
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
}
