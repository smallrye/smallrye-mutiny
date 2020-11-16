package io.smallrye.mutiny.operators;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiEmptyAndNeverTest {

    @Test
    public void testEmpty() {
        Multi<String> nothing = Multi.createFrom().empty();
        nothing.subscribe().withSubscriber(AssertSubscriber.create())
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testNever() {
        Multi<String> nothing = Multi.createFrom().nothing();
        nothing.subscribe().withSubscriber(AssertSubscriber.create())
                .assertNotTerminated()
                .request(2)
                .assertNotTerminated();
    }

}
