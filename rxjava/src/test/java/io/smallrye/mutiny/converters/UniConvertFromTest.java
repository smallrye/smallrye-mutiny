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
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;

import org.junit.Test;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniRxConverters;

public class UniConvertFromTest {

    @Test
    public void testCreatingFromACompletable() {
        Uni<Void> uni = Uni.createFrom().converter(UniRxConverters.fromCompletable(), Completable.complete());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromACompletableFromVoid() {
        Uni<Void> uni = Uni.createFrom().converter(UniRxConverters.fromCompletable(),
                Completable.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromASingle() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRxConverters.fromSingle(), Single.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromASingleWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRxConverters.fromSingle(), Single.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromAMaybe() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRxConverters.fromMaybe(), Maybe.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyMaybe() {
        Uni<Void> uni = Uni.createFrom().converter(UniRxConverters.fromMaybe(), Maybe.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAMaybeWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRxConverters.fromMaybe(), Maybe.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromAFlowable() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRxConverters.fromFlowable(), Flowable.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAMultiValuedFlowable() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRxConverters.fromFlowable(), Flowable.just(1, 2, 3));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyFlowable() {
        Uni<Void> uni = Uni.createFrom().converter(UniRxConverters.fromFlowable(), Flowable.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAFlowableWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRxConverters.fromFlowable(), Flowable.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromAnObserver() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRxConverters.fromObservable(), Observable.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAMultiValuedObservable() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRxConverters.fromObservable(), Observable.just(1, 2, 3));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyObservable() {
        Uni<Void> uni = Uni.createFrom().converter(UniRxConverters.fromObservable(), Observable.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAnObservableWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRxConverters.fromObservable(),
                Observable.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }
}
