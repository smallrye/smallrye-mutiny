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
package io.smallrye.mutiny.streams.stages;

import org.junit.Test;

import io.smallrye.mutiny.streams.operators.PublisherStage;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

/**
 * Checks the behavior of {@link FailedPublisherStageFactory}.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FailedPublisherStageFactoryTest extends StageTestBase {

    private final FailedPublisherStageFactory factory = new FailedPublisherStageFactory();

    @Test
    public void createWithError() {
        Exception failure = new Exception("Boom");
        PublisherStage<Object> boom = factory.create(null, () -> failure);
        boom.get().subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasFailedWith(Exception.class, "Boom");
    }

    @Test(expected = NullPointerException.class)
    public void createWithoutError() {
        factory.create(null, () -> null);
    }

    @Test(expected = NullPointerException.class)
    public void createWithoutStage() {
        factory.create(null, null);
    }
}
