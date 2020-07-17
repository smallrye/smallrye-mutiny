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

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

/**
 * Allow being notified when a new {@link Uni} instance is created and when this {@link Uni} receives events.
 * <p>
 * Implementations are expected to be exposed as SPI, and so the implementation class must be declared in the
 * {@code META-INF/services/io.smallrye.mutiny.infrastructure.UniInterceptor} file.
 */
public interface UniInterceptor {

    /**
     * @return the interceptor ordinal. The ordinal is used to sort the interceptor. Lower value are executed first.
     *         Default is 100.
     */
    default int ordinal() {
        return 100;
    }

    /**
     * Method called when a new instance of {@link Uni} is created. If can return a new {@code Uni},
     * or the passed {@code Uni} (default behavior) if the interceptor is not interested by this {@code uni}.
     * <p>
     * One use case for this method is the capture of a context at creation time (when the method is called) and
     * restored when a subscriber subscribed to the produced {@code uni}. It is recommended to extend
     * {@link AbstractUni} to produce a new {@link Uni} instance.
     *
     * @param uni the created uni
     * @param <T> the type of item produced by the uni
     * @return the passed uni or a new instance, must not be {@code null}
     */
    default <T> Uni<T> onUniCreation(Uni<T> uni) {
        return uni;
    }

    /**
     * Method called when a subscriber subscribes to a {@link Uni}.
     * This method lets you substitute the subscriber.
     *
     * @param instance the instance of uni
     * @param subscriber the subscriber
     * @param <T> the type of item
     * @return the subscriber to use instead of the passed one. By default, it returns the given subscriber.
     */
    default <T> UniSubscriber<? super T> onSubscription(Uni<T> instance, UniSubscriber<? super T> subscriber) {
        return subscriber;
    }

}
