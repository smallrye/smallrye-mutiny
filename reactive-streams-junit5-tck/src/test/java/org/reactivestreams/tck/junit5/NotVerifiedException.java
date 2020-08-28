package org.reactivestreams.tck.junit5;

public class NotVerifiedException extends RuntimeException {
    public NotVerifiedException(String message) {
        super(message);
    }
}
