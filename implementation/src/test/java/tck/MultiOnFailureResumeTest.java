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
package tck;

import static org.testng.Assert.assertEquals;
import static tck.Await.await;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class MultiOnFailureResumeTest {

    @Test
    public void onErrorResumeShouldCatchErrorFromSource() {
        AtomicReference<Throwable> exception = new AtomicReference<>();
        Uni<List<String>> uni = Multi.createFrom().<String> failure(
                () -> new QuietRuntimeException("failed"))
                .onFailure().recoverWithItem(err -> {
                    exception.set(err);
                    return "foo";
                })
                .collectItems().asList();
        assertEquals(await(uni.subscribeAsCompletionStage()), Collections.singletonList("foo"));
        assertEquals(exception.get().getMessage(), "failed");
    }

    @Test
    public void onErrorResumeWithShouldCatchErrorFromSource() {
        AtomicReference<Throwable> exception = new AtomicReference<>();
        assertEquals(await(Multi.createFrom().<String> failure(new QuietRuntimeException("failed"))
                .onFailure().recoverWithMulti(err -> {
                    exception.set(err);
                    return Multi.createFrom().items("foo", "bar");
                })
                .collectItems().asList()
                .subscribeAsCompletionStage()), Arrays.asList("foo", "bar"));
        assertEquals(exception.get().getMessage(), "failed");
    }

    @Test
    public void onErrorResumeShouldCatchErrorFromStage() {
        AtomicReference<Throwable> exception = new AtomicReference<>();
        assertEquals(await(Multi.createFrom().items("a", "b", "c")
                .map(word -> {
                    if (word.equals("b")) {
                        throw new QuietRuntimeException("failed");
                    }
                    return word.toUpperCase();
                })
                .onFailure().recoverWithItem(err -> {
                    exception.set(err);
                    return "foo";
                })
                .collectItems().asList()
                .subscribeAsCompletionStage()), Arrays.asList("A", "foo"));
        assertEquals(exception.get().getMessage(), "failed");
    }

    @Test
    public void onErrorResumeWithShouldCatchErrorFromStage() {
        AtomicReference<Throwable> exception = new AtomicReference<>();
        assertEquals(await(Multi.createFrom().items("a", "b", "c")
                .map(word -> {
                    if (word.equals("b")) {
                        throw new QuietRuntimeException("failed");
                    }
                    return word.toUpperCase();
                })
                .onFailure().recoverWithMulti(err -> {
                    exception.set(err);
                    return Multi.createFrom().items("foo", "bar");
                })
                .collectItems().asList()
                .subscribeAsCompletionStage()), Arrays.asList("A", "foo", "bar"));
        assertEquals(exception.get().getMessage(), "failed");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void onErrorResumeStageShouldPropagateRuntimeExceptions() {
        await(Multi.createFrom().<String> failure(new Exception("source-failure"))
                .onFailure().recoverWithMulti(t -> {
                    throw new QuietRuntimeException("failed");
                })
                .collectItems().asList()
                .subscribeAsCompletionStage());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void onErrorResumeWithStageShouldPropagateRuntimeExceptions() {
        await(Multi.createFrom().<String> failure(new Exception("source-failure"))
                .onFailure().recoverWithItem(t -> {
                    throw new QuietRuntimeException("failed");
                })
                .collectItems().asList()
                .subscribeAsCompletionStage());
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = ".*boom.*")
    public void onErrorResumeWithShouldBeAbleToInjectAFailure() {
        await(Multi.createFrom().<String> failure(new QuietRuntimeException("failed"))
                .onFailure().recoverWithMulti(err -> Multi.createFrom().failure(new QuietRuntimeException("boom")))
                .collectItems().asList()
                .subscribeAsCompletionStage());
    }
}
