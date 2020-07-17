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

import java.util.stream.LongStream;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiOnFailureRecoverWithItemTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long l) {
        return Multi.createFrom().<Long> failure(new RuntimeException("failed"))
                .onFailure().recoverWithMulti(t -> Multi.createFrom().items(LongStream.rangeClosed(1, l).boxed()));
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new RuntimeException("failed"))
                .onFailure().recoverWithItem(t -> {
                    // Re-throw the exception.
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    // Wrap if required.
                    throw new RuntimeException(t);
                });
    }

}
