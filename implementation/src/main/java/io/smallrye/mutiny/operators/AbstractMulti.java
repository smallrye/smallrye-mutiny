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
package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Executor;
import java.util.function.Predicate;

import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.*;
import io.smallrye.mutiny.helpers.StrictMultiSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiCacheOp;
import io.smallrye.mutiny.operators.multi.MultiEmitOnOp;
import io.smallrye.mutiny.operators.multi.MultiSubscribeOnOp;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public abstract class AbstractMulti<T> implements Multi<T> {

    public void subscribe(MultiSubscriber<? super T> subscriber) {
        this.subscribe(Infrastructure.onMultiSubscription(this, subscriber));
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        if (subscriber == null) {
            // NOTE The Reactive Streams TCK mandates throwing an NPE.
            throw new NullPointerException("Subscriber is `null`");
        }

        this.subscribe(new StrictMultiSubscriber<>(subscriber));
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
    public MultiOnEvent<T> on() {
        return new MultiOnEvent<>(this);
    }

    @Override
    public Multi<T> cache() {
        return Infrastructure.onMultiCreation(new MultiCacheOp<>(this));
    }

    @Override
    public MultiCollect<T> collectItems() {
        return new MultiCollect<>(this);
    }

    @Override
    public MultiGroup<T> groupItems() {
        return new MultiGroup<>(this);
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
    public MultiTransform<T> transform() {
        return new MultiTransform<>(this);
    }

    @Override
    public MultiOverflow<T> onOverflow() {
        return new MultiOverflow<>(this);
    }

    @Override
    public MultiOnSubscribe<T> onSubscribe() {
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

}
