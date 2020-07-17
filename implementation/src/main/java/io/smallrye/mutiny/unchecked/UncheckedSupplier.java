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
package io.smallrye.mutiny.unchecked;

import java.util.function.Supplier;

/**
 * Represents a supplier of items.
 * <p>
 * The supplier can throw {@link Exception Exceptions}.
 *
 * @param <T> the type of items supplied by this supplier
 */
@FunctionalInterface
public interface UncheckedSupplier<T> {

    /**
     * Creates a new {@link UncheckedSupplier} from an existing {@link Supplier}
     *
     * @param supplier the supplier
     * @param <T> the type of items supplied by this supplier
     * @return the new {@link UncheckedSupplier}
     */
    static <T> UncheckedSupplier<T> from(Supplier<T> supplier) {
        return supplier::get;
    }

    /**
     * Gets an item.
     *
     * @return an item
     * @throws Exception if anything wrong happen
     */
    T get() throws Exception;

    /**
     * @return the {@link Supplier} getting the item produced by this {@link UncheckedSupplier}. If an exception is
     *         thrown during the production, this exception is rethrown, wrapped into a {@link RuntimeException} if needed.
     */
    default Supplier<T> toSupplier() {
        return () -> {
            try {
                return get();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

}
