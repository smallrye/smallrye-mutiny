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
package io.smallrye.mutiny.operators.multi.builders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class CollectionBasedMulti<T> extends AbstractMulti<T> {

    /**
     * The collection, immutable once set.
     */
    private final Collection<T> collection;

    @SafeVarargs
    public CollectionBasedMulti(T... array) {
        this.collection = Arrays.asList(ParameterValidation.doesNotContainNull(array, "array"));
    }

    public CollectionBasedMulti(Collection<T> collection) {
        this.collection = Collections.unmodifiableCollection(
                ParameterValidation.doesNotContainNull(collection, "collection"));
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        ParameterValidation.nonNullNpe(actual, "subscriber");
        if (collection.isEmpty()) {
            Subscriptions.complete(actual);
            return;
        }
        actual.onSubscribe(new CollectionSubscription<>(actual, collection));
    }

    public static final class CollectionSubscription<T> implements Subscription {

        private final MultiSubscriber<? super T> downstream;
        private final List<T> collection; // Immutable
        private int index;

        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicLong requested = new AtomicLong();

        public CollectionSubscription(MultiSubscriber<? super T> downstream, Collection<T> collection) {
            this.downstream = downstream;
            this.collection = new ArrayList<>(collection);
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                if (Subscriptions.add(requested, n) == 0) {
                    if (n == Long.MAX_VALUE) {
                        produceWithoutBackPressure();
                    } else {
                        followRequests(n);
                    }
                }
            } else {
                downstream.onFailure(Subscriptions.getInvalidRequestException());
            }
        }

        void followRequests(long n) {
            final List<T> items = collection;
            final int size = items.size();

            int current = index;
            int emitted = 0;

            for (;;) {
                if (cancelled.get()) {
                    return;
                }

                while (current != size && emitted != n) {
                    downstream.onItem(items.get(current));

                    if (cancelled.get()) {
                        return;
                    }

                    current++;
                    emitted++;
                }

                if (current == size) {
                    downstream.onCompletion();
                    return;
                }

                n = requested.get();

                if (n == emitted) {
                    index = current;
                    n = requested.addAndGet(-emitted);
                    if (n == 0) {
                        return;
                    }
                    emitted = 0;
                }
            }
        }

        void produceWithoutBackPressure() {
            for (T item : collection) {
                if (cancelled.get()) {
                    return;
                }
                downstream.onItem(item);
            }

            if (cancelled.get()) {
                return;
            }
            downstream.onCompletion();
        }

        @Override
        public void cancel() {
            cancelled.set(true);
        }
    }

}
