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

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public abstract class MultiOperatorProcessor<I, O> implements MultiSubscriber<I>, Subscription {

    protected final MultiSubscriber<? super O> downstream;
    protected AtomicReference<Subscription> upstream = new AtomicReference<>();
    AtomicBoolean hasDownstreamCancelled = new AtomicBoolean();

    public MultiOperatorProcessor(MultiSubscriber<? super O> downstream) {
        this.downstream = ParameterValidation.nonNull(downstream, "downstream");
    }

    void failAndCancel(Throwable throwable) {
        Subscription subscription = upstream.get();
        if (subscription != null) {
            subscription.cancel();
        }
        onFailure(throwable);
    }

    protected boolean isDone() {
        return upstream.get() == CANCELLED;
    }

    protected boolean isCancelled() {
        return hasDownstreamCancelled.get();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (upstream.compareAndSet(null, subscription)) {
            // Propagate subscription to downstream.
            downstream.onSubscribe(this);
        } else {
            subscription.cancel();
        }
    }

    @Override
    public void onFailure(Throwable throwable) {
        Subscription subscription = upstream.getAndSet(CANCELLED);
        if (subscription != CANCELLED) {
            downstream.onFailure(throwable);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onItem(I item) {
        Subscription subscription = upstream.get();
        if (subscription != CANCELLED) {
            downstream.onItem((O) item);
        }
    }

    @Override
    public void onCompletion() {
        Subscription subscription = upstream.getAndSet(CANCELLED);
        if (subscription != CANCELLED) {
            downstream.onCompletion();
        }
    }

    @Override
    public void request(long numberOfItems) {
        Subscription subscription = upstream.get();
        if (subscription != CANCELLED) {
            if (numberOfItems <= 0) {
                onFailure(new IllegalArgumentException("Invalid number of request, must be greater than 0"));
            }
            subscription.request(numberOfItems);
        }
    }

    @Override
    public void cancel() {
        if (hasDownstreamCancelled.compareAndSet(false, true)) {
            Subscriptions.cancel(upstream);
        }
    }

}
