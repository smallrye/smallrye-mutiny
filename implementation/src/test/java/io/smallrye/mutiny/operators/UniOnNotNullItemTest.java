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

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

@SuppressWarnings("ConstantConditions")
public class UniOnNotNullItemTest {

    @Test
    public void testApply() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().apply(String::toUpperCase)
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().apply(String::toUpperCase)
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().apply(String::toUpperCase)
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testTransform() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().transform(String::toUpperCase)
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().transform(String::toUpperCase)
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().transform(String::toUpperCase)
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testInvoke() {
        AtomicBoolean invoked = new AtomicBoolean();
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().invoke(s -> invoked.set(s.equals("hello")))
                .await().indefinitely()).isEqualTo("hello");
        assertThat(invoked).isTrue();

        invoked.set(false);
        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().invoke(s -> invoked.set(true))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");
        assertThat(invoked).isFalse();

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().invoke(s -> invoked.set(true))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");

        assertThat(invoked).isFalse();
    }

    @Test
    public void testInvokeUni() {
        AtomicBoolean invoked = new AtomicBoolean();
        AtomicReference<String> called = new AtomicReference<>();
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().invokeUni(s -> {
                    invoked.set(s.equals("hello"));
                    return Uni.createFrom().item("something").onItem().invoke(called::set);
                })
                .await().indefinitely()).isEqualTo("hello");
        assertThat(invoked).isTrue();
        assertThat(called).hasValue("something");

        invoked.set(false);
        called.set(null);
        assertThat(Uni.createFrom().nullItem()
                .onItem().ifNotNull().invokeUni(s -> {
                    invoked.set(s.equals("hello"));
                    return Uni.createFrom().item("something").onItem().invoke(called::set);
                })
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");
        assertThat(invoked).isFalse();
        assertThat(called).hasValue(null);

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().invokeUni(s -> {
                    invoked.set(s.equals("hello"));
                    return Uni.createFrom().item("something").onItem().invoke(called::set);
                })
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");

        assertThat(invoked).isFalse();
        assertThat(called).hasValue(null);
    }

    @Test
    public void testInvokeUniProducingNull() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> Uni.createFrom().item("hello")
                        .onItem().ifNotNull().invokeUni(s -> null)
                        .await().indefinitely());
    }

    @Test
    public void testInvokeUniProducingFailure() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> Uni.createFrom().item("hello")
                        .onItem().ifNotNull()
                        .invokeUni(s -> Uni.createFrom().failure(new IllegalStateException("boom")))
                        .await().indefinitely())
                .withMessageContaining("boom");
    }

    @Test
    public void testInvokeUniWithCancellationBeforeEmission() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicReference<String> res = new AtomicReference<>();
        Uni<Object> emitter = Uni.createFrom().emitter(e -> e.onTermination(() -> called.set(true)));

        Cancellable cancellable = Uni.createFrom().item("hello")
                .onItem().ifNotNull()
                .invokeUni(s -> emitter)
                .subscribe().with(res::set);

        cancellable.cancel();
        assertThat(res).hasValue(null);
        assertThat(called).isTrue();
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testProduceUniDeprecated() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().produceUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().produceUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().produceUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testProduceUni() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().transformToUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().transformToUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().transformToUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testProduceUniWithEmitterDeprecated() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().produceUni((s, e) -> e.complete(s.toUpperCase()))
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().produceUni((s, e) -> e.complete(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().produceUni((s, e) -> e.complete(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testTransformToUniWithEmitter() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().transformToUni((s, e) -> e.complete(s.toUpperCase()))
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().transformToUni((s, e) -> e.complete(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().transformToUni((s, e) -> e.complete(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testProduceCompletionStageDeprecated() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().produceCompletionStage(x -> CompletableFuture.completedFuture(x.toUpperCase()))
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().produceCompletionStage(x -> CompletableFuture.completedFuture(x.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().produceCompletionStage(x -> CompletableFuture.completedFuture(x.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testTransformToMulti() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().transformToMulti(x -> Multi.createFrom().item(x.toUpperCase()))
                .collectItems().first()
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().transformToMulti(x -> Multi.createFrom().item(x.toUpperCase()))
                .collectItems().first()
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().transformToMulti(x -> Multi.createFrom().item(x.toUpperCase()))
                .collectItems().first()
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testProduceMultiDeprecated() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().produceMulti(x -> Multi.createFrom().item(x.toUpperCase()))
                .collectItems().first()
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().produceMulti(x -> Multi.createFrom().item(x.toUpperCase()))
                .collectItems().first()
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().produceMulti(x -> Multi.createFrom().item(x.toUpperCase()))
                .collectItems().first()
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }
}
