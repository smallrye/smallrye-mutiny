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
package io.smallrye.mutiny.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.reactivestreams.Subscriber;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.test.Mocks;

public class MultiInterceptorTest {

    @Test
    public void testDefaultInterceptorBehavior() {
        MultiInterceptor interceptor = new MultiInterceptor() {
            // Default.
        };

        assertThat(interceptor.ordinal()).isEqualTo(MultiInterceptor.DEFAULT_ORDINAL);
        Multi<String> multi = new AbstractMulti<String>() {
            // Do nothing
        };
        assertThat(interceptor.onMultiCreation(multi)).isSameAs(multi);

        Subscriber<Object> subscriber = Mocks.subscriber();
        assertThat(interceptor.onSubscription(multi, subscriber)).isSameAs(subscriber);
    }

}
