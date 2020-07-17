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
package io.smallrye.mutiny.helpers;

import java.util.function.Predicate;

import io.smallrye.mutiny.operators.UniSerializedSubscriber;

public class Predicates {

    private Predicates() {
        // Avoid direct instantiation
    }

    public static <T> boolean testFailure(Predicate<? super Throwable> predicate,
            UniSerializedSubscriber<? super T> subscriber, Throwable failure) {
        if (predicate != null) {
            boolean pass;
            try {
                pass = predicate.test(failure);
            } catch (Throwable e) {
                subscriber.onFailure(e);
                return false;
            }
            if (!pass) {
                subscriber.onFailure(failure);
                return false;
            } else {
                // We pass!
                return true;
            }
        } else {
            return true;
        }
    }
}
