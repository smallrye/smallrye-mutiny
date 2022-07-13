package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.groups.MultiResource.getUniFunction;

import java.util.concurrent.Flow.Publisher;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * Allows configuring a <em>finalizer</em> to close the resource attached to the stream.
 * Unlike {@link MultiResource}, this version receives a {@code () -> Uni<R>}, meaning that the resource instance can be
 * resolved asynchronously.
 *
 * @param <R> the type of resource
 * @param <I> the type of item emitted by the resulting {@code Multi}
 * @see MultiCreate#resourceFromUni(Supplier, Function)
 */
public class MultiResourceUni<R, I> {
    private final Function<? super R, ? extends Publisher<I>> streamSupplier;
    private final Supplier<Uni<R>> resourceSupplier;

    public MultiResourceUni(Supplier<Uni<R>> resourceSupplier,
            Function<? super R, ? extends Publisher<I>> streamSupplier) {
        this.resourceSupplier = resourceSupplier;
        this.streamSupplier = streamSupplier;
    }

    /**
     * Configures a <em>synchronous</em> finalizer. The given function is called when the stream completes, fails or
     * when the subscriber cancels.
     * If the finalizer throws an exception, this exception is propagated to the subscriber, unless it has already
     * cancelled.
     *
     * @param finalizer the finalizer, must not be {@code null}
     * @return the multi
     */
    @CheckReturnValue
    public Multi<I> withFinalizer(Consumer<? super R> finalizer) {
        Function<? super R, Uni<Void>> actual = getUniFunction(finalizer);
        return withFinalizer(actual, (r, ignored) -> actual.apply(r), actual);
    }

    /**
     * Configures an <em>asynchronous</em> finalizer. The given function is called when the stream completes, fails or
     * when the subscriber cancels. The returned {@code Uni} is flattened with the stream meaning that the subscriber
     * gets the events fired by the {@code Uni}. If the {@link Uni} completes successfully, the subscriber gets
     * the {@code completion} event. If the {@link Uni} fails, the subscriber gets the failure even if the resource
     * stream completed successfully. If the {@link Uni} fails after a resource stream failure, the subscriber receives
     * a {@link io.smallrye.mutiny.CompositeException}. If the subscribers cancels, the {@link Uni} outcome is ignored.
     * <p>
     * If the finalizer throws an exception, this exception is propagated to the subscriber, unless it has already
     * cancelled.
     * If the finalizer returns {@code null}, a {@link NullPointerException} is propagated to the subscriber, unless it
     * has already cancelled.
     *
     * @param finalizer the finalizer, must not be {@code null}
     * @return the multi
     */
    @CheckReturnValue
    public Multi<I> withFinalizer(Function<? super R, Uni<Void>> finalizer) {
        Function<? super R, Uni<Void>> actual = Infrastructure
                .decorate(ParameterValidation.nonNull(finalizer, "finalizer"));
        return withFinalizer(actual, (r, ignored) -> actual.apply(r), actual);
    }

    /**
     * Configures <em>asynchronous</em> finalizers distinct for each event. The given functions are called when the
     * stream completes, fails or when the subscriber cancels.
     * <p>
     * The returned {@code Uni} is flattened with the stream meaning that the subscriber
     * gets the events fired by the {@code Uni}. If the {@link Uni} completes successfully, the subscriber gets
     * the {@code completion} event. If the {@link Uni} fails, the subscriber gets the failure even if the resource
     * stream completed successfully. If the {@link Uni} fails after a resource stream failure, the subscriber receives
     * a {@link io.smallrye.mutiny.CompositeException}. If the subscribers cancels, the {@link Uni} outcome is ignored.
     * <p>
     * If a finalizer throws an exception, this exception is propagated to the subscriber, unless it has already
     * cancelled.
     * If a finalizer returns {@code null}, a {@link NullPointerException} is propagated to the subscriber, unless it
     * has already cancelled.
     *
     * @param onCompletion the completion finalizer called when the resource stream completes successfully. Must not be
     *        {@code null}
     * @param onFailure the failure finalizer called when the resource stream propagated a failure. The finalizer is
     *        called with the resource and the failure. Must not be {@code null}
     * @param onCancellation the cancellation finalizer called when the subscribers cancels the subscription. Must not
     *        be {@code null}.
     * @return the multi
     */
    @CheckReturnValue
    public Multi<I> withFinalizer(
            Function<? super R, Uni<Void>> onCompletion,
            BiFunction<? super R, ? super Throwable, Uni<Void>> onFailure,
            Function<? super R, Uni<Void>> onCancellation) {

        return Uni.createFrom().deferred(() -> {
            Uni<R> uni;
            try {
                uni = resourceSupplier.get();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }

            if (uni == null) {
                throw new IllegalArgumentException(ParameterValidation.SUPPLIER_PRODUCED_NULL);
            }
            return uni;
        }).onItem().transformToMulti(res -> Multi.createFrom().resource(() -> res, streamSupplier)
                .withFinalizer(onCompletion, onFailure, onCancellation));
    }
}
