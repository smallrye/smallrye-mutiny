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
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.junit.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

public class RunSubscriptionOnTest {

    @Test
    public void testRunSubscriptionOn() {
        Executor executor = Infrastructure.getDefaultExecutor();
        AtomicBoolean completed = new AtomicBoolean();
        //tag::runSubscriptionOn[]
        Multi.createFrom().items(() -> {
            // called on a thread from the executor
            return retrieveItemsFromSource();
        })
                .onItem().transform(this::applySomeOperation)
                .runSubscriptionOn(executor)
                .subscribe().with(
                item -> System.out.println("Item: " + item),
                Throwable::printStackTrace,
                () -> completed.set(true)
        );
        //end::runSubscriptionOn[]

        await().untilAtomic(completed, is(true));
    }

    @Test
    public void testEmitOn() {
        Executor executor = Infrastructure.getDefaultExecutor();
        AtomicBoolean completed = new AtomicBoolean();
        //tag::emitOn[]
        Multi.createFrom().items(this::retrieveItemsFromSource)
                .emitOn(executor)
                .onItem().transform(this::applySomeOperation)
                .subscribe().with(
                item -> System.out.println("Item: " + item),
                Throwable::printStackTrace,
                () -> completed.set(true)
        );
        //end::emitOn[]

        await().untilAtomic(completed, is(true));
    }

    public String applySomeOperation(String s) {
        return s.toUpperCase();
    }

    public Stream<String> retrieveItemsFromSource() {
        return Stream.of("a", "b", "c");
    }
}
