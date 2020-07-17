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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;

/**
 * Allow being notified when a new {@link Multi} instance is created and when this {@link Multi} receives events.
 * <p>
 * Implementations are expected to be exposed as SPI, and so the implementation class must be declared in the
 * {@code META-INF/services/io.smallrye.mutiny.infrastructure.MultiInterceptor} file.
 */
public interface MultiInterceptor {

    /**
     * Default ordinal value.
     */
    int DEFAULT_ORDINAL = 100;

    /**
     * @return the interceptor ordinal. The ordinal is used to sort the interceptor. Lower value are executed first.
     *         Default is 100.
     */
    default int ordinal() {
        return DEFAULT_ORDINAL;
    }

    /**
     * Method called when a new instance of {@link Multi} is created. If can return a new {@code Multi},
     * or the passed {@code Multi} (default behavior) if the interceptor is not interested by this {@code Multi}.
     * <p>
     * One use case for this method is the capture of a context at creation time (when the method is called) and
     * restored when a subscriber subscribed to the produced {@code multi}. It is recommended to extend
     * {@link io.smallrye.mutiny.operators.AbstractMulti} to produce a new {@link Multi} instance.
     *
     * @param multi the created multi
     * @param <T> the type of item produced by the multi
     * @return the passed multi or a new instance, must not be {@code null}
     */
    default <T> Multi<T> onMultiCreation(Multi<T> multi) {
        return multi;
    }

    /**
     * Method called when a subscriber subscribes to a {@link Multi}.
     * This method lets you substitute the subscriber.
     *
     * @param instance the instance of publisher
     * @param subscriber the subscriber
     * @param <T> the type of item
     * @return the subscriber to use instead of the passed one. By default, it returns the given subscriber.
     */
    default <T> Subscriber<? super T> onSubscription(Publisher<? extends T> instance, Subscriber<? super T> subscriber) {
        return subscriber;
    }

}
