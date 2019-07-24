package io.smallrye.reactive.groups;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.operators.UniFlatMapOnResult;
import io.smallrye.reactive.operators.UniMapOnResult;
import io.smallrye.reactive.operators.UniPeekOnEvent;
import io.smallrye.reactive.subscription.UniEmitter;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class UniOnResult<T> {

    private final Uni<T> upstream;

    public UniOnResult(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Produces a new {@link Uni} invoking the given callback when the {@code result}  event is fired. Note that if can
     * be {@code null}.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> peek(Consumer<? super T> callback) {
        return new UniPeekOnEvent<>(upstream, null, nonNull(callback, "callback"), null, null, null);
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} fire the {@code result} event.
     * The function receives the result as parameter, and can transform it. The returned object is sent downstream
     * as {@code result}.
     * <p>
     * For asynchronous composition, see {@link #mapToUni(Function)}.
     *
     * @param mapper the mapper function, must not be {@code null}
     * @param <R> the type of Uni result
     * @return the new {@link Uni}
     */
    public <R> Uni<R> mapToResult(Function<? super T, ? extends R> mapper) {
        return new UniMapOnResult<>(upstream, mapper);
    }

    /**
     * Transforms the received result asynchronously, forwarding the events  emitted by another {@link Uni} produced by
     * the given {@code mapper}.
     * <p>
     * The mapper is called with the result event of the current {@link Uni} and produces an {@link Uni}, possibly
     * using another type of result ({@code R}). The events fired by produced {@link Uni} are forwarded to the
     * {@link Uni} returned by this method.
     * <p>
     * This operation is generally named {@code flatMap}.
     *
     * @param mapper the function called with the result of the this {@link Uni} and producing the {@link Uni},
     *               must not be {@code null}, must not return {@code null}.
     * @param <R>    the type of result
     * @return a new {@link Uni} that would fire events from the uni produced by the mapper function, possibly
     * in an asynchronous manner.
     */
    public <R> Uni<R> mapToUni(Function<? super T, ? extends Uni<? extends R>> mapper) {
        return new UniFlatMapOnResult<>(upstream, mapper);
    }

    /**
     * Transforms the received result asynchronously, forwarding the events emitted an {@link UniEmitter} consumes by
     * the given consumer.
     * <p>
     * The consumer is called with the result event of the current {@link Uni} and an emitter uses to fires events.
     * These events are these propagated by the produced {@link Uni}.
     *
     * @param consumer the function called with the result of the this {@link Uni} and an {@link UniEmitter}.
     *                 It must not be {@code null}.
     * @param <R>      the type of result emitted by the emitter
     * @return a new {@link Uni} that would fire events from the emitter consumed by the mapper function, possibly
     * in an asynchronous manner.
     */
    public <R> Uni<R> mapToUni(BiConsumer<? super T, UniEmitter<? super R>> consumer) {
        nonNull(consumer, "consumer");
        return this.mapToUni(x ->
                Uni.createFrom().emitter(emitter -> consumer.accept(x, emitter)));
    }

    /**
     * Produces a new {@link Uni} receiving a result from upstream and delaying the emission on the {@code result}
     * event (with the same result).
     *
     * @return the object to configure the delay.
     */
    public UniOnResultDelay<T> delayIt() {
        return new UniOnResultDelay<>(upstream, null);
    }

    /**
     * Produces a {@link Uni} ignoring the result of the current {@link Uni} and continuing with either
     * {@link UniOnResultIgnore#andContinueWith(Object) another result}, {@link UniOnResultIgnore#andFail() a failure},
     * or {@link UniOnResultIgnore#andSwitchTo(Uni) another Uni}. The produced {@link Uni} propagates the failure
     * event if the upstream uni fires a failure.
     *
     * <p>Examples:</p>
     * <pre><code>
     *     Uni&lt;T&gt; upstream = ...;
     *     uni = upstream.onResult().ignoreIt().andSwitchTo(other) // Ignore the result from upstream and switch to another uni
     *     uni = upstream.onResult().ignoreIt().andContinueWith(newResult) // Ignore the result from upstream, and fire newResult
     * </code></pre>
     *
     * @return the object to configure the continuation logic.
     */
    public UniOnResultIgnore<T> ignoreIt() {
        return new UniOnResultIgnore<>(this);
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} fires a result. The
     * function transforms the received result into a failure that will be fired by the produced {@link Uni}.
     * For asynchronous composition, see {@link #mapToUni(Function)}}.
     *
     * @param mapper the mapper function, must not be {@code null}, must not return {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> failWith(Function<? super T, ? extends Throwable> mapper) {
        nonNull(mapper, "mapper");
        return mapToUni(t -> Uni.createFrom().failure(mapper.apply(t)));
    }


    /**
     * Produces an {@link Uni} emitting a result based on the upstream result but casted to the target class.
     *
     * @param target the target class
     * @param <O>    the type of result emitted by the produced uni
     * @return the new Uni
     */
    public <O> Uni<O> castTo(Class<O> target) {
        nonNull(target, "target");
        return mapToResult(target::cast);
    }
}
