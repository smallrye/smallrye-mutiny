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
package io.smallrye.mutiny.test;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@SuppressWarnings("SubscriberImplementation")
public class AbstractSubscriber<T> implements Subscriber<T>, Subscription {

    private final long upfrontRequest;

    public AbstractSubscriber() {
        upfrontRequest = 0;
    }

    public AbstractSubscriber(long req) {
        upfrontRequest = req;
    }

    private AtomicReference<Subscription> upstream = new AtomicReference<>();

    @Override
    public void onSubscribe(Subscription s) {
        if (upstream.compareAndSet(null, s)) {
            if (upfrontRequest > 0) {
                s.request(upfrontRequest);
            }
        } else {
            throw new IllegalStateException("We already have a subscription");
        }
    }

    @Override
    public void onNext(T t) {

    }

    @Override
    public void onError(Throwable t) {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public void request(long n) {
        Subscription subscription = upstream.get();
        if (subscription != null) {
            subscription.request(n);
        } else {
            throw new IllegalStateException("No subscription");
        }
    }

    @Override
    public void cancel() {
        Subscription subscription = upstream.get();
        if (subscription != null) {
            subscription.cancel();
        } else {
            throw new IllegalStateException("No subscription");
        }
    }
}
