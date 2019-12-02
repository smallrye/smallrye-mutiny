package io.smallrye.mutiny.operators;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiCastTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatClassCannotBeNull() {
        Multi.createFrom().item(1)
                .onItem().castTo(null);
    }

    @Test
    public void testCastThatWorks() {
        Multi.createFrom().item(1)
                .onItem().castTo(Number.class)
                .subscribe().with(MultiAssertSubscriber.create(1))
                .assertCompletedSuccessfully()
                .assertReceived(1);
    }

    @Test
    public void testCastThatDoesNotWork() {
        Multi.createFrom().item(1)
                .onItem().castTo(String.class)
                .subscribe().with(MultiAssertSubscriber.create(1))
                .assertHasFailedWith(ClassCastException.class, "String");
    }
}
