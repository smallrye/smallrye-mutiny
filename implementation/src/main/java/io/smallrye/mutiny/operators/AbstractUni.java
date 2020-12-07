package io.smallrye.mutiny.operators;

import java.util.concurrent.Executor;
import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniToMultiPublisher;
import io.smallrye.mutiny.groups.*;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.tuples.Tuple2;

public abstract class AbstractUni<T> implements Uni<T> {

    protected abstract void subscribing(UniSubscriber<? super T> subscriber);

    /**
     * Encapsulates subscription to slightly optimized the AbstractUni case.
     *
     * In the case of AbstractUni, it avoid creating the UniSubscribe group instance.
     *
     * @param upstream the upstream, must not be {@code null} (not checked)
     * @param subscriber the subscriber, must not be {@code null} (not checked)
     * @param <T> the type of item
     */
    public static <T> void subscribe(Uni<? extends T> upstream, UniSubscriber<? super T> subscriber) {
        if (upstream instanceof AbstractUni) {
            //noinspection unchecked
            UniSerializedSubscriber.subscribe((AbstractUni<? extends T>) upstream, subscriber);
        } else {
            upstream.subscribe().withSubscriber(subscriber);
        }
    }

    @Override
    public UniSubscribe<T> subscribe() {
        return new UniSubscribe<>(this);
    }

    @Override
    public UniOnItem<T> onItem() {
        return new UniOnItem<>(this);
    }

    @Override
    public UniIfNoItem<T> ifNoItem() {
        return new UniIfNoItem<>(this);
    }

    @Override
    public UniOnFailure<T> onFailure() {
        return new UniOnFailure<>(this, null);
    }

    @Override
    public UniOnFailure<T> onFailure(Predicate<? super Throwable> predicate) {
        return new UniOnFailure<>(this, predicate);
    }

    @Override
    public UniOnFailure<T> onFailure(Class<? extends Throwable> typeOfFailure) {
        return new UniOnFailure<>(this, typeOfFailure::isInstance);
    }

    @Override
    public UniOnSubscribe<T> onSubscribe() {
        return new UniOnSubscribe<>(this);
    }

    @Override
    public UniOnItemOrFailure<T> onItemOrFailure() {
        return new UniOnItemOrFailure<>(this);
    }

    @Override
    public UniAndGroup<T> and() {
        return new UniAndGroup<>(this);
    }

    @Override
    public <T2> Uni<Tuple2<T, T2>> and(Uni<T2> other) {
        return Infrastructure.onUniCreation(
                new UniAndGroup<>(this).uni(ParameterValidation.nonNull(other, "other")).asTuple());
    }

    @Override
    public UniOr<T> or() {
        return new UniOr<>(this);
    }

    @Override
    public UniAwait<T> await() {
        return new UniAwait<>(this);
    }

    @Override
    public Uni<T> emitOn(Executor executor) {
        return Infrastructure.onUniCreation(
                new UniEmitOn<>(this, ParameterValidation.nonNull(executor, "executor")));
    }

    @Override
    public Uni<T> runSubscriptionOn(Executor executor) {
        return Infrastructure.onUniCreation(
                new UniRunSubscribeOn<>(this, executor));
    }

    @Override
    public UniMemoize<T> memoize() {
        return new UniMemoize<T>(this);
    }

    @Override
    public Uni<T> cache() {
        return Infrastructure.onUniCreation(new UniMemoizeOp<>(this));
    }

    @Override
    public UniConvert<T> convert() {
        return new UniConvert<>(this);
    }

    @Override
    public Multi<T> toMulti() {
        return Multi.createFrom().safePublisher(new UniToMultiPublisher<>(this));
    }

    @Override
    public UniOnEvent<T> on() {
        return new UniOnEvent<>(this);
    }

    @Override
    public UniRepeat<T> repeat() {
        return new UniRepeat<>(this);
    }

    @Override
    public UniOnTerminate<T> onTermination() {
        return new UniOnTerminate<>(this);
    }

    @Override
    public UniOnCancel<T> onCancellation() {
        return new UniOnCancel<>(this);
    }
}
