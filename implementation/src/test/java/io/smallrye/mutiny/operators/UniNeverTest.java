package io.smallrye.mutiny.operators;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

public class UniNeverTest {

    @Test
    public void testTheBehaviorOfNever() {
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().<Void> nothing()
                .subscribe().withSubscriber(subscriber);
        subscriber.assertSubscribed().assertNotTerminated();
        ;
    }
}
