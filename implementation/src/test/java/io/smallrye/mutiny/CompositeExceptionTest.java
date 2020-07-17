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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

public class CompositeExceptionTest {

    private final static IllegalArgumentException IAE = new IllegalArgumentException("iae");
    private final static IOException IO = new IOException("io");
    private final static UnsupportedOperationException OUPS = new UnsupportedOperationException("oups");

    @Test
    public void testCreationWithArrays() {
        CompositeException ce1 = new CompositeException(IAE, IO, OUPS);
        CompositeException ce2 = new CompositeException(IO, IAE, OUPS);
        assertThat(ce1).hasMessageContaining("iae").hasMessageContaining("io").hasMessageContaining("oups");

        assertThat(ce1).getCause().isEqualTo(IAE);
        assertThat(ce2).getCause().isEqualTo(IO);

        assertThat(ce1.getCauses()).containsExactly(IAE, IO, OUPS);
        assertThat(ce1.getSuppressed()).containsExactly(IO, OUPS);
        assertThat(ce2.getCauses()).containsExactly(IO, IAE, OUPS);
        assertThat(ce2.getSuppressed()).containsExactly(IAE, OUPS);

        CompositeException ce3 = new CompositeException(OUPS);
        assertThat(ce3).hasMessageContaining("oups");
        assertThat(ce3.getCauses()).containsExactly(OUPS);
        assertThat(ce3.getCause()).isEqualTo(OUPS);
        assertThat(ce3.getSuppressed()).isEmpty();
    }

    @Test
    public void testCreationWithList() {
        CompositeException ce1 = new CompositeException(Arrays.asList(IAE, IO, OUPS));
        CompositeException ce2 = new CompositeException(Arrays.asList(IO, IAE, OUPS));
        assertThat(ce1).hasMessageContaining("iae").hasMessageContaining("io").hasMessageContaining("oups");

        assertThat(ce1).getCause().isEqualTo(IAE);
        assertThat(ce2).getCause().isEqualTo(IO);

        assertThat(ce1.getCauses()).containsExactly(IAE, IO, OUPS);
        assertThat(ce1.getSuppressed()).containsExactly(IO, OUPS);
        assertThat(ce2.getCauses()).containsExactly(IO, IAE, OUPS);
        assertThat(ce2.getSuppressed()).containsExactly(IAE, OUPS);

        CompositeException ce3 = new CompositeException(Collections.singletonList(OUPS));
        assertThat(ce3).hasMessageContaining("oups");
        assertThat(ce3.getCauses()).containsExactly(OUPS);
        assertThat(ce3.getCause()).isEqualTo(OUPS);
        assertThat(ce3.getSuppressed()).isEmpty();
    }

    @Test
    public void testCreationFromAnExisting() {
        CompositeException ce1 = new CompositeException(Arrays.asList(IAE, IO, OUPS));
        Exception another = new Exception("another");
        CompositeException ce2 = new CompositeException(ce1, another);

        assertThat(ce2.getCauses()).containsExactly(IAE, IO, OUPS, another);
        assertThat(ce2.getCause()).isEqualTo(IAE);
    }

    @SuppressWarnings("ThrowableNotThrown")
    @Test
    public void testCreationWithNull() {
        assertThatThrownBy(() -> new CompositeException((Throwable[]) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CompositeException((Throwable) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CompositeException((List<Throwable>) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CompositeException(Collections.singletonList(null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @SuppressWarnings("ThrowableNotThrown")
    @Test
    public void testCreationWithEmpty() {
        assertThatThrownBy(() -> new CompositeException(new Throwable[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CompositeException(Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testWithASingleException() {
        CompositeException ce = new CompositeException(IAE);
        assertThat(ce.getCauses()).hasSize(1);
        assertThat(ce.getCause()).isEqualTo(IAE);
        assertThat(ce.getSuppressed()).isEmpty();
    }

}
