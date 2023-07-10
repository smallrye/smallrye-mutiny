package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.function.BiFunction;
import java.util.function.LongFunction;
import java.util.function.Predicate;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.*;
import io.smallrye.mutiny.helpers.StrictMultiSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.*;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public abstract class AbstractMulti<T> implements Multi<T> {

    public void subscribe(MultiSubscriber<? super T> subscriber) {
        this.subscribe(Infrastructure.onMultiSubscription(this, subscriber));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        // NOTE The Reactive Streams TCK mandates throwing an NPE.
        Objects.requireNonNull(subscriber, "Subscriber is `null`");
        MultiSubscriber<? super T> actual;
        if (subscriber instanceof MultiSubscriber) {
            actual = (MultiSubscriber<? super T>) subscriber;
        } else {
            actual = new StrictMultiSubscriber<>(subscriber);
        }
        this.subscribe(actual);
    }

    @Override
    public MultiOnItem<T> onItem() {
        return new MultiOnItem<>(this);
    }

    @Override
    public MultiSubscribe<T> subscribe() {
        return new MultiSubscribe<>(this);
    }

    @Override
    public Uni<T> toUni() {
        return Uni.createFrom().publisher(this);
    }

    @Override
    public MultiOnFailure<T> onFailure() {
        return new MultiOnFailure<>(this, null);
    }

    @Override
    public MultiOnFailure<T> onFailure(Predicate<? super Throwable> predicate) {
        return new MultiOnFailure<>(this, predicate);
    }

    @Override
    public MultiOnFailure<T> onFailure(Class<? extends Throwable> typeOfFailure) {
        return new MultiOnFailure<>(this, typeOfFailure::isInstance);
    }

    @Override
    public MultiIfNoItem<T> ifNoItem() {
        return new MultiIfNoItem<>(this);
    }

    @Override
    public Multi<T> cache() {
        return Infrastructure.onMultiCreation(new MultiCacheOp<>(this));
    }

    @Override
    public Multi<T> emitOn(Executor executor) {
        return Infrastructure.onMultiCreation(new MultiEmitOnOp<>(this, nonNull(executor, "executor")));
    }

    @Override
    public Multi<T> runSubscriptionOn(Executor executor) {
        return Infrastructure.onMultiCreation(new MultiSubscribeOnOp<>(this, executor));
    }

    @Override
    public MultiOnCompletion<T> onCompletion() {
        return new MultiOnCompletion<>(this);
    }

    @Override
    public MultiSelect<T> select() {
        return new MultiSelect<>(this);
    }

    @Override
    public MultiSkip<T> skip() {
        return new MultiSkip<>(this);
    }

    @Override
    public MultiOverflow<T> onOverflow() {
        return new MultiOverflow<>(this);
    }

    @Override
    public MultiOnSubscribe<T> onSubscription() {
        return new MultiOnSubscribe<>(this);
    }

    @Override
    public MultiBroadcast<T> broadcast() {
        return new MultiBroadcast<>(this);
    }

    @Override
    public MultiConvert<T> convert() {
        return new MultiConvert<>(this);
    }

    @Override
    public MultiOnTerminate<T> onTermination() {
        return new MultiOnTerminate<>(this);
    }

    @Override
    public MultiOnCancel<T> onCancellation() {
        return new MultiOnCancel<>(this);
    }

    @Override
    public MultiOnRequest<T> onRequest() {
        return new MultiOnRequest<>(this);
    }

    @Override
    public MultiCollect<T> collect() {
        return new MultiCollect<>(this);
    }

    @Override
    public MultiGroup<T> group() {
        return new MultiGroup<>(this);
    }

    public Multi<T> toHotStream() {
        BroadcastProcessor<T> processor = BroadcastProcessor.create();
        this.subscribe(processor);
        return processor;
    }

    @Override
    public Multi<T> log(String identifier) {
        return Infrastructure.onMultiCreation(new MultiLogger<>(this, identifier));
    }

    @Override
    public Multi<T> log() {
        return log("Multi." + this.getClass().getSimpleName());
    }

    @Override
    public <R> Multi<R> withContext(BiFunction<Multi<T>, Context, Multi<R>> builder) {
        return Infrastructure.onMultiCreation(new MultiWithContext<>(this, nonNull(builder, "builder")));
    }

    @Override
    public MultiDemandPacing<T> paceDemand() {
        return new MultiDemandPacing<>(this);
    }

    @Override
    public Multi<T> capDemandsUsing(LongFunction<Long> function) {
        return Infrastructure.onMultiCreation(new MultiDemandCapping<>(this, nonNull(function, "function")));
    }
}
