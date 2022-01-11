package org.reactivestreams.tck.junit5;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testng.SkipException;

@ExtendWith(TestngSkippingExtension.class)
class TestngSkippingExtensionTest {

    @Test
    void passingTest() {
        Assertions.assertEquals(1, 1);
    }

    @Test
    void skippedTest() {
        throw new SkipException("Please skip this test");
    }
}
