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

import org.junit.Test;
import org.reactivestreams.Subscription;

public class WrappedSubscriptionTest {

    @Test
    public void testWrappedSubscription() {
        Subscription subscription = new Subscription() {
            @Override
            public void request(long n) {

            }

            @Override
            public void cancel() {

            }
        };

        WrappedSubscription wrapped = new WrappedSubscription(subscription, null);
        assertThat(wrapped).isNotNull();
        wrapped.request(10);
        wrapped.cancel();
    }

    @Test
    public void testWrappedSubscriptionWithCompletionCallback() {
        Subscription subscription = new Subscription() {
            @Override
            public void request(long n) {

            }

            @Override
            public void cancel() {

            }
        };
        AtomicBoolean called = new AtomicBoolean();
        WrappedSubscription wrapped = new WrappedSubscription(subscription, () -> called.set(true));
        assertThat(wrapped).isNotNull();
        wrapped.request(10);
        wrapped.cancel();
        assertThat(called).isTrue();
    }

}
