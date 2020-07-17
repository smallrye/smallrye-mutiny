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
package io.smallrye.mutiny.subscription;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.Subscriptions;

public class Subscribers {

    public static <T> CancellableSubscriber<T> cancelled() {
        return new CancellationSubscriber<>();
    }

    @SuppressWarnings("ThrowableNotThrown")
    private static final Consumer<? super Throwable> NO_ON_FAILURE = failure -> new Exception(
            "Missing onError method in the subscriber", failure).printStackTrace(); // NOSONAR

    public static <T> CancellableSubscriber<T> from(Consumer<? super T> onItem) {
        return new CallbackBasedSubscriber<>(onItem, NO_ON_FAILURE, null, null);
    }

    public static <T> CancellableSubscriber<T> from(Consumer<? super T> onItem, Consumer<? super Throwable> onFailure) {
        return new CallbackBasedSubscriber<>(onItem, onFailure, null, null);
    }

    public static <T> CancellableSubscriber<T> from(Consumer<? super T> onItem, Consumer<? super Throwable> onFailure,
            Runnable onCompletion) {
        return new CallbackBasedSubscriber<>(onItem, onFailure, onCompletion, null);
    }

    public static <T> CancellableSubscriber<T> from(Consumer<? super T> onItem, Consumer<? super Throwable> onFailure,
            Runnable onCompletion,
            Consumer<? super Subscription> onSubscription) {
        return new CallbackBasedSubscriber<>(onItem, onFailure, onCompletion, onSubscription);
    }

    private static class CancellationSubscriber<T> implements CancellableSubscriber<T> {
        @Override
        public void onSubscribe(Subscription s) {
            s.cancel();
        }

        @Override
        public void onItem(T t) {
            // Ignored
        }

        @Override
        public void onFailure(Throwable t) {
            // Ignored
        }

        @Override
        public void onCompletion() {
            // Ignored
        }

        @Override
        public void cancel() {
            // already cancelled, so ignoring.
        }
    }

    private static class CallbackBasedSubscriber<T> implements CancellableSubscriber<T>, Subscription {

        private final AtomicReference<Subscription> subscription = new AtomicReference<>();
        private final Consumer<? super T> onItem;
        private final Consumer<? super Throwable> onFailure;
        private final Runnable onCompletion;
        private final Consumer<? super Subscription> onSubscription;

        public CallbackBasedSubscriber(
                Consumer<? super T> onItem,
                Consumer<? super Throwable> onFailure,
                Runnable onCompletion,
                Consumer<? super Subscription> onSubscription) {
            this.onItem = nonNull(onItem, "onItem");
            this.onFailure = onFailure;
            this.onCompletion = onCompletion;
            this.onSubscription = nonNull(onSubscription, "onSubscription");
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (subscription.compareAndSet(null, s)) {
                try {
                    // onSubscription cannot be null
                    onSubscription.accept(this);
                } catch (Throwable ex) {
                    s.cancel();
                    onError(ex);
                }
            } else {
                s.cancel();
            }
        }

        @Override
        public void onItem(T item) {
            if (subscription.get() != Subscriptions.CANCELLED) {
                try {
                    // onItem cannot be null.
                    onItem.accept(item);
                } catch (Throwable e) {
                    subscription.getAndSet(Subscriptions.CANCELLED).cancel();
                    onError(e);
                }
            }
        }

        @Override
        public void onFailure(Throwable t) {
            if (subscription.getAndSet(Subscriptions.CANCELLED) != Subscriptions.CANCELLED) {
                if (onFailure != null) {
                    onFailure.accept(t);
                }
            }
        }

        @Override
        public void onCompletion() {
            if (subscription.getAndSet(Subscriptions.CANCELLED) != Subscriptions.CANCELLED) {
                if (onCompletion != null) {
                    onCompletion.run();
                }
            }
        }

        @Override
        public void request(long n) {
            subscription.get().request(n);
        }

        @Override
        public void cancel() {
            Subscription prev = subscription.getAndSet(Subscriptions.CANCELLED);
            if (prev != null && prev != Subscriptions.CANCELLED) {
                prev.cancel();
            }
        }
    }
}
