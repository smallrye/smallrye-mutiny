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
package tck;

/**
 * RuntimeException with no stack trace for expected failures, to make logging not so noisy.
 */
public class QuietRuntimeException extends RuntimeException {
    public QuietRuntimeException() {
        this(null, null);
    }

    public QuietRuntimeException(String message) {
        this(message, null);
    }

    public QuietRuntimeException(String message, Throwable cause) {
        super(message, cause, true, false);
    }

    public QuietRuntimeException(Throwable cause) {
        this(null, cause);
    }
}