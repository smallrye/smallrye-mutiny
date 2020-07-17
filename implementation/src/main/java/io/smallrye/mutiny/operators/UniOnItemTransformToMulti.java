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

import static io.smallrye.mutiny.helpers.ParameterValidation.MAPPER_RETURNED_NULL;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnItemTransformToMulti<I, O> extends AbstractMulti<O> {

    private final Function<? super I, ? extends Publisher<? extends O>> mapper;
    private final Uni<I> upstream;

    public UniOnItemTransformToMulti(Uni<I> upstream, Function<? super I, ? extends Publisher<? extends O>> mapper) {
        this.upstream = nonNull(upstream, "upstream");
        this.mapper = nonNull(mapper, "mapper");
    }

    @Override
    public void subscribe(MultiSubscriber<? super O> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException("The subscriber must not be `null`");
        }
        AbstractUni.subscribe(upstream, new FlatMapPublisherSubscriber<>(subscriber, mapper));
    }

    @SuppressWarnings("SubscriberImplementation")
    static final class FlatMapPublisherSubscriber<I, O> implements Subscriber<O>, UniSubscriber<I>, Subscription {

        private AtomicReference<Subscription> secondUpstream;
        private AtomicReference<UniSubscription> firstUpstream;
        private final Subscriber<? super O> downstream;
        private final Function<? super I, ? extends Publisher<? extends O>> mapper;
        private final AtomicLong requested = new AtomicLong();

        FlatMapPublisherSubscriber(Subscriber<? super O> downstream,
                Function<? super I, ? extends Publisher<? extends O>> mapper) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.firstUpstream = new AtomicReference<>();
            this.secondUpstream = new AtomicReference<>();
        }

        @Override
        public void onNext(O item) {
            downstream.onNext(item);
        }

        @Override
        public void onError(Throwable failure) {
            downstream.onError(failure);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void request(long n) {
            Subscriptions.requestIfNotNullOrAccumulate(secondUpstream, requested, n);
        }

        @Override
        public void cancel() {
            UniSubscription subscription = firstUpstream.getAndSet(EmptyUniSubscription.CANCELLED);
            if (subscription != null && subscription != EmptyUniSubscription.CANCELLED) {
                subscription.cancel();
            }
            Subscriptions.cancel(secondUpstream);
        }

        /**
         * Called when we get the subscription from the upstream UNI
         *
         * @param subscription the subscription allowing to cancel the computation.
         */
        @Override
        public void onSubscribe(UniSubscription subscription) {
            if (firstUpstream.compareAndSet(null, subscription)) {
                downstream.onSubscribe(this);
            }
        }

        /**
         * Called after we produced the {@link Publisher} and subscribe on it.
         *
         * @param subscription the subscription from the produced {@link Publisher}
         */
        @Override
        public void onSubscribe(Subscription subscription) {
            if (secondUpstream.compareAndSet(null, subscription)) {
                long r = requested.getAndSet(0L);
                if (r != 0L) {
                    subscription.request(r);
                }
            }
        }

        @Override
        public void onItem(I item) {
            Publisher<? extends O> publisher;

            try {
                publisher = mapper.apply(item);
                if (publisher == null) {
                    throw new NullPointerException(MAPPER_RETURNED_NULL);
                }
            } catch (Throwable ex) {
                downstream.onError(ex);
                return;
            }

            publisher.subscribe(this);
        }

        @Override
        public void onFailure(Throwable failure) {
            downstream.onError(failure);
        }
    }
}
