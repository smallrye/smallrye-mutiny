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

import io.smallrye.mutiny.Uni;
import org.junit.Test;

import io.smallrye.mutiny.Multi;

public class EventsTest {

    @Test
    public void test() {
        // tag::code[]
        Multi<String> source = Multi.createFrom().items("a", "b", "c");

        source
                .onItem().invoke(item -> System.out.println("Received item " + item))
                .onFailure().invoke(failure -> System.out.println("Failed with " + failure.getMessage()))
                .onCompletion().invoke(() -> System.out.println("Completed"))
                .on().subscribed(subscription -> System.out.println("We are subscribed!"))

                .on().cancellation(() -> System.out.println("Downstream has cancelled the interaction"))
                .on().request(n -> System.out.println("Downstream requested " + n + " items"))
                .subscribe().with(item -> {
                });
        // end::code[]

        // tag::shortcut[]
        Multi<String> multi = Multi.createFrom().items("a", "b", "c");
        multi.invoke(item -> System.out.println("Received item " + item));
        // end::shortcut[]

        // tag::invoke-uni[]
        multi.invokeUni(item -> executeAnAsyncAction(item));
        // end::invoke-uni[]

    }

    private Uni<?> executeAnAsyncAction(String item) {
        return Uni.createFrom().nullItem();
    }
}
