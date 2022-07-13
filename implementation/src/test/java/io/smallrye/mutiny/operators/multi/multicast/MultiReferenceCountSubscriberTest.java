package io.smallrye.mutiny.operators.multi.multicast;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.concurrent.Flow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
class MultiReferenceCountSubscriberTest {

    @Test
    public void testCancellationWithASingleSubscriber() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3, 4, 5)
                .broadcast().withCancellationAfterLastSubscriberDeparture().toAllSubscribers();

        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.request(2).cancel();
        subscriber.assertItems(1, 2, 3);
    }

    @Test
    public void testCancellationWithATwoSubscribers() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3, 4, 5)
                .broadcast().withCancellationAfterLastSubscriberDeparture().toAllSubscribers();

        AssertSubscriber<Integer> subscriber1 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(0));
        AssertSubscriber<Integer> subscriber2 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(0));

        subscriber2.request(3);
        subscriber1.request(3).cancel();
        subscriber1.assertItems(1, 2, 3);
        subscriber2.request(20);
        subscriber2.assertItems(1, 2, 3, 4, 5);
    }

    @Test
    public void testFailureWithASingleSubscriber() {
        Multi<Integer> multi = Multi.createBy().concatenating().streams(
                Multi.createFrom().items(1, 2, 3, 4, 5),
                Multi.createFrom().failure(new IOException("boom")))
                .broadcast()
                .withCancellationAfterLastSubscriberDeparture().toAllSubscribers();

        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber
                .assertFailedWith(IOException.class, "boom");

    }

    @Test
    public void testFailureWithATwoSubscribers() {
        Multi<Integer> multi = Multi.createBy().concatenating().streams(
                Multi.createFrom().items(1, 2, 3, 4, 5),
                Multi.createFrom().failure(new IOException("boom")))
                .broadcast()
                .withCancellationAfterLastSubscriberDeparture().toAllSubscribers();

        AssertSubscriber<Integer> subscriber1 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        AssertSubscriber<Integer> subscriber2 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber1
                .assertFailedWith(IOException.class, "boom");

        subscriber2
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testFailureWithRogueUpstream() {
        Multi<Integer> multi = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(MultiSubscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(mock(Flow.Subscription.class));
                subscriber.onItem(1);
                subscriber.onItem(2);
                subscriber.onItem(3);
                subscriber.onFailure(new IOException("boom"));
                subscriber.onItem(4);
                subscriber.onItem(5);
                subscriber.onComplete();
                subscriber.onItem(6);
            }
        }
                .broadcast()
                .withCancellationAfterLastSubscriberDeparture().toAllSubscribers();

        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber
                .assertFailedWith(IOException.class, "boom");

    }

}
