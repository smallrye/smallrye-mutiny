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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subscribers.TestSubscriber;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.ToSingle;
import io.smallrye.mutiny.converters.uni.ToSingleWithDefault;
import io.smallrye.mutiny.converters.uni.UniRxConverters;

public class UniConvertToTest {

    @Test
    public void testCreatingACompletable() {
        Completable completable = Uni.createFrom().item(1).convert().with(UniRxConverters.toCompletable());
        assertThat(completable).isNotNull();
        completable.test().assertComplete();
    }

    @Test
    public void testThatSubscriptionOnCompletableProducesTheValue() {
        AtomicBoolean called = new AtomicBoolean();
        Completable completable = Uni.createFrom().deferred(() -> {
            called.set(true);
            return Uni.createFrom().item(2);
        }).convert().with(UniRxConverters.toCompletable());

        assertThat(completable).isNotNull();
        assertThat(called).isFalse();
        completable.test().assertComplete();
        assertThat(called).isTrue();
    }

    @Test
    public void testCreatingACompletableFromVoid() {
        Completable completable = Uni.createFrom().item((Object) null).convert().with(UniRxConverters.toCompletable());
        assertThat(completable).isNotNull();
        completable.test().assertComplete();
    }

    @Test
    public void testCreatingACompletableWithFailure() {
        Completable completable = Uni.createFrom().failure(new IOException("boom")).convert()
                .with(UniRxConverters.toCompletable());
        assertThat(completable).isNotNull();
        completable.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @Test
    public void testCreatingASingle() {
        Single<Optional<Integer>> single = Uni.createFrom().item(1).convert().with(UniRxConverters.toSingle());
        assertThat(single).isNotNull();
        single.test()
                .assertValue(Optional.of(1))
                .assertComplete();
    }

    @Test
    public void testCreatingASingleByConverter() {
        Single<Optional<Integer>> single = Uni.createFrom().item(1).convert().with(UniRxConverters.toSingle());
        assertThat(single).isNotNull();
        single.test()
                .assertValue(Optional.of(1))
                .assertComplete();
    }

    @Test
    public void testCreatingASingleWithDefaultFromNull() {
        Single<Integer> single = Uni.createFrom().item((Integer) null).convert().with(new ToSingleWithDefault<>(22));
        assertThat(single).isNotNull();
        single.test()
                .assertValue(22)
                .assertComplete();
    }

    @Test
    public void testCreatingASingleWithDefaultWithValue() {
        Single<Integer> single = Uni.createFrom().item(5).convert().with(new ToSingleWithDefault<>(22));
        assertThat(single).isNotNull();
        single.test()
                .assertValue(5)
                .assertComplete();
    }

    @Test
    public void testCreatingASingleWithDefaultFromNullAlternative() {
        Single<Integer> single = Uni.createFrom().item((Integer) null).convert().with(ToSingle.withDefault(22));
        assertThat(single).isNotNull();
        single.test()
                .assertValue(22)
                .assertComplete();
    }

    @Test
    public void testCreatingASingleWithDefaultFromNullByConverter() {
        Single<Integer> single = Uni.createFrom().item((Integer) null).convert().with(new ToSingleWithDefault<>(22));
        assertThat(single).isNotNull();
        single.test()
                .assertValue(22)
                .assertComplete();
    }

    @Test
    public void testFailureToCreateSingleFromNull() {
        Single<Integer> single = Uni.createFrom().item((Integer) null).convert()
                .with(UniRxConverters.toSingle().failOnNull());
        assertThat(single).isNotNull();
        single.test().assertError(e -> {
            assertThat(e).isInstanceOf(NoSuchElementException.class);
            return true;
        });
    }

    @Test
    public void testCreatingASingleFromNull() {
        Single<Optional<Integer>> single = Uni.createFrom().item((Integer) null).convert()
                .with(UniRxConverters.toSingle());
        assertThat(single).isNotNull();
        single
                .test()
                .assertValue(Optional.empty())
                .assertComplete();
    }

    @Test
    public void testCreatingASingleWithFailure() {
        Single<Optional<Integer>> single = Uni.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(UniRxConverters.toSingle());
        assertThat(single).isNotNull();
        single.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @Test
    public void testCreatingAMaybe() {
        Maybe<Integer> maybe = Uni.createFrom().item(1).convert().with(UniRxConverters.toMaybe());
        assertThat(maybe).isNotNull();
        maybe.test()
                .assertValue(1)
                .assertComplete();
    }

    @Test
    public void testCreatingAMaybeFromNull() {
        Maybe<Integer> maybe = Uni.createFrom().item((Integer) null).convert().with(UniRxConverters.toMaybe());
        assertThat(maybe).isNotNull();
        maybe
                .test()
                .assertComplete()
                .assertNoValues();
    }

    @Test
    public void testCreatingAMaybeWithFailure() {
        Maybe<Integer> maybe = Uni.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(UniRxConverters.toMaybe());
        assertThat(maybe).isNotNull();
        maybe.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @Test
    public void testCreatingAnObservable() {
        Observable<Integer> observable = Uni.createFrom().item(1).convert().with(UniRxConverters.toObservable());
        assertThat(observable).isNotNull();
        observable.test()
                .assertValue(1)
                .assertComplete();
    }

    @Test
    public void testCreatingAnObservableFromNull() {
        Observable<Integer> observable = Uni.createFrom().item((Integer) null).convert()
                .with(UniRxConverters.toObservable());
        assertThat(observable).isNotNull();
        observable
                .test()
                .assertComplete()
                .assertNoValues();
    }

    @Test
    public void testCreatingAnObservableWithFailure() {
        Observable<Integer> observable = Uni.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(UniRxConverters.toObservable());
        assertThat(observable).isNotNull();
        observable.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @Test
    public void testCreatingAFlowable() {
        Flowable<Integer> flowable = Uni.createFrom().item(1).convert().with(UniRxConverters.toFlowable());
        assertThat(flowable).isNotNull();
        flowable.test()
                .assertValue(1)
                .assertComplete();
    }

    @Test
    public void testCreatingAFlowableWithRequest() {
        AtomicBoolean called = new AtomicBoolean();
        Flowable<Integer> flowable = Uni.createFrom().deferred(() -> {
            called.set(true);
            return Uni.createFrom().item(1);
        }).convert().with(UniRxConverters.toFlowable());
        assertThat(flowable).isNotNull();
        TestSubscriber<Integer> test = flowable.test(0);
        assertThat(called).isFalse();
        test.assertNoValues().assertSubscribed();
        test.request(2);
        test.assertValue(1).assertComplete();
        assertThat(called).isTrue();
    }

    @Test
    public void testCreatingAFlowableFromNull() {
        Flowable<Integer> flowable = Uni.createFrom().item((Integer) null).convert().with(UniRxConverters.toFlowable());
        assertThat(flowable).isNotNull();
        flowable
                .test()
                .assertComplete()
                .assertNoValues();
    }

    @Test
    public void testCreatingAFlowableWithFailure() {
        Flowable<Integer> flowable = Uni.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(UniRxConverters.toFlowable());
        assertThat(flowable).isNotNull();
        flowable.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }
}
