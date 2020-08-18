package io.smallrye.mutiny.operators;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiCreateFromRangeTest {

    @Test
    public void testARangeFrom0to10() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();
        Multi.createFrom().range(1, 10).subscribe().withSubscriber(subscriber)
                .request(3)
                .assertReceived(1, 2, 3)
                .assertHasNotCompleted()
                .request(10)
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testARangeFrom0to10WithFullConsumptionAtSubscription() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(9);
        Multi.createFrom().range(1, 10).subscribe().withSubscriber(subscriber)
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .assertCompletedSuccessfully()
                .request(3)
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .assertCompletedSuccessfully();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatEndBoundaryCannotBeLessThanStart() {
        Multi.createFrom().range(1, -1);
    }

}
