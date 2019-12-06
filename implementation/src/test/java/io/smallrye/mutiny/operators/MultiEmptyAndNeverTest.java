package io.smallrye.mutiny.operators;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiEmptyAndNeverTest {

    @Test
    public void testEmpty() {
        Multi<String> nothing = Multi.createFrom().empty();
        nothing.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testNever() {
        Multi<String> nothing = Multi.createFrom().nothing();
        nothing.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertNotTerminated()
                .request(2)
                .assertNotTerminated();
    }

}
