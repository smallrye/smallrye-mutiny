package io.smallrye.mutiny.zero.impl;

import org.reactivestreams.Subscription;

public class AlreadyCompletedSubscription implements Subscription {

    @Override
    public void request(long n) {
        // Do nothing
    }

    @Override
    public void cancel() {
        // Do nothing
    }
}
