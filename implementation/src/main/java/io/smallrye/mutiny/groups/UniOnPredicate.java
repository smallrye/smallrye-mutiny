package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.*;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.UniEmitter;

/**
 * Group for conditionally handling a Uni depending on a given {@link Predicate}.
 */
public class UniOnPredicate<T> {

    private final Uni<T> upstream;
    private final Predicate<? super T> predicate;

    /**
     * Constructor for creating a group with the given {@code upstream} and {@code predicate}.
     * <p>
     * All methods within the resulting {@link UniOnPredicate} will take the {@code predicate} into account to
     * allow for conditional executions.
     *
     * @param upstream the upstream of this {@code upstream}
     * @param predicate the predicate which should decide whether an action should be executed or not
     */
    public UniOnPredicate(Uni<T> upstream, Predicate<? super T> predicate) {
        this.upstream = nonNull(upstream, "upstream");
        this.predicate = nonNull(predicate, "predicate");
    }

    /**
     * If the current {@link Uni} matches the predicate, the produced {@link Uni} emits the passed failure.
     *
     * @param failure the exception to fire if the current {@link Uni} matches the predicate.
     * @return the new {@link Uni}
     */
    public Uni<T> failWith(Throwable failure) {
        nonNull(failure, "failure");
        return failWith(() -> failure);
    }

