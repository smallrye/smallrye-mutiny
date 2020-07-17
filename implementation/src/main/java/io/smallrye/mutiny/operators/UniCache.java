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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniCache<I> extends UniOperator<I, I> implements UniSubscriber<I> {

    private static final int NOT_INITIALIZED = 0;
    private static final int SUBSCRIBING = 1;
    private static final int SUBSCRIBED = 2;
    private static final int COMPLETED = 3;
    private final AtomicReference<UniSubscription> subscription = new AtomicReference<>();
    private final List<UniSubscriber<? super I>> subscribers = new ArrayList<>();
    private int state = 0;
    private I item;
    private Throwable failure;

    UniCache(Uni<? extends I> upstream) {
        super(nonNull(upstream, "upstream"));
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super I> subscriber) {
        Runnable action = null;
        synchronized (this) {
            switch (state) {
                case NOT_INITIALIZED:
                    // First subscriber,
                    state = SUBSCRIBING;
                    action = () -> AbstractUni.subscribe(upstream(), this);
                    subscribers.add(subscriber);
                    break;
                case SUBSCRIBING:
                    // Subscription pending
                    subscribers.add(subscriber);
                    break;
                case SUBSCRIBED:
                    // No item yet, but we can provide a Subscription
                    subscribers.add(subscriber);
                    action = () -> subscriber.onSubscribe(() -> onCancellation(subscriber));
                    break;
                case COMPLETED:
                    // Result already computed
                    subscribers.add(subscriber);
                    action = () -> {
                        // We must first pass a subscription
                        subscriber.onSubscribe(() -> onCancellation(subscriber));
                        replay(subscriber);
                    };
                    break;
                default:
                    throw new IllegalStateException("Unknown state: " + state);
            }
        }

        // Execute outside of the synchronized await
        if (action != null) {
            action.run();
        }
    }

    private synchronized void onCancellation(UniSubscriber<? super I> subscriber) {
        subscribers.remove(subscriber);
    }

    private void replay(UniSubscriber<? super I> subscriber) {
        synchronized (this) {
            if (state != COMPLETED) {
                throw new IllegalStateException(
                        "Invalid state - expected being in the DONE state, but is in state: " + state);
            }
        }
        if (failure != null) {
            subscriber.onFailure(failure);
        } else {
            subscriber.onItem(item);
        }
    }

    @Override
    public void onSubscribe(UniSubscription subscription) {
        List<UniSubscriber<? super I>> list;
        synchronized (this) {
            if (!this.subscription.compareAndSet(null, subscription)) {
                throw new IllegalStateException("Invalid state - received a second subscription from source");
            }
            state = SUBSCRIBED;
            list = new ArrayList<>(subscribers);
        }
        list.forEach(s -> s.onSubscribe(() -> onCancellation(s)));
    }

    @Override
    public void onItem(I item) {
        List<UniSubscriber<? super I>> list;
        synchronized (this) {
            if (state != SUBSCRIBED) {
                throw new IllegalStateException(
                        "Invalid state - received item while we where not in the SUBSCRIBED state, current state is: "
                                + state);
            }
            state = COMPLETED;
            this.item = item;
            list = new ArrayList<>(subscribers);
            // Clear the list
            this.subscribers.clear();
        }
        // Here we may notify a subscriber that would have cancelled its subscription just after the synchronized await
        // we consider it as pending cancellation.
        list.forEach(s -> s.onItem(item));
    }

    @Override
    public void onFailure(Throwable failure) {
        List<UniSubscriber<? super I>> list;
        synchronized (this) {
            if (state != SUBSCRIBED) {
                throw new IllegalStateException(
                        "Invalid state - received item while we where not in the SUBSCRIBED state, current state is: "
                                + state);
            }
            state = COMPLETED;
            this.failure = failure;
            list = new ArrayList<>(subscribers);
            // Clear the list
            this.subscribers.clear();
        }
        // Here we may notify a subscriber that would have cancelled its subscription just after the synchronized await
        // we consider it as pending cancellation.
        list.forEach(s -> s.onFailure(failure));
    }
}
