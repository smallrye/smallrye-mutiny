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

import io.smallrye.mutiny.Multi;
import org.junit.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class UniBlockingTest {

    @Test
    public void test() {
        // tag::code[]
        Uni<String> blocking = Uni.createFrom().item(this::invokeRemoteServiceUsingBlockingIO)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
        // end::code[]
        assertThat(blocking.await().indefinitely()).isEqualTo("hello");
    }

    @Test
    public void testEmitOn() {
        // tag::code-emitOn[]
        Multi<String> multi = Multi.createFrom().items("john", "jack", "sue")
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transform(this::invokeRemoteServiceUsingBlockingIO);
        // end::code-emitOn[]
        assertThat(multi.collectItems().asList().await().indefinitely()).containsExactly("JOHN", "JACK", "SUE");
    }

    private String invokeRemoteServiceUsingBlockingIO() {
        try {
            Thread.sleep(300);  // NOSONAR
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "hello";
    }

    private String invokeRemoteServiceUsingBlockingIO(String s) {
        try {
            Thread.sleep(300); // NOSONAR
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return s.toUpperCase();
    }

}
