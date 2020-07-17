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
package io.smallrye.mutiny.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class Mocks {

    /**
     * Mocks a subscriber and prepares it to request {@code Long.MAX_VALUE}.
     * 
     * @param <T> the value type
     * @return the mocked subscriber
     */
    @SuppressWarnings("unchecked")
    public static <T> Subscriber<T> subscriber() {
        Subscriber<T> w = mock(Subscriber.class);

        Mockito.doAnswer((Answer<Object>) a -> {
            Subscription s = a.getArgument(0);
            s.request(Long.MAX_VALUE);
            return null;
        }).when(w).onSubscribe(any());

        return w;
    }

}
