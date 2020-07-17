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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniCreationTest {

    @Test
    public void test() {
        // tag::code[]

        // Creation from a known item, or computed at subscription time
        Uni.createFrom().item("some known value");
        Uni.createFrom().item(() -> "some value computed at subscription time");

        // Creation from a completion stage or completable future
        Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> "result"))
                .subscribe().with(
                        item -> System.out.println("Received: " + item),
                        failure -> System.out.println("Failed with " + failure.getMessage()));

        // Creation from a failure
        Uni.createFrom().failure(() -> new Exception("exception created at subscription time"));

        // Creation from an emitter
        Uni.createFrom().emitter(emitter -> {
            // ...
            emitter.complete("some result");
            //...
        });

        // Create from a Reactive Streams Publisher or a Multi
        Uni.createFrom().publisher(Multi.createFrom().ticks().every(Duration.ofMillis(1)))
                .subscribe().with(
                        item -> System.out.println("Received tick " + item),
                        failure -> System.out.println("Failed with " + failure.getMessage()));

        // Defer the creation of the uni until subscription time
        Uni.createFrom().deferred(() -> Uni.createFrom().item("create the uni at subscription time"));

        // end::code[]
    }

    @Test
    public void subscription() {
        // tag::subscription[]
        Uni<String> uni = Uni.createFrom().item("hello");

        // Passing callbacks
        Cancellable cancellable = uni.subscribe().with(
                item -> System.out.println("Got item: " + item),
                failure -> System.out.println("Got a failure " + failure.getMessage()));
        // You can use the returned `cancellation` to cancel the computation.
        cancellable.cancel();

        uni.subscribe().withSubscriber(new UniSubscriber<String>() {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                System.out.println("Got the subscription: " + subscription);
            }

            @Override
            public void onItem(String item) {
                System.out.println("Got the item: " + item);
            }

            @Override
            public void onFailure(Throwable failure) {
                System.out.println("Got the failure: " + failure);
            }
        });
        // end::subscription[]
    }

}
