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
package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.testng.annotations.Test;

import io.reactivex.Flowable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiCreateFromPublisherTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatPublisherCannotBeNull() {
        Multi.createFrom().publisher(null);
    }

    @Test
    public void testWithFailedPublisher() {
        MultiAssertSubscriber<String> subscriber = Multi.createFrom().<String> publisher(
                Flowable.error(new IOException("boom"))).subscribe()
                .withSubscriber(MultiAssertSubscriber.create());
        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testWithEmptyPublisher() {
        MultiAssertSubscriber<String> subscriber = Multi.createFrom().<String> publisher(Flowable.empty()).subscribe()
                .withSubscriber(MultiAssertSubscriber.create());
        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithRegularPublisher() {
        AtomicLong requests = new AtomicLong();
        AtomicInteger count = new AtomicInteger();
        Flowable<Integer> flowable = Flowable.defer(() -> {
            count.incrementAndGet();
            return Flowable.just(1, 2, 3, 4);
        }).doOnRequest(requests::addAndGet);

        Multi<Integer> multi = Multi.createFrom().publisher(flowable);

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create()).assertHasNotReceivedAnyItem()
                .request(2)
                .assertReceived(1, 2)
                .run(() -> assertThat(requests).hasValue(2))
                .request(1)
                .assertReceived(1, 2, 3)
                .request(1)
                .assertReceived(1, 2, 3, 4)
                .run(() -> assertThat(requests).hasValue(4))
                .assertCompletedSuccessfully();

        assertThat(count).hasValue(1);

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create()).assertHasNotReceivedAnyItem()
                .request(2)
                .assertReceived(1, 2)
                .request(1)
                .assertReceived(1, 2, 3)
                .request(1)
                .assertReceived(1, 2, 3, 4)
                .run(() -> assertThat(requests).hasValue(8))
                .assertCompletedSuccessfully();

        assertThat(count).hasValue(2);

    }

    @Test
    public void testThatCancellingTheMultiCancelThePublisher() {
        AtomicBoolean cancellation = new AtomicBoolean();
        Flowable<Integer> flowable = Flowable.just(1, 2, 3, 4).doOnCancel(() -> cancellation.set(true));

        Multi<Integer> multi = Multi.createFrom().publisher(flowable);

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create()).assertHasNotReceivedAnyItem()
                .request(2)
                .assertReceived(1, 2)
                .run(() -> assertThat(cancellation).isFalse())
                .request(1)
                .assertReceived(1, 2, 3)
                .cancel()
                .request(1)
                .assertReceived(1, 2, 3)
                .assertNotTerminated();

        assertThat(cancellation).isTrue();
    }
}
