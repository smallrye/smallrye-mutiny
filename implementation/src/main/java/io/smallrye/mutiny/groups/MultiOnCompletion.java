package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiOnCompletionCall;
import io.smallrye.mutiny.operators.multi.MultiOnCompletionInvoke;
import io.smallrye.mutiny.operators.multi.MultiSwitchOnCompletion;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class MultiOnCompletion<T> {

    private final Multi<T> upstream;

    public MultiOnCompletion(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Creates a new {@link Multi} executing the given {@link Runnable action} when this {@link Multi} completes.
     *
     * @param action the action, must not be {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> invoke(Runnable action) {
        Runnable runnable = Infrastructure.decorate(nonNull(action, "action"));
        return Infrastructure.onMultiCreation(new MultiOnCompletionInvoke<>(upstream, runnable));
    }

    /**
     * Creates a new {@link Multi} executing the given {@link Uni} action when this {@link Multi} completes.
     * The completion notification is sent downstream when the {@link Uni} has completed.
     *
     * @param supplier the supplier, must return a non-{@code null} {@link Uni}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> call(Supplier<Uni<?>> supplier) {
        Supplier<Uni<?>> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return Infrastructure.onMultiCreation(new MultiOnCompletionCall<>(upstream, actual));
    }

    /**
     * When the current {@link Multi} completes, the passed failure is sent downstream.
     *
     * @param failure the failure
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> failWith(Throwable failure) {
        nonNull(failure, "failure");
        return failWith(() -> failure);
    }

    /**
     * When the current {@link Multi} completes, a failure produced by the given {@link Supplier} is sent downstream.
     *
     * @param supplier the supplier to produce the failure, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> failWith(Supplier<Throwable> supplier) {
        Supplier<Throwable> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return switchToEmitter(MultiIfEmpty.createMultiFromFailureSupplier(actual));
    }

    /**
     * Like {@link #failWith(Throwable)} but using a {@link NoSuchElementException}.
     *
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> fail() {
        return failWith(NoSuchElementException::new);
    }

    /**
     * When the upstream {@link Multi} completes, it continues with the events fired with the emitter passed to the
     * {@code consumer} callback.
     * <p>
     * If the upstream {@link Multi} fails, the switch does not apply.
     *
     * @param consumer the callback receiving the emitter to fire the events. Must not be {@code null}. Throwing exception
     *        in this function propagates a failure downstream.
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> switchToEmitter(Consumer<MultiEmitter<? super T>> consumer) {
        Consumer<MultiEmitter<? super T>> actual = Infrastructure.decorate(nonNull(consumer, "consumer"));
        return switchTo(() -> Multi.createFrom().emitter(actual));
    }

    /**
     * When the upstream {@link Multi} completes, it continues with the events fired by the passed
     * {@link Publisher} / {@link Multi}.
     * <p>
     * If the upstream {@link Multi} fails, the switch does not apply.
     *
     * @param other the stream to switch to when the upstream completes.
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> switchTo(Publisher<? extends T> other) {
        return switchTo(() -> other);
    }

    /**
     * When the upstream {@link Multi} completes, it continues with the events fired by a {@link Publisher} produces
     * with the given {@link Supplier}.
     *
     * @param supplier the supplier to use to produce the publisher, must not be {@code null}, must not return {@code null}s
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Multi<T> switchTo(Supplier<Publisher<? extends T>> supplier) {
        Supplier<Publisher<? extends T>> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return Infrastructure.onMultiCreation(new MultiSwitchOnCompletion<>(upstream, actual));
    }

    /**
     * When the upstream {@link Multi} completes, continue with the given items.
     *
     * @param items the items, must not be {@code null}, must not contain {@code null}
     * @return the new {@link Multi}
     */
    @SafeVarargs
    @CheckReturnValue
    public final Multi<T> continueWith(T... items) {
        nonNull(items, "items");
        doesNotContainNull(items, "items");
        return continueWith(() -> Arrays.asList(items));
    }

    /**
     * When the upstream {@link Multi} completes, continue with the given items.
     *
     * @param items the items, must not be {@code null}, must not contain {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> continueWith(Iterable<T> items) {
        nonNull(items, "items");
        doesNotContainNull(items, "items");
        return continueWith(() -> items);
    }

    /**
     * When the upstream {@link Multi} completes, continue with the items produced by the given {@link Supplier}.
     *
     * @param supplier the supplier to produce the items, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> continueWith(Supplier<? extends Iterable<? extends T>> supplier) {
        Supplier<? extends Iterable<? extends T>> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return switchTo(() -> MultiIfEmpty.createMultiFromIterableSupplier(actual));
    }

    @CheckReturnValue
    public MultiIfEmpty<T> ifEmpty() {
        return new MultiIfEmpty<>(upstream);
    }
}
