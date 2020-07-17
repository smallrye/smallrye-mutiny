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
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.MultiInterceptor;
import io.smallrye.mutiny.operators.AbstractMulti;

/**
 * Provides context propagation to Multi types.
 */
public class ContextPropagationMultiInterceptor implements MultiInterceptor {

    static final ThreadContext THREAD_CONTEXT = ContextManagerProvider.instance().getContextManager()
            .newThreadContextBuilder().build();

    @SuppressWarnings("SubscriberImplementation")
    @Override
    public <T> Subscriber<? super T> onSubscription(Publisher<? extends T> instance, Subscriber<? super T> subscriber) {
        Executor executor = THREAD_CONTEXT.currentContextExecutor();
        return new Subscriber<T>() {

            @Override
            public void onSubscribe(Subscription subscription) {
                executor.execute(() -> subscriber.onSubscribe(subscription));
            }

            @Override
            public void onNext(T item) {
                executor.execute(() -> subscriber.onNext(item));
            }

            @Override
            public void onError(Throwable failure) {
                executor.execute(() -> subscriber.onError(failure));
            }

            @Override
            public void onComplete() {
                executor.execute(subscriber::onComplete);
            }
        };
    }

    @Override
    public <T> Multi<T> onMultiCreation(Multi<T> multi) {
        Executor executor = THREAD_CONTEXT.currentContextExecutor();
        return new AbstractMulti<T>() {

            @Override
            public void subscribe(Subscriber<? super T> subscriber) {
                executor.execute(() -> multi.subscribe().withSubscriber(subscriber));
            }
        };
    }
}
