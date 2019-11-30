package io.smallrye.mutiny.subscription;

public class BackPressureFailure extends RuntimeException {

    public BackPressureFailure(String message) {
        super(message);
    }

}
