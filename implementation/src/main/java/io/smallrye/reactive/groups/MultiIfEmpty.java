package io.smallrye.reactive.groups;

import static io.smallrye.reactive.helpers.ParameterValidation.*;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.operators.MultiSwitchOnEmpty;
import io.smallrye.reactive.subscription.MultiEmitter;

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
    public Multi<T> failWith(Supplier<Throwable> supplier) {
        nonNull(supplier, "supplier");

        return switchToEmitter(createMultiFromFailureSupplier(supplier));
    }

    static <T> Consumer<MultiEmitter<? super T>> createMultiFromFailureSupplier(Supplier<Throwable> supplier) {
        return emitter -> {
            Throwable throwable;
            try {
                throwable = supplier.get();
            } catch (Exception e) {
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
    public Multi<T> switchToEmitter(Consumer<MultiEmitter<? super T>> consumer) {
        nonNull(consumer, "consumer");
        return switchTo(() -> Multi.createFrom().emitter(consumer));
    }

    /**
     * When the upstream {@link Multi} completes without having emitted items, it continues with the events fired by the
     * passed {@link org.reactivestreams.Publisher} / {@link Multi}.
     * <p>
     * If the upstream {@link Multi} fails, the switch does not apply.
     *
     * @param other the stream to switch to when the upstream completes.
     * @return the new {@link Multi}
     */
    public Multi<T> switchTo(Publisher<? extends T> other) {
        return switchTo(() -> other);
    }

    /**
     * When the upstream {@link Multi} completes without having emitted items, it continues with the events fired by a
     * {@link Publisher} produces with the given {@link Supplier}.
     *
     * @param supplier the supplier to use to produce the publisher, must not be {@code null}, must not return {@code null}s
     * @return the new {@link Uni}
     */
    public Multi<T> switchTo(Supplier<Publisher<? extends T>> supplier) {
        return new MultiSwitchOnEmpty<>(upstream, nonNull(supplier, "supplier"));
    }

    /**
     * When the upstream {@link Multi} completes without having emitted items, continue with the given items.
     *
     * @param items the items, must not be {@code null}, must not contain {@code null}
     * @return the new {@link Multi}
     */
    @SafeVarargs
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
    public Multi<T> continueWith(Supplier<? extends Iterable<? extends T>> supplier) {
        nonNull(supplier, "supplier");
        return switchTo(() -> createMultiFromIterableSupplier(supplier));
    }

    static <T> Publisher<? extends T> createMultiFromIterableSupplier(Supplier<? extends Iterable<? extends T>> supplier) {
        Iterable<? extends T> iterable;
        try {
            iterable = supplier.get();
        } catch (Exception e) {
            return Multi.createFrom().failure(e);
        }
        if (iterable == null) {
            return Multi.createFrom().failure(new NullPointerException(SUPPLIER_PRODUCED_NULL));
        } else {
            return Multi.createFrom().iterable(iterable);
        }
    }
}