    /**
     * If the current {@link Uni} matches the predicate, the produced {@link Uni} emits a failure produced
     * using the given {@link Supplier}.
     *
     * @param supplier the supplier to produce the failure, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> failWith(Supplier<? extends Throwable> supplier) {
        Supplier<? extends Throwable> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));

        return Infrastructure.onUniCreation(upstream.onItem().transformToUni((item, emitter) -> {
            if (!predicate.test(item)) {
                emitter.complete(item);
                return;
            }
            Throwable throwable;
            try {
                throwable = actual.get();
            } catch (Throwable e) {
                emitter.fail(e);
                return;
            }

            if (throwable == null) {
                emitter.fail(new NullPointerException(SUPPLIER_PRODUCED_NULL));
            } else {
                emitter.fail(throwable);
            }
        }));
    }

    /**
     * Produces a new {@link Uni} invoking the given callback if the {@code item} matches the predicate. If the item
     * does not match the predicate, the callback is not invoked.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> invoke(Consumer<? super T> callback) {
        Consumer<? super T> actual = Infrastructure.decorate(nonNull(callback, "callback"));
        // Decoration happens in `invoke`
        return upstream.onItem().invoke(item -> {
            if (predicate.test(item)) {
                actual.accept(item);
            }
        });
    }

    /**
     * Produces a new {@link Uni} invoking the given callback if the {@code item} matches the predicate. If the item
     * does not match the predicate, the callback is not invoked.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> invoke(Runnable callback) {
        Runnable runnable = nonNull(callback, "callback");
        // Decoration happens in `invoke`
        return invoke(x -> runnable.run());
    }

    /**
     * Produces a new {@link Uni} invoking the given {@code action} if the {@code item} matches the predicate. Note
     * that if the received item does not match the predicate, the action is not executed, and the item is propagated
     * downstream.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its result, the result is discarded, and the original (non null) {@code item} is forwarded downstream. If the
     * produced {@code Uni} fails, the failure is propagated downstream.
     *
     * @param action the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> call(Function<? super T, Uni<?>> action) {
        Function<? super T, Uni<?>> actual = Infrastructure.decorate(nonNull(action, "action"));
        return upstream.onItem().call(item -> {
            if (predicate.test(item)) {
                return actual.apply(item);
            } else {
                return Uni.createFrom().nullItem();
            }
        });
    }

    /**
     * Produces a new {@link Uni} invoking the given {@code action} when the {@code item} matches the predicate. Note
     * that if the received item does not match the predicate, the action is not executed.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its result, the result is discarded, and the original (non null) {@code item} is forwarded downstream. If the
     * produced {@code Uni} fails, the failure is propagated downstream.
     *
     * @param action the callback, must not be {@code null} and must not return {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> call(Supplier<Uni<?>> action) {
        Supplier<Uni<?>> actual = Infrastructure.decorate(nonNull(action, "action"));
        return call(ignored -> actual.get());
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} fires the {@code item} event
     * and its item matches the predicate. The function receives the item as parameter, and can transform it. The
     * returned object is sent downstream as {@code item}.
     * <p>
     * If the item does not match the predicate, the mapper is not called and it produces a {@code null} item.
     * <p>
     * Please note that the original item not matching the predicate is <b>not</b> retained in that case since it might
     * not be of the same type as the result of the mapper function. Therefore, to avoid having a pipeline with
     * ambiguous type information, {@code null} is returned instead. If you want to have a different behavior for this
     * case, please use {@link #transformElse(Function, Function)}.
     * <p>
     * Therefore, this method is equivalent to the following:
     * 
     * <pre>
     * {@code
     * uni.onItem().transform(item -> predicate.test(item) ? item : null)
     *    .onItem().ifNotNull().transform(mapper);
     * }
     * </pre>
     * 
     * For asynchronous composition, see {@link #transformToUni(Function)}.
     *
     * @param mapper the mapper function called if the item matches the predicate, must not be {@code null}
     * @param <R> the type of Uni item
     * @return the new {@link Uni}
     */
    public <R> Uni<R> transform(Function<? super T, ? extends R> mapper) {
        return transformElse(mapper, item -> null);
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} fires the {@code item} event
     * and its item matches the predicate. The function receives the item as parameter, and can transform it. The
     * returned object is sent downstream as {@code item}.
     * <p>
     * If the item does not match the predicate, the {@code elseMapper} is called instead of the {@code mapper}. This
     * allows to have different mapping functions be called depending on whether the item matches the predicate or not.
     * <p>
     * For asynchronous composition, see {@link #transformToUniElse(Function, Function)}.
     *
     * @param mapper the mapper function called if the item matches the predicate, must not be {@code null}
     * @param elseMapper the mapper function in case the predicate is not matched, must not be {@code null}
     * @param <R> the type of Uni item
     * @return the new {@link Uni}
     */
    public <R> Uni<R> transformElse(Function<? super T, ? extends R> mapper, Function<? super T, ? extends R> elseMapper) {
        Function<? super T, ? extends R> actual = Infrastructure.decorate(nonNull(mapper, "mapper"));
        Function<? super T, ? extends R> elseActual = Infrastructure.decorate(nonNull(elseMapper, "elseMapper"));
        return upstream.onItem().transform(item -> {
            if (predicate.test(item)) {
                return actual.apply(item);
            } else {
                return elseActual.apply(item);
            }
        });
    }

    /**
     * Transforms the received item asynchronously, forwarding the events emitted by another {@link Uni} produced by
     * the given {@code mapper}.
     * <p>
     * The mapper is called with the item event of the current {@link Uni} (if it matches the predicate) and produces a
     * {@link Uni}, possibly using another type of item ({@code R}). The events fired by produced {@link Uni} are
     * forwarded to the {@link Uni} returned by this method.
     * <p>
     * If the item does not match the predicate, the mapper is not called, and {@code null} is propagated downstream.
     * <p>
     * Please note that the original item not matching the predicate is <b>not</b> retained in that case since it might
     * not be of the same type as the result of the mapper function. Therefore, to avoid having a pipeline with
     * ambiguous type information, {@code null} is returned instead. If you want to have a different behavior for this
     * case, please use {@link #transformToUniElse(Function, Function)}.
     * <p>
     * Therefore, this method is equivalent to the following:
     * 
     * <pre>
     * {@code
     * uni.onItem().transform(item -> predicate.test(item) ? item : null)
     *    .onItem().ifNotNull().transformToUni(mapper);
     * }
     * </pre>
     * 
     * This operation is generally named {@code flatMap}.
     *
     * @param mapper the function called with the item of this {@link Uni} and producing the {@link Uni},
     *        must not be {@code null}, must not return {@code null}.
     * @param <R> the type of item
     * @return a new {@link Uni} that would fire events from the uni produced by the mapper function, possibly
     *         in an asynchronous manner.
     */
    public <R> Uni<R> transformToUni(Function<? super T, Uni<? extends R>> mapper) {
        return transformToUniElse(mapper, item -> Uni.createFrom().nullItem());
    }

    /**
     * Transforms the received item asynchronously, forwarding the events emitted by another {@link Uni} produced by
     * the given {@code mapper} or {@code elseMapper}, depending on whether the predicate is matched or not.
     * <p>
     * The mapper is called with the item event of the current {@link Uni} (if it matches the predicate) and produces a
     * {@link Uni}, possibly using another type of item ({@code R}). The events fired by produced {@link Uni} are
     * forwarded to the {@link Uni} returned by this method.
     * <p>
     * If the item does not match the predicate, the {@code elseMapper} is called instead of the {@code mapper}. This
     * allows to have different mapping functions be called depending on whether the item matches the predicate or not.
     * <p>
     * This operation is generally named {@code flatMap}.
     *
     * @param mapper the function called with the item of this {@link Uni} and producing the {@link Uni} if the item
     *        matches the predicate, must not be {@code null}, must not return {@code null}
     * @param elseMapper the function with the item of this {@link Uni} and producing the {@link Uni} in case the
     *        predicate is not matched, must not be {@code null}, must not return {@code null}
     * @param <R> the type of item
     * @return a new {@link Uni} that would fire events from the uni produced by the mapper function, possibly
     *         in an asynchronous manner.
     */
    public <R> Uni<R> transformToUniElse(Function<? super T, Uni<? extends R>> mapper,
            Function<? super T, Uni<? extends R>> elseMapper) {
        Function<? super T, Uni<? extends R>> actual = Infrastructure.decorate(nonNull(mapper, "mapper"));
        Function<? super T, Uni<? extends R>> elseActual = Infrastructure.decorate(nonNull(elseMapper, "elseMapper"));
        return upstream.onItem().transformToUni(item -> {
            if (predicate.test(item)) {
                return actual.apply(item);
            } else {
                return elseActual.apply(item);
            }
        });
    }

    /**
     * When this {@code Uni} produces its item (and matches the predicate), call the given {@code mapper} to produce
     * a {@link Publisher}. Continue the pipeline with this publisher (as a {@link Multi}).
     * <p>
     * The mapper is called with the item event of the current {@link Uni} and produces a {@link Publisher}, possibly
     * using another type of item ({@code R}). Events fired by the produced {@link Publisher} are forwarded to the
     * {@link Multi} returned by this method.
     * <p>
     * If the item does not match the predicate, the mapper is not called and an empty {@link Multi} is produced.
     * <p>
     * Please note that the original item not matching the predicate is <b>not</b> retained in that case since it might
     * not be of the same type as the result of the mapper function. Therefore, to avoid having a pipeline with
     * ambiguous type information, an empty {@link Multi} is returned instead. If you want to have a different behavior
     * for this case, please use {@link #transformToMultiElse(Function, Function)}.
     * <p>
     * Therefore, this method is equivalent to the following:
     * 
     * <pre>
     * {@code
     * uni.onItem().transform(item -> predicate.test(item) ? item : null)
     *    .onItem().ifNotNull().transformToMulti(mapper);
     * }
     * </pre>
     * 
     * This operation is generally named {@code flatMapPublisher}.
     *
     * @param mapper the mapper, must not be {@code null}, may expect to receive {@code null} as item.
     * @param <R> the type of item produced by the resulting {@link Multi}
     * @return the multi
     */
    public <R> Multi<R> transformToMulti(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return transformToMultiElse(mapper, item -> Multi.createFrom().empty());
    }

    /**
     * When this {@code Uni} produces its item (and matches the predicate), call the given {@code mapper} to produce
     * a {@link Publisher}. Continue the pipeline with this publisher (as a {@link Multi}).
     * <p>
     * The mapper is called with the item event of the current {@link Uni} and produces a {@link Publisher}, possibly
     * using another type of item ({@code R}). Events fired by the produced {@link Publisher} are forwarded to the
     * {@link Multi} returned by this method.
     * <p>
     * If the item does not match the predicate, the {@code elseMapper} is called instead of the {@code mapper}. This
     * allows to have different mapping functions be called depending on whether the item matches the predicate or not.
     * <p>
     * This operation is generally named {@code flatMapPublisher}.
     *
     * @param mapper the mapper function called if the item matches the predicate, must not be {@code null}, may expect
     *        to receive {@code null} as item
     * @param elseMapper the mapper function in case the predicate is not matched, must not be {@code null}, may expect
     *        to receive {@code null} as item
     * @param <R> the type of item produced by the resulting {@link Multi}
     * @return the multi
     */
    public <R> Multi<R> transformToMultiElse(Function<? super T, ? extends Publisher<? extends R>> mapper,
            Function<? super T, ? extends Publisher<? extends R>> elseMapper) {
        Function<? super T, ? extends Publisher<? extends R>> actual = Infrastructure
                .decorate(nonNull(mapper, "mapper"));
        Function<? super T, ? extends Publisher<? extends R>> elseActual = Infrastructure
                .decorate(nonNull(elseMapper, "elseMapper"));
        return upstream.onItem().transformToMulti(item -> {
            if (predicate.test(item)) {
                return actual.apply(item);
            } else {
                return elseActual.apply(item);
            }
        });
    }

    /**
     * Transforms the received item asynchronously, forwarding the events emitted an {@link UniEmitter} consumed by
     * the given consumer.
     * <p>
     * The consumer is called with the item event of the current {@link Uni} and an emitter used to fire events.
     * These events are then propagated by the produced {@link Uni}.
     * <p>
     * If the item does not match the predicate, the {@code consumer} is not called and a {@code null} item is propagated
     * downstream.
     * <p>
     * Please note that the original item not matching the predicate is <b>not</b> retained in that case since it might
     * not be of the same type as the result of the mapper function. Therefore, to avoid having a pipeline with
     * ambiguous type information, {@code null} is returned instead.. If you want to have a different behavior
     * for this case, please use {@link #transformToUniElse(BiConsumer, BiConsumer)}.
     * <p>
     * Therefore, this method is equivalent to the following:
     * 
     * <pre>
     * {@code
     * uni.onItem().transform(item -> predicate.test(item) ? item : null)
     *    .onItem().ifNotNull().transformToUni(consumer);
     * }
     * </pre>
     *
     * @param consumer the function called with the item of the this {@link Uni} and an {@link UniEmitter} if the item
     *        matches the predicate, must not be {@code null}
     * @param <R> the type of item emitted by the emitter
     * @return a new {@link Uni} that would fire events from the emitter consumed by the mapper function, possibly
     *         in an asynchronous manner.
     */
    public <R> Uni<R> transformToUni(BiConsumer<? super T, UniEmitter<? super R>> consumer) {
        return transformToUniElse(consumer, (item, emitter) -> emitter.complete(null));
    }

    /**
     * Transforms the received item asynchronously, forwarding the events emitted an {@link UniEmitter} consumed by
     * the given consumer.
     * <p>
     * The consumer is called with the item event of the current {@link Uni} and an emitter used to fire events.
     * These events are then propagated by the produced {@link Uni}.
     * <p>
     * If the item does not match the predicate, the {@code elseConsumer} is called instead of the {@code consumer}.
     * This allows to have different mapping functions be called depending on whether the item matches the predicate or
     * not.
     *
     * @param consumer the function called with the item of the this {@link Uni} and an {@link UniEmitter} if the item
     *        matches the predicate, must not be {@code null}
     * @param elseConsumer the function called with the item of the this {@link Uni} and an {@link UniEmitter} in case
     *        the item does not match the predicate, must not be {@code null}
     * @param <R> the type of item emitted by the emitter
     * @return a new {@link Uni} that would fire events from the emitter consumed by the mapper function, possibly
     *         in an asynchronous manner.
     */
    public <R> Uni<R> transformToUniElse(BiConsumer<? super T, UniEmitter<? super R>> consumer,
            BiConsumer<? super T, UniEmitter<? super R>> elseConsumer) {
        BiConsumer<? super T, UniEmitter<? super R>> actual = Infrastructure.decorate(nonNull(consumer, "consumer"));
        BiConsumer<? super T, UniEmitter<? super R>> elseActual = Infrastructure
                .decorate(nonNull(elseConsumer, "elseConsumer"));
        return upstream.onItem().transformToUni((item, emitter) -> {
            if (predicate.test(item)) {
                actual.accept(item, emitter);
            } else {
                elseActual.accept(item, emitter);
            }
        });
    }
}
