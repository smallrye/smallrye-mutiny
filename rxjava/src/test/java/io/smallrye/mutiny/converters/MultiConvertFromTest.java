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

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.multi.MultiRxConverters;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiConvertFromTest {

    @Test
    public void testCreatingFromACompletable() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromCompletable(), Completable.complete())
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromACompletableFromVoid() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromCompletable(), Completable.error(new IOException("boom")))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCreatingFromASingle() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromSingle(), Single.just(1))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromASingleWithFailure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromSingle(), Single.<Integer> error(new IOException("boom")))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCreatingFromAMaybe() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromMaybe(), Maybe.just(1))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromAnEmptyMaybe() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromMaybe(), Maybe.<Void> empty())
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromAMaybeWithFailure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromMaybe(), Maybe.<Integer> error(new IOException("boom")))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCreatingFromAFlowable() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromFlowable(), Flowable.just(1))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromAMultiValuedFlowable() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromFlowable(), Flowable.just(1, 2, 3))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(3));

        subscriber.assertCompletedSuccessfully().assertReceived(1, 2, 3);
    }

    @Test
    public void testCreatingFromAnEmptyFlowable() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromFlowable(), Flowable.<Void> empty())
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromAFlowableWithFailure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromFlowable(), Flowable.<Integer> error(new IOException("boom")))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCreatingFromAnObserver() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromObservable(), Observable.just(1))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromAMultiValuedObservable() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromObservable(), Observable.just(1, 2, 3))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(3));

        subscriber.assertCompletedSuccessfully().assertReceived(1, 2, 3);
    }

    @Test
    public void testCreatingFromAnEmptyObservable() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromObservable(), Observable.<Void> empty())
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromAnObservableWithFailure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromObservable(), Observable.<Integer> error(new IOException("boom")))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }
}
