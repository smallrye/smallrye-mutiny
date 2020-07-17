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
package io.smallrye.mutiny.operators.multi;

import java.util.function.Consumer;
import java.util.function.LongConsumer;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Attach consumers to the various events and signals received by this {@link org.reactivestreams.Publisher}.
 * Consumer methods can be {@code null}
 *
 * @param <T> the value type
 */
public final class MultiSignalConsumerOp<T> extends AbstractMultiOperator<T, T> {

    private final Consumer<? super T> onItem;

    private final Consumer<? super Throwable> onFailure;

    private final Runnable onCompletion;

    private final Runnable onCancellation;

    private final LongConsumer onRequest;

    public MultiSignalConsumerOp(Multi<? extends T> upstream,
            Consumer<? super T> onItem,
            Consumer<? super Throwable> onFailure,
            Runnable onCompletion,
            LongConsumer onRequest,
            Runnable onCancellation) {
        super(upstream);
        this.onItem = onItem;
        this.onFailure = onFailure;
        this.onCompletion = onCompletion;
        this.onRequest = onRequest;
        this.onCancellation = onCancellation;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        if (actual == null) {
            throw new NullPointerException("Subscriber must not be `null`");
        }
        upstream.subscribe().withSubscriber(new SignalSubscriber(actual));
    }

    private final class SignalSubscriber extends MultiOperatorProcessor<T, T>
            implements MultiSubscriber<T>, Subscription {

        SignalSubscriber(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        void failAndCancel(Throwable throwable) {
            Subscription current = upstream.get();
            if (current != null) {
                current.cancel();
            }
            onFailure(throwable);
        }

        @Override
        public void request(long n) {
            if (onRequest != null) {
                try {
                    onRequest.accept(n);
                } catch (Throwable e) {
                    failAndCancel(e);
                    return;
                }
            }

            upstream.get().request(n);
        }

        @Override
        public void cancel() {
            if (onCancellation != null) {
                try {
                    onCancellation.run();
                } catch (Throwable e) {
                    failAndCancel(e);
                    return;
                }
            }
            upstream.getAndSet(Subscriptions.CANCELLED).cancel();
        }

        @Override
        public void onItem(T t) {
            if (upstream.get() != Subscriptions.CANCELLED) {
                if (onItem != null) {
                    try {
                        onItem.accept(t);
                    } catch (Throwable e) {
                        failAndCancel(e);
                        return;
                    }
                }
                downstream.onItem(t);
            }

        }

        @Override
        public void onFailure(Throwable failure) {
            Subscription up = upstream.getAndSet(Subscriptions.CANCELLED);
            if (up != Subscriptions.CANCELLED) {
                if (onFailure != null) {
                    try {
                        onFailure.accept(failure);
                    } catch (Throwable e) {
                        failure = new CompositeException(failure, e);
                    }
                }

                downstream.onFailure(failure);
            }
        }

        @Override
        public void onCompletion() {
            if (upstream.getAndSet(Subscriptions.CANCELLED) != Subscriptions.CANCELLED) {
                if (onCompletion != null) {
                    try {
                        onCompletion.run();
                    } catch (Throwable e) {
                        downstream.onFailure(e);
                        return;
                    }
                }

                downstream.onCompletion();
            }
        }

    }

}
