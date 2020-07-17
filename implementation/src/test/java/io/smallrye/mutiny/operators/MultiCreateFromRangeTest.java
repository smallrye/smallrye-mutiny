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

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiCreateFromRangeTest {

    @Test
    public void testARangeFrom0to10() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();
        Multi.createFrom().range(1, 10).subscribe().withSubscriber(ts)
                .request(3)
                .assertReceived(1, 2, 3)
                .assertHasNotCompleted()
                .request(10)
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testARangeFrom0to10WithFullConsumptionAtSubscription() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create(9);
        Multi.createFrom().range(1, 10).subscribe().withSubscriber(ts)
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .assertCompletedSuccessfully()
                .request(3)
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .assertCompletedSuccessfully();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatEndBoundaryCannotBeLessThanStart() {
        Multi.createFrom().range(1, -1);
    }

}
