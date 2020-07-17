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
package io.smallrye.mutiny.operators.multi;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.operators.AbstractMulti;

/**
 * Abstract base class for operators that take an upstream source {@link Multi}.
 *
 * @param <I> the upstream value type / input type
 * @param <O> the output value type / produced type
 */
public abstract class AbstractMultiOperator<I, O> extends AbstractMulti<O> implements Multi<O> {

    /**
     * The upstream {@link Multi}.
     */
    protected final Multi<? extends I> upstream;

    /**
     * Creates a new {@link AbstractMultiOperator} with the passed {@link Multi} as upstream.
     *
     * @param upstream the upstream, must not be {@code null}
     */
    public AbstractMultiOperator(Multi<? extends I> upstream) {
        this.upstream = ParameterValidation.nonNull(upstream, "upstream");
    }

    public Multi<? extends I> upstream() {
        return upstream;
    }
}
