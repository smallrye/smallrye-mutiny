package io.smallrye.reactive.operators;

import io.smallrye.reactive.Multi;
import org.junit.Test;

public class MultiCastTest {

    @Test(expected = IllegalArgumentException.class)
    public void testThatClassCannotBeNull() {
        Multi.createFrom().result(1)
                .onResult().castTo(null);
    }


    @Test
    public void testCastThatWorks() {
        Multi.createFrom().result(1)
                .onResult().castTo(Number.class)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertCompletedSuccessfully()
                .assertReceived(1);
    }

    @Test
    public void testCastThatDoesNotWork() {
        Multi.createFrom().result(1)
                .onResult().castTo(String.class)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertHasFailedWith(ClassCastException.class, "String");
    }
}
