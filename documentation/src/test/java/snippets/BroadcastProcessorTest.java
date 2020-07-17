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
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.test.MultiAssertSubscriber;
import org.junit.Test;

public class BroadcastProcessorTest {

    @Test
    public void test() {
        // tag::code[]
        BroadcastProcessor<String> processor = BroadcastProcessor.create();
        Multi<String> multi = processor
                .onItem().transform(String::toUpperCase)
                .onFailure().recoverWithItem("d'oh");

        new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                processor.onNext(Integer.toString(i));
            }
            processor.onComplete();
        }).start();

        // Subscribers can subscribe at any time.
        // They will only receive items emitted after their subscription.
        // If the source is already terminated (by a completion or a failure signal)
        // the subscriber receives this signal.

        // end::code[]
        MultiAssertSubscriber<String> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        multi.subscribe().withSubscriber(subscriber)
                .await()
                .assertCompletedSuccessfully();
    }
}
