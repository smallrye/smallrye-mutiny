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
package snippets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.Test;

import io.smallrye.mutiny.Multi;

public class MultiFailureTest {

    @Test
    public void test() {
        Multi<String> multi = Multi.createFrom().failure(new IOException("boom"));
        // tag::code[]

        CompletableFuture<String> res0 = multi.onFailure().transform(failure -> new MyBusinessException("oh no!"))
                .collectItems().first()
                .subscribeAsCompletionStage();

        String res1 = multi
                .onFailure().recoverWithItem("hello")
                .collectItems().first()
                .await().indefinitely();

        String res2 = multi
                .onFailure(IllegalArgumentException.class).recoverWithItem("bonjour")
                .onFailure(IOException.class).recoverWithItem("hello")
                .collectItems().first()
                .await().indefinitely();

        String res3 = multi
                .onFailure().recoverWithMulti(() -> Multi.createFrom().items("a", "b", "c"))
                .collectItems().first()
                .await().indefinitely();

        CompletableFuture<String> res4 = multi
                .onFailure().retry().atMost(2)
                .collectItems().first()
                .subscribeAsCompletionStage();

        // end::code[]

        assertThatThrownBy(res0::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(MyBusinessException.class);
        assertThat(res1).isEqualTo("hello");
        assertThat(res2).isEqualTo("hello");
        assertThat(res3).isEqualTo("a");
        assertThatThrownBy(res4::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    private class MyBusinessException extends Exception {

        MyBusinessException(String s) {
            super(s);
        }
    }
}
