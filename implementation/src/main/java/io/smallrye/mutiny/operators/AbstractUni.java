package io.smallrye.mutiny.operators;

import java.util.concurrent.Executor;
import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniAndGroup;
import io.smallrye.mutiny.groups.UniAwait;
import io.smallrye.mutiny.groups.UniConvert;
import io.smallrye.mutiny.groups.UniIfNoItem;
import io.smallrye.mutiny.groups.UniOnEvent;
import io.smallrye.mutiny.groups.UniOnFailure;
import io.smallrye.mutiny.groups.UniOnItem;
import io.smallrye.mutiny.groups.UniOr;
import io.smallrye.mutiny.groups.UniRepeat;
import io.smallrye.mutiny.groups.UniSubscribe;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.Tuple2;

public abstract class AbstractUni<T> implements Uni<T> {

    protected abstract void subscribing(UniSerializedSubscriber<? super T> subscriber);

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
                new UniCallSubscribeOn<>(this, executor));
    }

    @Override
    public Uni<T> cache() {
        return Infrastructure.onUniCreation(new UniCache<>(this));
    }

    @Override
    public UniConvert<T> convert() {
        return new UniConvert<>(this);
    }

    @Override
    public Multi<T> toMulti() {
        return Multi.createFrom().publisher(convert().toPublisher());
    }

    @Override
    public UniOnEvent<T> on() {
        return new UniOnEvent<>(this);
    }

    @Override
    public UniRepeat<T> repeat() {
        return new UniRepeat<>(this);
    }
}
