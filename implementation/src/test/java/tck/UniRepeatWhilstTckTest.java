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

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class UniRepeatWhilstTckTest extends AbstractPublisherTck<Integer> {
    @Override
    public Publisher<Integer> createPublisher(long elements) {
        if (elements == 0) {
            return Multi.createFrom().empty();
        }
        AtomicInteger count = new AtomicInteger();
        return Uni.createFrom().item(1).repeat().whilst(x -> count.getAndIncrement() < elements - 1);
    }
}
