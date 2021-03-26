package org.reactivestreams.tck.junit5;

import java.util.logging.Logger;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.testng.SkipException;

public class TestngSkippingExtension implements TestExecutionExceptionHandler {

    private static final Logger logger = Logger.getLogger(TestngSkippingExtension.class.getName());

    @Override
    public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        if (throwable instanceof SkipException) {
            Assumptions.assumeTrue(false, "[skip] " + extensionContext.getDisplayName() + " -> " + throwable.getMessage());
        } else {
            throw throwable;
        }
    }
}
