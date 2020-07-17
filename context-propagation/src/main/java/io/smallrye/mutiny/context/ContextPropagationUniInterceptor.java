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
package io.smallrye.mutiny.context;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.UniInterceptor;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniSerializedSubscriber;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

/**
 * Provides context propagation to Uni types.
 */
public class ContextPropagationUniInterceptor implements UniInterceptor {

    static final ThreadContext THREAD_CONTEXT = ContextManagerProvider.instance().getContextManager()
            .newThreadContextBuilder().build();

    @Override
    public <T> UniSubscriber<? super T> onSubscription(Uni<T> instance, UniSubscriber<? super T> subscriber) {
        Executor executor = THREAD_CONTEXT.currentContextExecutor();
        return new UniSubscriber<T>() {

            @Override
            public void onSubscribe(UniSubscription subscription) {
                executor.execute(() -> subscriber.onSubscribe(subscription));
            }

            @Override
            public void onItem(T item) {
                executor.execute(() -> subscriber.onItem(item));
            }

            @Override
            public void onFailure(Throwable failure) {
                executor.execute(() -> subscriber.onFailure(failure));
            }
        };
    }

    @Override
    public <T> Uni<T> onUniCreation(Uni<T> uni) {
        Executor executor = THREAD_CONTEXT.currentContextExecutor();
        return new AbstractUni<T>() {
            @Override
            protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
                executor.execute(() -> AbstractUni.subscribe(uni, subscriber));
            }
        };
    }
}
