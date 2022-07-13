package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Flow.Publisher;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.UniEmitter;

public class UniOnNotNull<T> {

    private final Uni<T> upstream;

    public UniOnNotNull(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Produces a new {@link Uni} invoking the given callback when the {@code item} event is fired. If the item is
     * {@code null}, the callback is not invoked.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> invoke(Consumer<? super T> callback) {
        Consumer<? super T> actual = Infrastructure.decorate(nonNull(callback, "callback"));
        // Decoration happens in `invoke`
        return upstream.onItem().invoke(item -> {
            if (item != null) {
                actual.accept(item);
            }
        });
    }

    /**
     * Produces a new {@link Uni} invoking the given callback when the {@code item} event is fired. If the item is
     * {@code null}, the callback is not invoked.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> invoke(Runnable callback) {
        Runnable runnable = nonNull(callback, "callback");
        // Decoration happens in `invoke`
        return invoke(x -> runnable.run());
    }

    /**
     * Produces a new {@link Uni} invoking the given @{code action} when the {@code item} event is received. Note that
     * if the received item is {@code null}, the action is not executed, and the item is propagated downstream.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its result, the result is discarded, and the original (non null) {@code item} is forwarded downstream. If the
     * produced {@code Uni} fails, the failure is propagated downstream.
     *
     * @param action the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> call(Function<? super T, Uni<?>> action) {
        Function<? super T, Uni<?>> actual = Infrastructure.decorate(nonNull(action, "action"));
        return upstream.onItem().call(item -> {
            if (item != null) {
                return actual.apply(item);
            } else {
                return Uni.createFrom().nullItem();
            }
        });
    }

    /**
     * Produces a new {@link Uni} invoking the given @{code action} when the {@code item} event is received. Note that
     * if the received item is {@code null}, the action is not executed.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its result, the result is discarded, and the original (non null) {@code item} is forwarded downstream. If the
     * produced {@code Uni} fails, the failure is propagated downstream.
     *
     * @param action the callback, must not be {@code null} and must not return {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> call(Supplier<Uni<?>> action) {
        Supplier<Uni<?>> actual = Infrastructure.decorate(nonNull(action, "action"));
        return call(ignored -> actual.get());
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} fires the {@code item} event.
     * The function receives the (non-null) item as parameter, and can transform it. The returned object is sent downstream
     * as {@code item}.
     * <p>
     * If the item is `null`, the mapper is not called and it produces a {@code null} item.
     * <p>
     * For asynchronous composition, see {@link #transformToUni(Function)}.
     *
     * @param mapper the mapper function, must not be {@code null}
     * @param <R> the type of Uni item
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public <R> Uni<R> transform(Function<? super T, ? extends R> mapper) {
        Function<? super T, ? extends R> actual = Infrastructure.decorate(nonNull(mapper, "mapper"));
        return upstream.onItem().transform(item -> {
            if (item != null) {
                return actual.apply(item);
            } else {
                return null;
            }
        });
    }

    /**
     * Transforms the received item asynchronously, forwarding the events emitted by another {@link Uni} produced by
     * the given {@code mapper}.
     * <p>
     * The mapper is called with the item event of the current {@link Uni} and produces an {@link Uni}, possibly
     * using another type of item ({@code R}). The events fired by produced {@link Uni} are forwarded to the
     * {@link Uni} returned by this method.
     * <p>
     * If the item is {@code null}, the mapper is not called, and {@code null} is propagated downstream.
     * <p>
     * This operation is generally named {@code flatMap}.
     *
     * @param mapper the function called with the item of this {@link Uni} and producing the {@link Uni},
     *        must not be {@code null}, must not return {@code null}.
     * @param <R> the type of item
     * @return a new {@link Uni} that would fire events from the uni produced by the mapper function, possibly
     *         in an asynchronous manner.
     */
    @CheckReturnValue
    public <R> Uni<R> transformToUni(Function<? super T, Uni<? extends R>> mapper) {
        Function<? super T, Uni<? extends R>> actual = Infrastructure.decorate(nonNull(mapper, "mapper"));
        return upstream.onItem().transformToUni(item -> {
            if (item != null) {
                return actual.apply(item);
            } else {
                return Uni.createFrom().nullItem();
            }
        });
    }

    /**
     * When this {@code Uni} produces its item (not {@code null}), call the given {@code mapper} to produce
     * a {@link Publisher}. Continue the pipeline with this publisher (as a {@link Multi}).
     * <p>
     * The mapper is called with the item event of the current {@link Uni} and produces a {@link Publisher}, possibly
     * using another type of item ({@code R}). Events fired by the produced {@link Publisher} are forwarded to the
     * {@link Multi} returned by this method.
     * <p>
     * If the item is `null`, the mapper is not called and an empty {@link Multi} is produced.
     * <p>
     * This operation is generally named {@code flatMapPublisher}.
     *
     * @param mapper the mapper, must not be {@code null}, may expect to receive {@code null} as item.
     * @param <R> the type of item produced by the resulting {@link Multi}
     * @return the multi
     */
    @CheckReturnValue
    public <R> Multi<R> transformToMulti(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        Function<? super T, ? extends Publisher<? extends R>> actual = Infrastructure
                .decorate(nonNull(mapper, "mapper"));
        return upstream.onItem().transformToMulti(item -> {
            if (item != null) {
                return actual.apply(item);
            } else {
                return Multi.createFrom().empty();
            }
        });
    }

    /**
     * Transforms the received item asynchronously, forwarding the events emitted an {@link UniEmitter} consumes by
     * the given consumer.
     * <p>
     * The consumer is called with the item event of the current {@link Uni} and an emitter uses to fire events.
     * These events are these propagated by the produced {@link Uni}.
     * <p>
     * If the incoming item is {@code null}, the {@code consumer} is not called and a {@code null} item is propagated
     * downstream.
     *
     * @param consumer the function called with the item of the this {@link Uni} and an {@link UniEmitter}.
     *        It must not be {@code null}.
     * @param <R> the type of item emitted by the emitter
     * @return a new {@link Uni} that would fire events from the emitter consumed by the mapper function, possibly
     *         in an asynchronous manner.
     */
    @CheckReturnValue
    public <R> Uni<R> transformToUni(BiConsumer<? super T, UniEmitter<? super R>> consumer) {
        BiConsumer<? super T, UniEmitter<? super R>> actual = Infrastructure.decorate(nonNull(consumer, "consumer"));
        return upstream.onItem().transformToUni((item, emitter) -> {
            if (item != null) {
                actual.accept(item, emitter);
            } else {
                emitter.complete(null);
            }
        });
    }

    /**
     * If the current {@link Uni} emits an item, the produced {@link Uni} emits the retrieved failure
     * using the given {@link Supplier}.
     *
     * @param supplier the supplier to produce the failure, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> failWith(Supplier<? extends Throwable> supplier) {
        Supplier<? extends Throwable> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return transformToUni(ignored -> {
            Throwable failure = ParameterValidation.nonNull(actual.get(), "supplier");
            return Uni.createFrom().failure(failure);
        });
    }

    /**
     * If the current {@link Uni} emits an item, the produced {@link Uni} emits the given failure.
     *
     * @param failure the exception to fire if the current {@link Uni} emits an item. Must not be {@code null}.
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> failWith(Throwable failure) {
        nonNull(failure, "failure");
        return failWith(() -> failure);
    }
}
