package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import org.junit.Test;

public class UniNeverTest {

    @Test
    public void testTheBehaviorOfNever() {
        AssertSubscriber<Void> subscriber = AssertSubscriber.create();
        Uni.createFrom().<Void>nothing()
                .subscribe().withSubscriber(subscriber);
        subscriber.assertNoResult().assertNoResult().assertSubscribed();
    }
}
