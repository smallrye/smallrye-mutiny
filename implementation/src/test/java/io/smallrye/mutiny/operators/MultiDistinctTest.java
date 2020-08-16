package io.smallrye.mutiny.operators;

import java.io.IOException;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiDistinctTest {

    @Test
    public void testDistinctWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .transform().byDroppingDuplicates()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDistinct() {
        Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .transform().byDroppingDuplicates()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testDistinctOnAStreamWithoutDuplicates() {
        Multi.createFrom().range(1, 5)
                .transform().byDroppingDuplicates()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testDropRepetitionsWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDropRepetitions() {
        Multi.createFrom().items(1, 2, 3, 4, 4, 2, 2, 4, 1, 1, 2, 4)
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 2, 4, 1, 2, 4);
    }

    @Test
    public void testDropRepetitionsOnAStreamWithoutDuplicates() {
        Multi.createFrom().range(1, 5)
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

}
