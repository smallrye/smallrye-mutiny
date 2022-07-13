package io.smallrye.mutiny.helpers.queues;

import static org.mockito.Mockito.mock;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.internal.util.QueueDrainHelper;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

public class DrainUtilsTest {

    @Test
    public void testPostCompleteWithNoItems() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        AtomicLong state = new AtomicLong();
        Flow.Subscription subscription = mock(Flow.Subscription.class);
        subscriber.onSubscribe(subscription);

        DrainUtils.postComplete(subscriber, queue, state, () -> false);

        subscriber.assertHasNotReceivedAnyItem()
                .assertCompleted();

    }

    @Test
    public void testPostCompleteWithRequest() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        AtomicLong state = new AtomicLong();
        Flow.Subscription subscription = mock(Flow.Subscription.class);
        subscriber.onSubscribe(subscription);
        queue.offer(1);
        state.getAndIncrement();
        DrainUtils.postComplete(subscriber, queue, state, () -> false);
        subscriber
                .assertCompleted()
                .assertItems(1);
    }

    @RepeatedTest(100)
    public void testCompleteVsRequestRace() throws InterruptedException {
        BooleanSupplier isCancelled = () -> false;
        Flow.Subscription subscription = mock(Flow.Subscription.class);
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        AtomicLong requested = new AtomicLong();
        subscriber.onSubscribe(subscription);

        queue.offer(1);

        CountDownLatch start = new CountDownLatch(2);
        CountDownLatch done = new CountDownLatch(2);
        Runnable r1 = () -> {
            start.countDown();
            try {
                start.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            DrainUtils.postCompleteRequest(1L, subscriber, queue, requested, isCancelled);
            done.countDown();
        };

        Runnable r2 = () -> {
            start.countDown();
            try {
                start.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            DrainUtils.postComplete(subscriber, queue, requested, isCancelled);
            done.countDown();
        };

        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);

        runnables.forEach(r -> new Thread(r).start());

        done.await();
        subscriber.assertItems(1);
    }

    @Test
    public void testPostCompleteAfterCancellation() {
        AssertSubscriber<Integer> subscriber = new AssertSubscriber<>(1);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        AtomicLong state = new AtomicLong();
        Flow.Subscription subscription = mock(Flow.Subscription.class);
        subscriber.onSubscribe(subscription);
        queue.offer(1);
        state.getAndIncrement();
        subscriber.cancel();

        QueueDrainHelper.postComplete(AdaptersToReactiveStreams.subscriber(subscriber), queue, state, subscriber::isCancelled);
        subscriber
                .assertSubscribed()
                .assertHasNotReceivedAnyItem()
                .assertNotTerminated();
    }

    @Test
    public void testPostCompleteCancelledAfterReceptionOfTheFirstItem() {
        final AssertSubscriber<Integer> subscriber = new AssertSubscriber<Integer>(10) {
            @Override
            public void onNext(Integer item) {
                // Cancel just after the item reception.
                super.onNext(item);
                cancel();
            }
        };
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        AtomicLong state = new AtomicLong();
        Flow.Subscription subscription = mock(Flow.Subscription.class);
        subscriber.onSubscribe(subscription);
        queue.offer(1);
        state.getAndIncrement();

        DrainUtils.postComplete(subscriber, queue, state, subscriber::isCancelled);
        subscriber
                .assertNotTerminated()
                .assertItems(1);
    }

    @Test
    public void testPostCompleteAlreadyComplete() {
        AssertSubscriber<Integer> subscriber = new AssertSubscriber<>(1);
        Queue<Integer> q = new ArrayDeque<>();
        q.offer(1);
        AtomicLong state = new AtomicLong(DrainUtils.COMPLETED_MASK);
        DrainUtils.postComplete(subscriber, q, state, () -> false);
    }

}
