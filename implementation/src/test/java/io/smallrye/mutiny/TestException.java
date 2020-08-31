package io.smallrye.mutiny;

public class TestException extends RuntimeException {

    public TestException() {
        super("Test Exception");
    }

    public TestException(String message) {
        super(message);
    }
}
