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
package io.smallrye.mutiny.streams.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

import io.smallrye.mutiny.Multi;

public class Transformer {

    private final ExecutionModel model;

    private static final Transformer INSTANCE;

    static {
        INSTANCE = new Transformer();
    }

    private Transformer() {
        ServiceLoader<ExecutionModel> loader = ServiceLoader.load(ExecutionModel.class);
        Iterator<ExecutionModel> iterator = loader.iterator();
        if (iterator.hasNext()) {
            model = iterator.next();
        } else {
            model = i -> i;
        }
    }

    /**
     * Calls the model.
     *
     * @param upstream the upstream
     * @param <T> the type of data
     * @return the decorated stream if needed
     */
    @SuppressWarnings("unchecked")
    public static <T> Multi<T> apply(Multi<T> upstream) {
        return INSTANCE.model.apply(upstream);
    }

}
