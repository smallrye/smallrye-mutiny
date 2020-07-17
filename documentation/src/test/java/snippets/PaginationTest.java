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
import io.smallrye.mutiny.test.MultiAssertSubscriber;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class PaginationTest {

    @SuppressWarnings("Convert2MethodRef")
    @Test
    public void test() {
        // tag::code[]
        PaginatedApi api = new PaginatedApi();

        Multi<String> stream = Multi.createBy().repeating()
                .completionStage(
                        () -> new AtomicInteger(),
                        state -> api.getPage(state.getAndIncrement()))
                .until(list -> list.isEmpty())
                .onItem().disjoint();
        // end::code[]
        stream.subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived("a", "b", "c", "d", "e", "f", "g", "h");

    }

    @SuppressWarnings("Convert2MethodRef")
    @Test
    public void test2() {
        // tag::code2[]
        PaginatedApi api = new PaginatedApi();

        Multi<Page> stream = Multi.createBy().repeating()
                .uni(
                        () -> new AtomicInteger(),
                        state -> api.retrieve(state.getAndIncrement()))
                .whilst(page -> page.hasNext());
        // end::code2[]
        MultiAssertSubscriber<Page> subscriber = stream.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully();

        assertThat(subscriber.items()).hasSize(3);

    }

    private static class Page {
        final boolean hasNext;

        private Page(boolean hasNext) {
            this.hasNext = hasNext;
        }

        public boolean hasNext() {
            return hasNext;
        }
    }


    private class PaginatedApi {

        Map<Integer, List<String>> pages = new LinkedHashMap<>();

        public PaginatedApi() {
            pages.put(0, Arrays.asList("a", "b", "c"));
            pages.put(1, Arrays.asList("d", "e", "f"));
            pages.put(2, Arrays.asList("g", "h"));
            pages.put(3, Collections.emptyList());
        }

        CompletionStage<List<String>> getPage(int page) {
            List<String> strings = pages.get(page);
            if (strings == null) {
                strings = Collections.emptyList();
            }
            return CompletableFuture.completedFuture(strings);
        }


        Uni<Page> retrieve(int page) {
            if (page == 2) {
                return Uni.createFrom().item(new Page(false));
            }
            return Uni.createFrom().item(new Page(true));
        }
    }

}
