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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.reactivestreams.Subscription;

public class AbstractSubscriberTest {

    @Test
    public void testOnNext() {
        List<String> items = new ArrayList<>();
        AbstractSubscriber<String> subscriber = new AbstractSubscriber<String>() {
            @Override
            public void onNext(String o) {
                items.add(o);
            }
        };

        subscriber.onNext("a");
        subscriber.onNext("b");
        subscriber.onComplete();
        assertThat(items).containsExactly("a", "b");
    }

    @Test
    public void testOnError() {
        AtomicBoolean called = new AtomicBoolean();
        AbstractSubscriber<String> subscriber = new AbstractSubscriber<String>() {
            @Override
            public void onError(Throwable t) {
                called.set(true);
            }
        };

        subscriber.onNext("a");
        subscriber.onNext("b");
        subscriber.onError(new Exception("boom"));
        assertThat(called).isTrue();
    }

    @Test
    public void testOnComplete() {
        AtomicBoolean called = new AtomicBoolean();
        AbstractSubscriber<String> subscriber = new AbstractSubscriber<String>() {
            @Override
            public void onComplete() {
                called.set(true);
            }
        };

        subscriber.onNext("a");
        subscriber.onNext("b");
        subscriber.onComplete();
        assertThat(called).isTrue();
    }

    @Test
    public void testSubscription() {
        Subscription subscription = mock(Subscription.class);
        AbstractSubscriber<String> subscriber = new AbstractSubscriber<>();

        subscriber.onSubscribe(subscription);
        subscriber.request(10);
        verify(subscription, times(1)).request(10);
    }

    @Test
    public void testSubscriptionUpstream() {
        Subscription subscription = mock(Subscription.class);
        AbstractSubscriber<String> subscriber = new AbstractSubscriber<>(2);

        subscriber.onSubscribe(subscription);
        verify(subscription, times(1)).request(2);
        subscriber.request(10);
        verify(subscription, times(1)).request(10);
        subscriber.cancel();
        verify(subscription, times(1)).cancel();
    }

    @Test
    public void testRequestWithoutSubscription() {
        AbstractSubscriber<String> subscriber = new AbstractSubscriber<>(2);
        assertThatThrownBy(() -> subscriber.request(2)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(subscriber::cancel).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testOnSubscribedCalledTwice() {
        Subscription subscription = mock(Subscription.class);
        AbstractSubscriber<String> subscriber = new AbstractSubscriber<>(2);
        subscriber.onSubscribe(subscription);
        assertThatThrownBy(() -> subscriber.onSubscribe(subscription)).isInstanceOf(IllegalStateException.class);
    }

}
