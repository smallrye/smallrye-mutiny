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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.helpers.Subscriptions;

/**
 * Wraps another Subscriber and ensures all onXXX methods conform the protocol
 * (except the requirement for serialized access).
 *
 * @param <T> the value type
 */
@SuppressWarnings("SubscriberImplementation")
public final class SafeSubscriber<T> implements Subscriber<T>, Subscription {
    /**
     * The actual Subscriber.
     */
    private final Subscriber<? super T> downstream;
    /**
     * The subscription.
     */
    private final AtomicReference<Subscription> upstream = new AtomicReference<>();
    /**
     * Indicates a terminal state.
     */
    private boolean done;

    /**
     * Constructs a SafeSubscriber by wrapping the given actual Subscriber.
     *
     * @param downstream the actual Subscriber to wrap, not null (not validated)
     */
    public SafeSubscriber(Subscriber<? super T> downstream) {
        this.downstream = downstream;
    }

    /**
     * For testing purpose only.
     * 
     * @return whether the subscriber is in a terminal state.
     */
    boolean isDone() {
        return done;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (upstream.compareAndSet(null, s)) {
            try {
                downstream.onSubscribe(this);
            } catch (Throwable e) {
                done = true;
                // can't call onError because the actual's state may be corrupt at this point
                try {
                    s.cancel();
                } catch (Throwable e1) {
                    // ignore it, nothing we can do.
                }
            }
        } else {
            s.cancel();
        }
    }

    @Override
    public void onNext(T t) {
        if (done) {
            return;
        }
        if (upstream.get() == null) {
            onNextNoSubscription();
            return;
        }

        if (t == null) {
            NullPointerException ex = new NullPointerException("onNext called with null.");
            cancelAndDispatch(ex);
            throw ex;
        }

        try {
            downstream.onNext(t);
        } catch (Throwable e) {
            cancelAndDispatch(e);
        }
    }

    private void cancelAndDispatch(Throwable ex) {
        try {
            upstream.get().cancel();
        } catch (Throwable e1) {
            onError(new CompositeException(ex, e1));
            return;
        }
        onError(ex);
    }

    private void onNextNoSubscription() {
        done = true;
        manageViolationProtocol();
    }

    @Override
    public void onError(Throwable t) {
        Objects.requireNonNull(t);
        if (done) {
            return;
        }
        done = true;

        if (upstream.get() == null) {
            Throwable npe = new NullPointerException("Subscription not set!");

            try {
                downstream.onSubscribe(Subscriptions.empty());
            } catch (Throwable e) {
                // can't call onError because the actual's state may be corrupt at this point
                return;
            }
            try {
                downstream.onError(new CompositeException(t, npe));
            } catch (Throwable e) {
                // nothing we can do.
            }
            return;
        }

        try {
            downstream.onError(t);
        } catch (Throwable ex) {
            // nothing we can do.
        }
    }

    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        done = true;

        if (upstream.get() == null) {
            onCompleteNoSubscription();
            return;
        }

        try {
            downstream.onComplete();
        } catch (Throwable e) {
            // nothing we can do.
        }
    }

    private void onCompleteNoSubscription() {

        manageViolationProtocol();
    }

    private void manageViolationProtocol() {
        Throwable ex = new NullPointerException("Subscription not set!");

        try {
            downstream.onSubscribe(Subscriptions.empty());
        } catch (Throwable e) {
            // can't call onError because the actual's state may be corrupt at this point
            return;
        }
        try {
            downstream.onError(ex);
        } catch (Throwable e) {
            // nothing we can do.
        }
    }

    @Override
    public void request(long n) {
        try {
            upstream.get().request(n);
        } catch (Throwable e) {
            try {
                upstream.get().cancel();
            } catch (Throwable ignored) {
                // nothing we can do.
            }
        }
    }

    @Override
    public void cancel() {
        try {
            upstream.get().cancel();
        } catch (Throwable ignored) {
            // nothing we can do.
        }
    }
}
