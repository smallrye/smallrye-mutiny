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

import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiCreateFromDeferredSupplierTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatTheSupplierCannotBeNull() {
        Multi.createFrom().deferred(null);
    }

    @Test
    public void testWhenTheSupplierProduceNull() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();

        Multi.createFrom().<Integer> deferred(() -> null).subscribe(ts);

        ts
                .assertHasNotCompleted()
                .assertHasNotReceivedAnyItem()
                .assertHasFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testWhenTheSupplierThrowsAnException() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();

        Multi.createFrom().<Integer> deferred(() -> {
            throw new IllegalStateException("boom");
        }).subscribe(ts);

        ts
                .assertHasNotCompleted()
                .assertHasNotReceivedAnyItem()
                .assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithASupplierProducingOne() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create(1);

        Multi.createFrom().deferred(() -> Multi.createFrom().item(1)).subscribe(ts);

        ts.assertCompletedSuccessfully()
                .assertReceived(1)
                .assertHasNotFailed();
    }

    @Test
    public void testThatEachSubscriberHasItsOwn() {
        AtomicInteger count = new AtomicInteger();

        Multi<Integer> multi = Multi.createFrom().deferred(() -> Multi.createFrom().item(count.incrementAndGet()));

        MultiAssertSubscriber<Integer> s1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        MultiAssertSubscriber<Integer> s2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        MultiAssertSubscriber<Integer> s3 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));

        s1.assertReceived(1).assertCompletedSuccessfully();
        s2.assertReceived(2).assertCompletedSuccessfully();
        s3.assertReceived(3).assertCompletedSuccessfully();
    }
}
