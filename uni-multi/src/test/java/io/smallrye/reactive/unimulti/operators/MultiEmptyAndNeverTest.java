package io.smallrye.reactive.unimulti.operators;

import org.junit.Test;

import io.smallrye.reactive.unimulti.Multi;

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
