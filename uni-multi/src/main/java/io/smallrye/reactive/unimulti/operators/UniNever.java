package io.smallrye.reactive.unimulti.operators;

import static io.smallrye.reactive.unimulti.helpers.EmptyUniSubscription.CANCELLED;

public class UniNever<T> extends AbstractUni<T> {
    public static final UniNever<Object> INSTANCE = new UniNever<>();

    private UniNever() {
        // avoid direct instantiation.
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        subscriber.onSubscribe(CANCELLED);
    }
}
