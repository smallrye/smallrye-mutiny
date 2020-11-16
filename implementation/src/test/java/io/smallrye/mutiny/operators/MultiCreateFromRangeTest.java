package io.smallrye.mutiny.operators;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiCreateFromRangeTest {

    @Test
    public void testARangeFrom0to10() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();
        Multi.createFrom().range(1, 10).subscribe().withSubscriber(subscriber)
                .request(3)
                .assertItems(1, 2, 3)
                .assertNotTerminated()
                .request(10)
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .assertCompleted();
    }

    @Test
    public void testARangeFrom0to10WithFullConsumptionAtSubscription() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(9);
        Multi.createFrom().range(1, 10).subscribe().withSubscriber(subscriber)
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .assertCompleted()
                .request(3)
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .assertCompleted();
    }

    @Test
    public void testThatEndBoundaryCannotBeLessThanStart() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, -1));
    }

}
