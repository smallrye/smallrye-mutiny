package io.smallrye.reactive.unimulti.operators;

import org.junit.Test;

import io.smallrye.reactive.unimulti.Uni;

public class UniNeverTest {

    @Test
    public void testTheBehaviorOfNever() {
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().<Void> nothing()
                .subscribe().withSubscriber(subscriber);
        subscriber.assertNoResult().assertNoResult().assertSubscribed();
    }
}
