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
package snippets;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;

public class PollableSourceTest {

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Test
    public void test() { // NOSONAR
        // tag::code[]
        PollableDataSource source = new PollableDataSource();
        // First creates a uni that emit the polled item. Because `poll` blocks, let's use a specific executor
        Uni<String> pollItemFromSource = Uni.createFrom().item(source::poll).runSubscriptionOn(executor);
        // To get the stream of items, just repeat the uni indefinitely
        Multi<String> stream = pollItemFromSource.repeat().indefinitely();

        Cancellable cancellable = stream.subscribe().with(item -> System.out.println("Polled item: " + item));
        // end::code[]
        await().until(() -> source.counter.get() >= 4);
        // tag::code[]
        // ... later ..
        // when you don't want the items anymore, cancel the subscription and close the source if needed.
        cancellable.cancel();
        source.close();
        // end::code[]
    }

    @SuppressWarnings("Convert2MethodRef")
    @Test
    public void test2() { // NOSONAR
        // tag::code2[]
        PollableDataSource source = new PollableDataSource();
        Multi<String> stream = Multi.createBy().repeating()
                    .supplier(source::poll)
                    .until(s -> s == null)
                .runSubscriptionOn(executor);

        stream.subscribe().with(item -> System.out.println("Polled item: " + item));
        // end::code2[]
        await().until(() -> source.counter.get() >= 5);
    }

    private static class PollableDataSource {

        private final AtomicInteger counter = new AtomicInteger();

        String poll() {
            block();
            if (counter.get() == 5) {
                return null;
            }
            return Integer.toString(counter.getAndIncrement());
        }

        private void block() {
            try {
                Thread.sleep(100);  // NOSONAR
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public void close() {
            // do nothing.
        }
    }

}
