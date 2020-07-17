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
package io.smallrye.mutiny.tuples;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public interface Tuple extends Iterable<Object> {

    /**
     * Get the item stored at the given index.
     *
     * @param index The index of the item to retrieve.
     * @return The item, can be {@code null}
     * @throws IndexOutOfBoundsException if the index is greater than the size.
     */
    Object nth(int index);

    /**
     * Gets a {@link java.util.List} of {@link Object Objects} containing the items composing this {@link Tuple}
     *
     * @return A list containing the item of the tuple.
     */
    List<Object> asList();

    /**
     * Gets an immutable {@link Iterator} traversing the content of this {@link Tuple}.
     *
     * @return the iterator
     */
    @Override
    default Iterator<Object> iterator() {
        return Collections.unmodifiableList(asList()).iterator();
    }

    /**
     * @return the number of items stored in the {@link Tuple}
     */
    int size();
}
