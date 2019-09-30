package io.smallrye.reactive.unimulti.subscription;

public class BackPressureFailure extends RuntimeException {

    public BackPressureFailure(String message) {
        super(message);
    }

}
