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

import java.util.concurrent.Executor;

/**
 * SPI allowing customizing the default executor.
 * Implementors must register their implementation by indicating the fully qualified name of the implementation in the
 * {@code META-INF/services/io.smallrye.reactive.infrastructure.ExecutorConfiguration} file.
 * <p>
 * The SPI implementation is responsible for creating and terminating the created thread pools.
 */
public interface ExecutorConfiguration {

    /**
     * Gets the default executor.
     *
     * @return the default executor.
     */
    Executor getDefaultWorkerExecutor();

}
