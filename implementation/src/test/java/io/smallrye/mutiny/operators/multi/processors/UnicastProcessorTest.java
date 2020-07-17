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
package io.smallrye.mutiny.operators.multi.processors;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.testng.annotations.Test;

import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class UnicastProcessorTest {

    @Test
    public void testTheProcessorCanGetOnlyOneSubscriber() {
        UnicastProcessor<Integer> processor = UnicastProcessor.create();
        processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create());
        MultiAssertSubscriber<Integer> second = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create());

        second.assertHasNotReceivedAnyItem()
                .assertHasFailedWith(IllegalStateException.class, null)
                .assertHasNotCompleted();
    }

    @Test
    public void testWithMultithreadedUpstream() {
        UnicastProcessor<String> processor = UnicastProcessor.create();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            int t = i;
            Runnable produce = () -> {
                for (int j = 0; j < 10000; j++) {
                    processor.onNext(t + "-" + j);
                }
            };
            executor.submit(produce);
        }

        MultiAssertSubscriber<Object> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        processor.subscribe(subscriber);

        await().until(() -> subscriber.items().size() == 5 * 10000);
        executor.shutdownNow();
    }

}
