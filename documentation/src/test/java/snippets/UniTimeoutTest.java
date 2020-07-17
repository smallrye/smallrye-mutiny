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

import java.time.Duration;

import org.junit.Test;

import io.smallrye.mutiny.Uni;

public class UniTimeoutTest {

    @Test
    public void test() {
        Uni<String> uni = Uni.createFrom().nothing();
        // tag::code[]
        String item = uni
                .ifNoItem().after(Duration.ofMillis(100)).recoverWithItem("some fallback item")
                .await().indefinitely();
        // end::code[]

        assertThat(item).isEqualTo("some fallback item");
    }

}
