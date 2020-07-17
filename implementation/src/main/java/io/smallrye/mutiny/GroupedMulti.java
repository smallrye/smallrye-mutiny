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
package io.smallrye.mutiny;

/**
 * A specialization of {@link Multi} used by the {@code group} operation. It allows retrieving the computed key
 * associated with this {@link Multi}
 *
 * @param <K> the type of the key
 * @param <T> the type of the items emitted by this {@link Multi}
 */
public interface GroupedMulti<K, T> extends Multi<T> {

    /**
     * @return the key associated with this {@link GroupedMulti}
     */
    K key();
}
