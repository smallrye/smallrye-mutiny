package io.smallrye.mutiny.helpers;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ParameterValidationTest {

    @Test
    public void testUnexpectedSize() {
        assertThatThrownBy(() -> {
            List<Integer> list = Collections.singletonList(1);
            ParameterValidation.size(list, 2, "list");
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("list");

    }

    @Test
    public void testSizeWithNull() {
        assertThatThrownBy(() -> ParameterValidation.size(null, 2, "list")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("list");
    }

    @Test
    public void testNullArray() {
        assertThatThrownBy(() -> ParameterValidation.doesNotContainNull((Object[]) null, "array"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testArrayNoNullValue() {
        ParameterValidation.doesNotContainNull(new String[] { "foo", "bar", "baz" }, "array");
    }

    @Test
    public void testArrayWithNullValue() {
        String[] array = { "foo", "bar", null };
        assertThatThrownBy(() -> ParameterValidation.doesNotContainNull(array, "array"))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
