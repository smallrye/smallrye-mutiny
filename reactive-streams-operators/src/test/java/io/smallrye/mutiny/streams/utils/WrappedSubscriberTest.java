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
package io.smallrye.mutiny.streams.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class WrappedSubscriberTest {

    @Test
    public void checkThatOnSubscribeCanOnlyBeCallOnce() {
        AtomicReference<Subscription> subscriptionReference = new AtomicReference<>();
        WrappedSubscriber<Integer> subscriber = new WrappedSubscriber<>(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscriptionReference.set(s);
            }

            @Override
            public void onNext(Integer integer) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });

        AtomicBoolean cancellationReference1 = new AtomicBoolean();
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {

            }

            @Override
            public void cancel() {
                cancellationReference1.set(true);
            }
        });

        assertThat(cancellationReference1.get()).isFalse();
        assertThat(subscriptionReference.get()).isNotNull();

        AtomicBoolean cancellationReference2 = new AtomicBoolean();
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {

            }

            @Override
            public void cancel() {
                cancellationReference2.set(true);
            }
        });

        assertThat(cancellationReference1.get()).isFalse();
        assertThat(cancellationReference2.get()).isTrue();
        assertThat(subscriptionReference.get()).isNotNull();

    }

}
