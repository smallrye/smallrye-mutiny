package io.smallrye.reactive.operators;

import java.io.IOException;

import org.junit.Test;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.test.MultiAssertSubscriber;

public class MultiDistinctTest {

    @Test
    public void testDistinctWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .transform().byKeepingDistinctItems()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDistinct() {
        Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .transform().byKeepingDistinctItems()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testDistinctOnAStreamWithoutDuplicates() {
        Multi.createFrom().range(1, 5)
                .transform().byKeepingDistinctItems()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testDropRepetitionsWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDropRepetitions() {
        Multi.createFrom().items(1, 2, 3, 4, 4, 2, 2, 4, 1, 1, 2, 4)
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 2, 4, 1, 2, 4);
    }

    @Test
    public void testDropRepetitionsOnAStreamWithoutDuplicates() {
        Multi.createFrom().range(1, 5)
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

}
