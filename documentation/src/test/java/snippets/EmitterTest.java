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

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class EmitterTest {

    @Test
    public void test() {
        Client client = new Client();
        // tag::code[]

        Uni<String> uni = Uni.createFrom().emitter(emitter -> {
            client.execute(ar -> {
                if (ar.failed()) {
                    emitter.fail(ar.cause());
                } else {
                    emitter.complete(ar.value());
                }
            });
        });

        Multi<String> multi = Multi.createFrom().emitter(emitter -> {
            client.onMessage(e -> {
                if (e != null) {
                    emitter.emit(e);
                } else {
                    emitter.complete();
                }
            });
        });
        // end::code[]

        assertThat(uni.await().indefinitely()).isEqualTo("hello");
        assertThat(multi.collectItems().asList().await().indefinitely()).containsExactly("a", "b", "c");
    }

    class Client {
        void execute(Consumer<AsyncResult<String>> consumer) {
            consumer.accept(new AsyncResult<>("hello"));
        }

        void onMessage(Consumer<String> listener) {
            listener.accept("a");
            listener.accept("b");
            listener.accept("c");
            listener.accept(null);
        }
    }

    class AsyncResult<T> {

        final T value;

        public AsyncResult(T value) {
            this.value = value;
        }

        public boolean failed() {
            return false;
        }

        public Throwable cause() {
            return null;
        }

        public T value() {
            return value;
        }
    }
}