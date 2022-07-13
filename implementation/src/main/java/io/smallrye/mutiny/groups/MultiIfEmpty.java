package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiSwitchOnEmpty;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class MultiIfEmpty<T> {

    private final Multi<T> upstream;

    MultiIfEmpty(Multi<T> upstream) {
        this.upstream = upstream;
    }

    /**
     * When the current {@link Multi} completes without having emitted items, the passed failure is sent downstream.
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
     * When the current {@link Multi} completes without having emitted items, a failure produced by the given
     * {@link Supplier} is sent downstream.
     *
     * @param supplier the supplier to produce the failure, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> failWith(Supplier<? extends Throwable> supplier) {
        Supplier<? extends Throwable> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return switchToEmitter(createMultiFromFailureSupplier(actual));
    }

    static <T> Consumer<MultiEmitter<? super T>> createMultiFromFailureSupplier(Supplier<? extends Throwable> supplier) {
        // supplier already decorated.
        return emitter -> {
            Throwable throwable;
            try {
                throwable = supplier.get();
            } catch (Throwable e) {
                emitter.fail(e);
                return;
            }

            if (throwable == null) {
                emitter.fail(new NullPointerException(SUPPLIER_PRODUCED_NULL));
            } else {
                emitter.fail(throwable);
            }
        };
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
     * When the upstream {@link Multi} completes without having emitted items, it continues with the events fired with
     * the emitter passed to the {@code consumer} callback.
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
     * When the upstream {@link Multi} completes without having emitted items, it continues with the events fired by the
     * passed {@link Flow.Publisher} / {@link Multi}.
     * <p>
     * If the upstream {@link Multi} fails, the switch does not apply.
     *
     * @param other the stream to switch to when the upstream completes.
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> switchTo(Flow.Publisher<? extends T> other) {
        return switchTo(() -> other);
    }

    /**
     * When the upstream {@link Multi} completes without having emitted items, it continues with the events fired by a
     * {@link Flow.Publisher} produces with the given {@link Supplier}.
     *
     * @param supplier the supplier to use to produce the publisher, must not be {@code null}, must not return {@code null}s
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Multi<T> switchTo(Supplier<Flow.Publisher<? extends T>> supplier) {
        Supplier<Flow.Publisher<? extends T>> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return Infrastructure.onMultiCreation(new MultiSwitchOnEmpty<>(upstream, actual));
    }

    /**
     * When the upstream {@link Multi} completes without having emitted items, continue with the given items.
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
     * When the upstream {@link Multi} completes without having emitted items, continue with the given items.
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
     * When the upstream {@link Multi} completes without having emitted items, continue with the items produced by the
     * given {@link Supplier}.
     *
     * @param supplier the supplier to produce the items, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> continueWith(Supplier<? extends Iterable<? extends T>> supplier) {
        Supplier<? extends Iterable<? extends T>> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return switchTo(() -> createMultiFromIterableSupplier(actual));
    }

    static <T> Flow.Publisher<? extends T> createMultiFromIterableSupplier(Supplier<? extends Iterable<? extends T>> supplier) {
        Iterable<? extends T> iterable;
        try {
            iterable = supplier.get();
        } catch (Throwable e) {
            return Multi.createFrom().failure(e);
        }
        if (iterable == null) {
            return Multi.createFrom().failure(new NullPointerException(SUPPLIER_PRODUCED_NULL));
        } else {
            return Multi.createFrom().iterable(iterable);
        }
    }
}
