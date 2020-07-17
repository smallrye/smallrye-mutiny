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

import java.io.IOException;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiDistinctTest {

    @Test
    public void testDistinctWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .transform().byDroppingDuplicates()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDistinct() {
        Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .transform().byDroppingDuplicates()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testDistinctOnAStreamWithoutDuplicates() {
        Multi.createFrom().range(1, 5)
                .transform().byDroppingDuplicates()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testDropRepetitionsWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDropRepetitions() {
        Multi.createFrom().items(1, 2, 3, 4, 4, 2, 2, 4, 1, 1, 2, 4)
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 2, 4, 1, 2, 4);
    }

    @Test
    public void testDropRepetitionsOnAStreamWithoutDuplicates() {
        Multi.createFrom().range(1, 5)
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

}
