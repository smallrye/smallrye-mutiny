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
package io.smallrye.mutiny.converters.multi;

public class MultiRxConverters {

    private MultiRxConverters() {
        // Avoid direct instantiation
    }

    public static FromCompletable fromCompletable() {
        return FromCompletable.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> FromSingle<T> fromSingle() {
        return FromSingle.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> FromMaybe<T> fromMaybe() {
        return FromMaybe.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> FromFlowable<T> fromFlowable() {
        return FromFlowable.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> FromObservable<T> fromObservable() {
        return FromObservable.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ToSingle<T> toSingle() {
        return ToSingle.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ToCompletable<T> toCompletable() {
        return ToCompletable.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ToMaybe<T> toMaybe() {
        return ToMaybe.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ToFlowable<T> toFlowable() {
        return ToFlowable.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ToObservable<T> toObservable() {
        return ToObservable.INSTANCE;
    }

}
