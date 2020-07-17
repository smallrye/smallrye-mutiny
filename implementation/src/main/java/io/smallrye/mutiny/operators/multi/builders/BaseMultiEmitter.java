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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.MultiSubscriber;

abstract class BaseMultiEmitter<T>
        implements MultiEmitter<T>, Subscription {

    protected final AtomicLong requested = new AtomicLong();
    protected final MultiSubscriber<? super T> downstream;

    private final AtomicReference<Runnable> onTermination;

    private static final Runnable CLEARED = () -> {
    };

    BaseMultiEmitter(MultiSubscriber<? super T> downstream) {
        this.downstream = downstream;
        this.onTermination = new AtomicReference<>();
    }

    @Override
    public long requested() {
        return requested.get();
    }

    @Override
    public void complete() {
        completion();
    }

    protected void completion() {
        if (isCancelled()) {
            return;
        }

        try {
            downstream.onCompletion();
        } finally {
            cleanup();
        }
    }

    @Override
    public boolean isCancelled() {
        return onTermination.get() == CLEARED;
    }

    private void cleanup() {
        Runnable action = onTermination.getAndSet(CLEARED);
        if (action != null && action != CLEARED) {
            action.run();
        }
    }

    @Override
    public final void fail(Throwable failure) {
        failed(failure);
    }

    protected void failed(Throwable e) {
        if (e == null) {
            e = new NullPointerException("onError called with null.");
        }
        if (isCancelled()) {
            return;
        }
        try {
            downstream.onFailure(e);
        } finally {
            cleanup();
        }
    }

    @Override
    public final void cancel() {
        cleanup();
        onUnsubscribed();
    }

    void onUnsubscribed() {
        // default is no-op
    }

    @Override
    public final void request(long n) {
        if (n > 0) {
            Subscriptions.add(requested, n);
            onRequested();
        }
    }

    void onRequested() {
        // default is no-op
    }

    @Override
    public MultiEmitter<T> onTermination(Runnable onTermination) {
        Runnable runnable = this.onTermination.getAndSet(onTermination);
        if (runnable != null) {
            runnable.run();
        }
        return this;
    }

    public MultiEmitter<T> serialize() {
        return new SerializedMultiEmitter<>(this);
    }
}
