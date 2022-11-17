package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniToMultiPublisher;
import io.smallrye.mutiny.groups.*;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.*;
import io.smallrye.mutiny.subscription.UniSubscriber;

public abstract class AbstractUni<T> implements Uni<T> {

    public abstract void subscribe(UniSubscriber<? super T> subscriber);

    /**
     * Encapsulates subscription to slightly optimize the AbstractUni case.
     * <p>
     * In the case of AbstractUni, it avoid creating the UniSubscribe group instance.
     *
     * @param upstream the upstream, must not be {@code null} (not checked)
     * @param subscriber the subscriber, must not be {@code null} (not checked)
     * @param <T> the type of item
     */
    @SuppressWarnings("unchecked")
    public static <T> void subscribe(Uni<? extends T> upstream, UniSubscriber<? super T> subscriber) {
        if (upstream instanceof AbstractUni) {
            AbstractUni abstractUni = (AbstractUni) upstream;
            UniSubscriber actualSubscriber = Infrastructure.onUniSubscription(upstream, subscriber);
            abstractUni.subscribe(actualSubscriber);
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
    public UniOnSubscribe<T> onSubscription() {
        return new UniOnSubscribe<>(this);
    }

    @Override
    public UniOnItemOrFailure<T> onItemOrFailure() {
        return new UniOnItemOrFailure<>(this);
    }

    @Override
    public UniAwait<T> await() {
        return awaitUsing(null);
    }

    @Override
    public UniAwait<T> awaitUsing(Context context) {
        return new UniAwait<>(this, context);
    }

    @Override
    public Uni<T> emitOn(Executor executor) {
        return Infrastructure.onUniCreation(
                new UniEmitOn<>(this, nonNull(executor, "executor")));
    }

    @Override
    public Uni<T> runSubscriptionOn(Executor executor) {
        return Infrastructure.onUniCreation(
                new UniRunSubscribeOn<>(this, executor));
    }

    @Override
    public UniMemoize<T> memoize() {
        return new UniMemoize<>(this);
    }

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

    @Override
    public Uni<T> log(String identifier) {
        return Infrastructure.onUniCreation(new UniLogger<>(this, identifier));
    }

    @Override
    public Uni<T> log() {
        return log("Uni." + this.getClass().getSimpleName());
    }

    @Override
    public <R> Uni<R> withContext(BiFunction<Uni<T>, Context, Uni<R>> builder) {
        return Infrastructure.onUniCreation(new UniWithContext<>(this, nonNull(builder, "builder")));
    }
}
