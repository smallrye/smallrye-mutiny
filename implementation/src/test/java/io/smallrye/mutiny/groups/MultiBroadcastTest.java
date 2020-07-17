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
package io.smallrye.mutiny.groups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.MultiEmitterProcessor;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiBroadcastTest {

    @Test
    public void testPublishToAllSubscribers() {
        MultiEmitterProcessor<Integer> processor = MultiEmitterProcessor.create();

        Multi<Integer> multi = processor.toMulti().broadcast().toAllSubscribers();

        MultiAssertSubscriber<Integer> s1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(10));
        s1.assertHasNotReceivedAnyItem().assertNotTerminated();

        processor.emit(1).emit(2).emit(3);
        s1.assertReceived(1, 2, 3).assertNotTerminated();

        MultiAssertSubscriber<Integer> s2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        s2.assertHasNotReceivedAnyItem().assertNotTerminated();

        processor.emit(4).emit(5);
        // 5 is not received, because we use the lower number of requests.
        s1.assertReceived(1, 2, 3, 4).assertNotTerminated();
        s2.assertReceived(4).assertNotTerminated();

        processor.complete();

        s1.assertNotTerminated();
        s2.assertNotTerminated();

        // No one completed, because the are still ongoing items, and not enough requests
        s2.request(10);

        s1.assertCompletedSuccessfully();
        s2.assertCompletedSuccessfully();
    }

    @Test
    public void testPublishToAllSubscribersWithFailure() {
        MultiEmitterProcessor<Integer> processor = MultiEmitterProcessor.create();

        Multi<Integer> multi = processor.toMulti().broadcast().toAllSubscribers();

        MultiAssertSubscriber<Integer> s1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(10));
        s1.assertHasNotReceivedAnyItem().assertNotTerminated();

        processor.emit(1).emit(2).emit(3);
        s1.assertReceived(1, 2, 3).assertNotTerminated();

        MultiAssertSubscriber<Integer> s2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        s2.assertHasNotReceivedAnyItem().assertNotTerminated();

        processor.emit(4).emit(5);
        // 5 is not received, because we use the lower number of requests.
        s1.assertReceived(1, 2, 3, 4).assertNotTerminated();
        s2.assertReceived(4).assertNotTerminated();

        processor.fail(new IOException("boom"));

        s1.assertHasFailedWith(IOException.class, "boom");
        s2.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testPublishAfterDepartureOfASubscriber() {
        MultiEmitterProcessor<Integer> processor = MultiEmitterProcessor.create();

        Multi<Integer> multi = processor.toMulti().broadcast().toAllSubscribers();

        MultiAssertSubscriber<Integer> s1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(10));
        s1.assertHasNotReceivedAnyItem().assertNotTerminated();

        processor.emit(1).emit(2).emit(3);
        s1.assertReceived(1, 2, 3).assertNotTerminated();

        MultiAssertSubscriber<Integer> s2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        s2.assertHasNotReceivedAnyItem().assertNotTerminated();

        processor.emit(4).emit(5);
        // 5 is not received, because we use the lower number of requests.
        s1.assertReceived(1, 2, 3, 4).assertNotTerminated();
        s2.assertReceived(4).assertNotTerminated();

        processor.complete();

        s1.assertNotTerminated();
        s2.assertNotTerminated();

        s2.cancel();

        s1.assertReceived(1, 2, 3, 4, 5);
        s1.assertCompletedSuccessfully();
    }

    @Test
    public void testNoCancellationEvenWithoutSubscribers() {
        AtomicBoolean cancelled = new AtomicBoolean();
        MultiEmitterProcessor<Integer> processor = MultiEmitterProcessor.create();
        processor.onTermination(() -> cancelled.set(true));

        Multi<Integer> multi = processor.toMulti().broadcast().toAllSubscribers();

        MultiAssertSubscriber<Integer> s1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(10));
        s1.assertHasNotReceivedAnyItem().assertNotTerminated();

        processor.emit(1).emit(2).emit(3);
        s1.assertReceived(1, 2, 3).assertNotTerminated();

        MultiAssertSubscriber<Integer> s2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        s2.assertHasNotReceivedAnyItem().assertNotTerminated();

        processor.emit(4).emit(5);
        s1.assertReceived(1, 2, 3, 4).assertNotTerminated();
        s2.assertReceived(4).assertNotTerminated();

        s2.cancel();
        assertThat(cancelled).isFalse();
        s1.cancel();
        assertThat(cancelled).isFalse();
        assertThat(processor.isCancelled()).isFalse();
        MultiAssertSubscriber<Integer> s3 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(10));
        processor.emit(23);
        processor.complete();
        s3.assertReceived(23).assertCompletedSuccessfully();
    }

    @Test
    public void testSubscriptionAfterUpstreamCompletion() {
        MultiEmitterProcessor<Integer> processor = MultiEmitterProcessor.create();
        processor.emit(1).emit(2).emit(3);
        processor.complete();

        processor.toMulti().broadcast().toAllSubscribers().subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertReceived(1, 2, 3)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testPublishAtLeast() {
        MultiAssertSubscriber<Integer> s1 = MultiAssertSubscriber.create(10);
        MultiAssertSubscriber<Integer> s2 = MultiAssertSubscriber.create(10);

        Multi<Integer> multi = Multi.createFrom().range(1, 5).broadcast().toAtLeast(2);

        multi.subscribe(s1);

        s1.assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        multi.subscribe(s2);

        s1.assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
        s2.assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testCancellationAfterLastDeparture() {
        AtomicBoolean cancelled = new AtomicBoolean();
        MultiEmitterProcessor<Integer> processor = MultiEmitterProcessor.create();
        processor.onTermination(() -> cancelled.set(true));

        Multi<Integer> multi = processor.toMulti().broadcast().withCancellationAfterLastSubscriberDeparture()
                .toAllSubscribers();

        MultiAssertSubscriber<Integer> s1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(10));
        s1.assertHasNotReceivedAnyItem().assertNotTerminated();

        processor.emit(1).emit(2).emit(3);
        s1.assertReceived(1, 2, 3).assertNotTerminated();

        MultiAssertSubscriber<Integer> s2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        s2.assertHasNotReceivedAnyItem().assertNotTerminated();

        processor.emit(4).emit(5);
        s1.assertReceived(1, 2, 3, 4).assertNotTerminated();
        s2.assertReceived(4).assertNotTerminated();

        s2.cancel();
        assertThat(cancelled).isFalse();
        assertThat(processor.isCancelled()).isFalse();
        s1.cancel();
        assertThat(cancelled).isTrue();
        assertThat(processor.isCancelled()).isTrue();
    }

    @Test
    public void testCancellationAfterLastDepartureWithDuration() {
        AtomicBoolean cancelled = new AtomicBoolean();
        MultiEmitterProcessor<Integer> processor = MultiEmitterProcessor.create();
        processor.onTermination(() -> cancelled.set(true));

        Multi<Integer> multi = processor.toMulti().broadcast()
                .withCancellationAfterLastSubscriberDeparture(Duration.ofSeconds(1))
                .toAllSubscribers();

        MultiAssertSubscriber<Integer> s1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(10));
        s1.assertHasNotReceivedAnyItem().assertNotTerminated();

        processor.emit(1).emit(2).emit(3);
        s1.assertReceived(1, 2, 3).assertNotTerminated();

        MultiAssertSubscriber<Integer> s2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        s2.assertHasNotReceivedAnyItem().assertNotTerminated();

        processor.emit(4).emit(5);
        s1.assertReceived(1, 2, 3, 4).assertNotTerminated();
        s2.assertReceived(4).assertNotTerminated();

        s2.cancel();
        assertThat(cancelled).isFalse();
        s1.cancel();
        assertThat(cancelled).isFalse();
        await().until(cancelled::get);
    }

    @Test
    public void testCancellationWithAtLeastAfterLastDeparture() {
        AtomicBoolean cancelled = new AtomicBoolean();
        MultiEmitterProcessor<Integer> processor = MultiEmitterProcessor.create();
        processor.onTermination(() -> cancelled.set(true));

        Multi<Integer> multi = processor.toMulti().broadcast().withCancellationAfterLastSubscriberDeparture()
                .toAtLeast(2);

        MultiAssertSubscriber<Integer> s1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(10));
        s1.assertHasNotReceivedAnyItem().assertNotTerminated();

        processor.emit(1).emit(2).emit(3);

        s1.assertHasNotReceivedAnyItem().assertNotTerminated();

        MultiAssertSubscriber<Integer> s2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(10));

        s1.assertReceived(1, 2, 3).assertNotTerminated();
        s2.assertReceived(1, 2, 3).assertNotTerminated();

        s2.cancel();
        assertThat(cancelled).isFalse();
        s1.cancel();
        assertThat(cancelled).isTrue();
    }

    @Test
    public void testCancellationWithAtLeastAfterLastDepartureAndDelay() {
        AtomicBoolean cancelled = new AtomicBoolean();
        MultiEmitterProcessor<Integer> processor = MultiEmitterProcessor.create();
        processor.onTermination(() -> cancelled.set(true));

        Multi<Integer> multi = processor.toMulti().broadcast()
                .withCancellationAfterLastSubscriberDeparture(Duration.ofSeconds(1))
                .toAtLeast(2);

        MultiAssertSubscriber<Integer> s1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(10));
        s1.assertHasNotReceivedAnyItem().assertNotTerminated();

        processor.emit(1).emit(2).emit(3);

        s1.assertHasNotReceivedAnyItem().assertNotTerminated();

        MultiAssertSubscriber<Integer> s2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(10));

        s1.assertReceived(1, 2, 3).assertNotTerminated();
        s2.assertReceived(1, 2, 3).assertNotTerminated();

        s2.cancel();
        assertThat(cancelled).isFalse();
        s1.cancel();
        assertThat(cancelled).isFalse();
        await().until(cancelled::get);
    }
}
