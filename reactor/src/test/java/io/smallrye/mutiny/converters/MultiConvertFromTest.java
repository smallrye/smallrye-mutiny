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
package io.smallrye.mutiny.converters;

import java.io.IOException;

import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.multi.MultiReactorConverters;
import io.smallrye.mutiny.test.MultiAssertSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MultiConvertFromTest {

    @Test
    public void testCreatingFromAMono() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromMono(), Mono.just(1))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromAnEmptyMono() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromMono(), Mono.<Void> empty())
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromAMonoWithFailure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromMono(), Mono.<Integer> error(new IOException("boom")))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCreatingFromAFlux() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromFlux(), Flux.just(1))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromAMultiValuedFlux() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromFlux(), Flux.just(1, 2, 3))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(3));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(1, 2, 3);
    }

    @Test
    public void testCreatingFromAnEmptyFlux() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromFlux(), Flux.<Void> empty())
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromAFluxWithFailure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromFlux(), Flux.<Integer> error(new IOException("boom")))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }
}
