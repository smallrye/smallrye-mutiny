/*
 * Copyright (c) 2019-2020 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniOnFailureFlatMap;
import io.smallrye.mutiny.operators.UniOnFailureMap;
import io.smallrye.mutiny.operators.UniOnItemConsume;

/**
 * Configures the failure handler.
 * <p>
 * The upstream uni has sent us a failure, this class lets you decide what need to be done in this case. Typically,
 * you can recover with a fallback item ({@link #recoverWithItem(Object)}), or with another Uni
 * ({@link #recoverWithUni(Uni)}). You can also retry ({@link #retry()}). Maybe, you just want to look at the failure
 * ({@link #invoke(Consumer)}).
 * <p>
 * You can configure the type of failure on which your handler is called using:
 * 
 * <pre>
 * {@code
 * uni.onFailure(IOException.class).recoverWithItem("boom")
 * uni.onFailure(IllegalStateException.class).recoverWithItem("kaboom")
 * uni.onFailure(t -> accept(t)).recoverWithItem("another boom")
 * }
 * </pre>
 *
 * @param <T> the type of item
 */
public class UniOnFailure<T> {

    private final Uni<T> upstream;
    private final Predicate<? super Throwable> predicate;

    public UniOnFailure(Uni<T> upstream, Predicate<? super Throwable> predicate) {
        this.upstream = upstream;
        this.predicate = predicate == null ? x -> true : predicate;
    }

