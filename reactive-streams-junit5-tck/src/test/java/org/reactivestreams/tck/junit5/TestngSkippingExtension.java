package org.reactivestreams.tck.junit5;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.testng.SkipException;

public class TestngSkippingExtension implements TestExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        if (throwable instanceof SkipException) {
            System.out.println("[skip] " + extensionContext.getDisplayName() + " -> " + throwable.getMessage());
        } else {
            throw throwable;
        }
    }
}
