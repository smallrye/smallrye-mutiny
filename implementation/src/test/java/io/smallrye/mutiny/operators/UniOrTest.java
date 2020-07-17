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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;

public class UniOrTest {

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

    @AfterTest
    public void shutdown() {
        executor.shutdown();
    }

    @SuppressWarnings("unchecked")
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithNullAsIterable() {
        Uni.combine().any().of((Iterable) null);
    }

    @SuppressWarnings("unchecked")
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithNullAsArray() {
        Uni.combine().any().of((Uni[]) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithItemInIterable() {
        List<Uni<String>> unis = new ArrayList<>();
        unis.add(Uni.createFrom().item("foo"));
        unis.add(null);
        unis.add(Uni.createFrom().item("bar"));
        Uni.combine().any().of(unis);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithItemInArray() {
        Uni.combine().any().of(Uni.createFrom().item("foo"), null, Uni.createFrom().item("bar"));
    }

    @Test
    public void testWithNoCandidate() {
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        Uni.combine().any().<Void> of().subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedSuccessfully().assertItem(null);
    }

    @Test
    public void testWithSingleItemCompletingSuccessfully() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        Uni.combine().any().of(Uni.createFrom().item("foo")).subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedSuccessfully().assertItem("foo");
    }

    @Test
    public void testWithSingleItemCompletingWithAFailure() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        Uni.combine().any().of(Uni.createFrom().<String> failure(new IOException("boom"))).subscribe()
                .withSubscriber(subscriber);
        subscriber.assertCompletedWithFailure().assertFailure(IOException.class, "boom");
    }

    @Test
    public void testWithTwoUnisCompletingImmediately() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        Uni.combine().any().of(Uni.createFrom().item("foo"), Uni.createFrom().item("bar")).subscribe()
                .withSubscriber(subscriber);
        subscriber.assertCompletedSuccessfully().assertItem("foo");
    }

    @Test
    public void testWithTwoUnisCompletingWithAFailure() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        Uni.combine().any().of(Uni.createFrom().failure(new IOException("boom")), Uni.createFrom().item("foo"))
                .subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedWithFailure().assertFailure(IOException.class, "boom");
    }

    @Test
    public void testWithADelayedUni() {
        UniAssertSubscriber<String> subscriber1 = UniAssertSubscriber.create();
        Uni.combine().any().of(Uni.createFrom().item("foo")
                .onItem().delayIt().onExecutor(executor).by(Duration.ofMillis(10)), Uni.createFrom().item("bar"))
                .subscribe().withSubscriber(subscriber1);
        subscriber1.assertCompletedSuccessfully().assertItem("bar");

        UniAssertSubscriber<String> subscriber2 = UniAssertSubscriber.create();
        Uni.combine().any()
                .of(Uni.createFrom().item("foo").onItem().delayIt().onExecutor(executor).by(Duration.ofMillis(10)),
                        Uni.createFrom().item("bar").onItem().delayIt().onExecutor(executor).by(Duration.ofMillis(100)))
                .subscribe().withSubscriber(subscriber2);
        subscriber2.await().assertCompletedSuccessfully().assertItem("foo");
    }

    @Test(timeOut = 1000)
    public void testBlockingWithDelay() {
        Uni<Integer> uni1 = Uni.createFrom().item((Object) null)
                .onItem().delayIt().onExecutor(executor).by(Duration.ofMillis(500))
                .map(x -> 1);
        Uni<Integer> uni2 = Uni.createFrom().item((Object) null)
                .onItem().delayIt().onExecutor(executor).by(Duration.ofMillis(50))
                .map(x -> 2);
        assertThat(Uni.combine().any().of(uni1, uni2).await().indefinitely()).isEqualTo(2);
    }

    @Test(timeOut = 1000)
    public void testCompletingAgainstEmpty() {
        Uni<Integer> uni1 = Uni.createFrom().item((Object) null).map(x -> 1);
        Uni<Integer> uni2 = Uni.createFrom().item((Object) null).onItem().delayIt().onExecutor(executor)
                .by(Duration.ofMillis(50)).map(x -> 2);
        assertThat(Uni.combine().any().of(uni1, uni2).await().indefinitely()).isEqualTo(1);
    }

    @Test(timeOut = 1000)
    public void testCompletingAgainstNever() {
        Uni<Integer> uni1 = Uni.createFrom().nothing().map(x -> 1);
        Uni<Integer> uni2 = Uni.createFrom().item((Object) null).onItem().delayIt().onExecutor(executor)
                .by(Duration.ofMillis(50)).map(x -> 2);
        assertThat(Uni.combine().any().of(uni1, uni2).await().asOptional().indefinitely()).contains(2);
    }

    @Test
    public void testWithThreeImmediateChallengers() {
        Uni<Integer> any = Uni.combine().any()
                .of(Uni.createFrom().item(1), Uni.createFrom().item(2), Uni.createFrom().item(3));

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        any.subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedSuccessfully().assertItem(1);
    }

    @Test
    public void testUniOrWithAnotherUni() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        Uni.combine().any().of(Uni.createFrom().item("foo"), Uni.createFrom().item("bar"))
                .subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedSuccessfully().assertItem("foo");
    }

    @Test
    public void testUniOrWithDelayedUni() {
        Uni<String> first = Uni.createFrom().item("foo").onItem().delayIt().onExecutor(executor)
                .by(Duration.ofMillis(10));
        Uni<String> second = Uni.createFrom().item("bar").onItem().delayIt().onExecutor(executor)
                .by(Duration.ofMillis(1000));
        Uni<String> third = Uni.createFrom().item("baz").onItem().delayIt().onExecutor(executor)
                .by(Duration.ofMillis(10000));

        Uni<String> c1 = Uni.combine().any().of(third, first, second);
        Uni<String> c2 = Uni.combine().any().of(second, third, first);
        Uni<String> c3 = Uni.combine().any().of(first, third, second);

        assertThat(c1.await().indefinitely()).isEqualTo("foo");
        assertThat(c2.await().indefinitely()).isEqualTo("foo");
        assertThat(c3.await().indefinitely()).isEqualTo("foo");
    }

    @Test
    public void testUniOrWithDelayedUniAndDeprecatedApis() {
        Uni<String> first = Uni.createFrom().item("foo").onItem().delayIt().onExecutor(executor)
                .by(Duration.ofMillis(10));
        Uni<String> second = Uni.createFrom().item("bar").onItem().delayIt().onExecutor(executor)
                .by(Duration.ofMillis(1000));
        Uni<String> third = Uni.createFrom().item("baz").onItem().delayIt().onExecutor(executor)
                .by(Duration.ofMillis(10000));

        Uni<String> c1 = third.or().unis(first, second);
        Uni<String> c2 = second.or().unis(third, first);
        Uni<String> c3 = first.or().unis(third, second);

        assertThat(c1.await().indefinitely()).isEqualTo("foo");
        assertThat(c2.await().indefinitely()).isEqualTo("foo");
        assertThat(c3.await().indefinitely()).isEqualTo("foo");
    }

}