    /**
     * Produces a new {@link Uni} invoking the given callback when this {@link Uni} emits a failure (matching the
     * predicate if set).
     * <p>
     * If the callback throws an exception, a {@link io.smallrye.mutiny.CompositeException} is propagated downstream.
     * This exception is composed by the received failure and the thrown exception.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> invoke(Consumer<Throwable> callback) {
        return Infrastructure.onUniCreation(
                new UniOnItemConsume<>(upstream, null, nonNull(callback, "callback"), predicate));
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} propagates a failure
     * (matching the predicate if set). The function can transform the received failure into another exception that will
     * be fired as failure downstream.
     *
     * Produces a new {@link Uni} invoking the given @{code action} when the {@code failure} event is received.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its item, this item is discarded, and the original {@code failure} is forwarded downstream. If the produced
     * {@code Uni} fails, a composite failure containing both the original failure and the failure from the executed
     * action is propagated downstream.
     *
     * @param action the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> invokeUni(Function<Throwable, ? extends Uni<?>> action) {
        ParameterValidation.nonNull(action, "action");
        return recoverWithUni(failure -> {
            Uni<?> uni = Objects.requireNonNull(action.apply(failure), "The `action` produced a `null` uni");
            //noinspection unchecked
            return (Uni<T>) uni
                    .onItem().failWith(ignored -> failure)
                    .onFailure().apply(subFailure -> new CompositeException(failure, subFailure));
        });
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} propagates a failure. The
     * function can transform the received failure into another exception that will be fired as failure downstream.
     *
     * @param mapper the mapper function, must not be {@code null}, must not return {@code null}
     * @return the new {@link Uni}
     * @deprecated use {@link #transform(Function)}
     */
    @Deprecated
    public Uni<T> apply(Function<? super Throwable, ? extends Throwable> mapper) {
        return transform(mapper);
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} propagates a failure. The
     * function can transform the received failure into another exception that will be fired as failure downstream.
     *
     * @param mapper the mapper function, must not be {@code null}, must not return {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> transform(Function<? super Throwable, ? extends Throwable> mapper) {
        return Infrastructure.onUniCreation(new UniOnFailureMap<>(upstream, predicate, mapper));
    }

    /**
     * Recovers from the received failure (matching the predicate if set) by using a fallback item.
     *
     * @param fallback the fallback, can be {@code null}
     * @return the new {@link Uni} that would emit the given fallback in case the upstream sends us a failure.
     */
    public Uni<T> recoverWithItem(T fallback) {
        return recoverWithItem(() -> fallback);
    }

    /**
     * Recovers from the received failure (matching the predicate if set) by using a item generated by the given
     * supplier. The supplier is called when the failure is received.
     * <p>
     * If the supplier throws an exception, a {@link io.smallrye.mutiny.CompositeException} containing both the received
     * failure and the thrown exception is propagated downstream.
     *
     * @param supplier the supplier providing the fallback item. Must not be {@code null}, can return {@code null}.
     * @return the new {@link Uni} that would emit the produced item in case the upstream sends a failure.
     */
    public Uni<T> recoverWithItem(Supplier<T> supplier) {
        nonNull(supplier, "supplier");
        return recoverWithItem(ignored -> supplier.get());
    }

    /**
     * Recovers from the received failure (matching the predicate if set) by using a item generated by the given
     * function. The function is called when the failure is received.
     * <p>
     * If the function throws an exception, a {@link io.smallrye.mutiny.CompositeException} containing both the received
     * failure and the thrown exception is propagated downstream.
     *
     * @param function the function providing the fallback item. Must not be {@code null}, can return {@code null}.
     * @return the new {@link Uni} that would emit the produced item in case the upstream sends a failure.
     */
    public Uni<T> recoverWithItem(Function<? super Throwable, ? extends T> function) {
        nonNull(function, "function");
        return Infrastructure.onUniCreation(new UniOnFailureFlatMap<>(upstream, predicate, failure -> {
            T newResult = function.apply(failure);
            return Uni.createFrom().item(newResult);
        }));
    }

    /**
     * Recovers from the received failure (matching the predicate if set) with another {@link Uni}. This {@code uni} is
     * produced by the given function. This {@code uni} can emit an item (potentially {@code null} or a failure. The
     * function must not return {@code null}.
     * <p>
     * If the function throws an exception, a {@link io.smallrye.mutiny.CompositeException} containing both the received
     * failure and the thrown exception is propagated downstream.
     *
     * @param function the function providing the fallback uni. Must not be {@code null}, must not return {@code null}.
     * @return the new {@link Uni} that would emit events from the uni produced by the given function in case the
     *         upstream sends a failure.
     */
    public Uni<T> recoverWithUni(Function<? super Throwable, ? extends Uni<? extends T>> function) {
        return Infrastructure.onUniCreation(
                new UniOnFailureFlatMap<>(upstream, predicate, nonNull(function, "function")));
    }

    /**
     * Recovers from the received failure (matching the predicate if set) with another {@link Uni}. This {@code uni} is
     * produced by the given supplier. This {@code uni} can emit an item (potentially {@code null} or a failure. The
     * supplier must not return {@code null}.
     * <p>
     * If the supplier throws an exception, a {@link io.smallrye.mutiny.CompositeException} containing both the received
     * failure and the thrown exception is propagated downstream.
     *
     * @param supplier the supplier providing the fallback uni. Must not be {@code null}, must not return {@code null}.
     * @return the new {@link Uni} that would emits events from the uni produced by the given supplier in case the
     *         upstream sends a failure.
     */
    public Uni<T> recoverWithUni(Supplier<? extends Uni<? extends T>> supplier) {
        return recoverWithUni(ignored -> supplier.get());
    }

    /**
     * Recovers from the received failure (matching the predicate if set) with another {@link Uni}. This {@code uni}
     * can emit an item (potentially {@code null} or a failure.
     *
     * @param fallback the fallbakc uni, must not be {@code null}
     * @return the new {@link Uni} that would emit events from the uni in case the upstream sends a failure.
     */
    public Uni<T> recoverWithUni(Uni<? extends T> fallback) {
        return recoverWithUni(() -> fallback);
    }

    /**
     * Configures the retry strategy.
     *
     * @return the object to configure the retry.
     */
    public UniRetry<T> retry() {
        return new UniRetry<>(upstream, predicate);
    }

}
