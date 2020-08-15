package io.smallrye.mutiny.helpers;

import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

public class ParameterValidationTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testUnexpectedSize() {
        List<Integer> list = Collections.singletonList(1);
        ParameterValidation.size(list, 2, "list");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSizeWithNull() {
        ParameterValidation.size(null, 2, "list");
    }

}
