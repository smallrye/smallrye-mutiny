package io.smallrye.mutiny.operators;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiCastTest {

    @Test
    public void testThatClassCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().item(1)
                .onItem().castTo(null));
    }

    @Test
    public void testCastThatWorks() {
        Multi.createFrom().item(1)
                .onItem().castTo(Number.class)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompleted()
                .assertItems(1);
    }

    @Test
    public void testCastThatDoesNotWork() {
        Multi.createFrom().item(1)
                .onItem().castTo(String.class)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(ClassCastException.class, "String");
    }
}
