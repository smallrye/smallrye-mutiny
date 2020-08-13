package io.smallrye.mutiny.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;

public class SubscriptionsTest {

    @Test
    public void addFailures() {
        AtomicReference<Throwable> container = new AtomicReference<>();

        IOException boom = new IOException("boom");
        IllegalStateException ise = new IllegalStateException("boom");
        NullPointerException npe = new NullPointerException("boom!");
        assertThat(Subscriptions.addFailure(container, boom)).isTrue();
        assertThat(container.get()).isEqualTo(boom);

        assertThat(Subscriptions.addFailure(container, ise)).isTrue();
        assertThat(container.get()).isInstanceOf(CompositeException.class);
        CompositeException ce = (CompositeException) container.get();
        assertThat(ce.getCauses()).containsExactly(boom, ise);

        assertThat(Subscriptions.addFailure(container, npe)).isTrue();
        assertThat(container.get()).isInstanceOf(CompositeException.class);
        ce = (CompositeException) container.get();
        assertThat(ce.getCauses()).containsExactly(boom, ise, npe);

        assertThat(Subscriptions.markFailureAsTerminated(container)).isInstanceOf(CompositeException.class);
        assertThat(Subscriptions.addFailure(container, boom)).isFalse();
        assertThat(container).hasValue(Subscriptions.TERMINATED);
    }

    @Test
    public void deferredSubscriptionEmptyCancellationTest() {
        Subscriptions.DeferredSubscription subscription = new Subscriptions.DeferredSubscription();
        assertThat(subscription.isCancelled()).isFalse();
        subscription.cancel();
        assertThat(subscription.isCancelled()).isTrue();
    }

    @Test
    public void deferredSubscriptionRequestWithoutSubscription() {
        Subscriptions.DeferredSubscription subscription = new Subscriptions.DeferredSubscription();
        subscription.request(20);
        Subscription newSub = mock(Subscription.class);
        assertThat(subscription.set(newSub)).isTrue();
        verify(newSub).request(20);
    }

    @Test
    public void deferredSubscriptionSetAfterCancellation() {
        Subscriptions.DeferredSubscription subscription = new Subscriptions.DeferredSubscription();
        subscription.request(20);
        subscription.cancel();
        Subscription newSub = mock(Subscription.class);
        assertThat(subscription.set(newSub)).isFalse();
        verify(newSub, never()).request(anyLong());
        verify(newSub).cancel();
    }

    @Test
    public void deferredSubscriptionSetTwiceCancelTheSecondOne() {
        Subscriptions.DeferredSubscription subscription = new Subscriptions.DeferredSubscription();
        Subscription s1 = mock(Subscription.class);
        assertThat(subscription.set(s1)).isTrue();
        subscription.request(20);
        verify(s1).request(20);
        Subscription newSub = mock(Subscription.class);
        assertThat(subscription.set(newSub)).isFalse();
        verify(newSub, never()).request(anyLong());
        verify(newSub).cancel();
    }

    @Test(invocationCount = 1000)
    public void requestIfNotNullOrAccumulateRace() throws InterruptedException {
        AtomicReference<Subscription> container = new AtomicReference<>();
        Subscription subscription = mock(Subscription.class);
        AtomicLong requested = new AtomicLong();

        CountDownLatch latch = new CountDownLatch(2);
        Runnable r1 = () -> {
            Subscriptions.requestIfNotNullOrAccumulate(container, requested, 20);
            latch.countDown();
        };
        Runnable r2 = () -> {
            container.set(subscription);
            latch.countDown();
        };

        shuffleAndRun(r1, r2);

        latch.await();
    }

    @Test(invocationCount = 1000)
    public void setIfEmptyAndRequestRace() throws InterruptedException {
        final AtomicReference<Subscription> atomicSubscription = new AtomicReference<>();
        final AtomicLong r = new AtomicLong();

        final AtomicLong q = new AtomicLong();

        final Subscription a = new Subscription() {
            @Override
            public void request(long n) {
                q.addAndGet(n);
            }

            @Override
            public void cancel() {
                // Ignored.
            }
        };

        CountDownLatch latch = new CountDownLatch(2);
        Runnable r1 = () -> {
            Subscriptions.setIfEmptyAndRequest(atomicSubscription, r, a);
            latch.countDown();
        };
        Runnable r2 = () -> {
            Subscriptions.requestIfNotNullOrAccumulate(atomicSubscription, r, 1);
            latch.countDown();
        };

        shuffleAndRun(r1, r2);

        latch.await(10, TimeUnit.SECONDS);

        assertThat(atomicSubscription).hasValue(a);
        assertThat(q.get()).isEqualTo(1);
        assertThat(r.get()).isEqualTo(0);

    }

    @Test
    public void testThatTheCancelledSubscriptionCanBeCancelled() {
        Subscriptions.CANCELLED.cancel();
        Subscriptions.CANCELLED.cancel();
    }

    @Test
    public void testSetIfEmpty() {
        AtomicReference<Subscription> container = new AtomicReference<>();
        Subscription sub1 = mock(Subscription.class);
        Subscription sub2 = mock(Subscription.class);

        assertThat(Subscriptions.setIfEmpty(container, sub1)).isTrue();
        verify(sub1, never()).cancel();

        assertThat(Subscriptions.setIfEmpty(container, sub2)).isFalse();
        verify(sub1, never()).cancel();
        verify(sub2, times(1)).cancel();
    }

    @Test
    public void testSetIfEmptyAndRequest() {
        AtomicReference<Subscription> container = new AtomicReference<>();
        AtomicLong requests = new AtomicLong();
        Subscription sub1 = mock(Subscription.class);
        Subscription sub2 = mock(Subscription.class);

        assertThat(Subscriptions.setIfEmptyAndRequest(container, requests, sub1)).isTrue();
        verify(sub1, never()).cancel();
        verify(sub1, never()).request(anyLong());

        requests.set(2);
        assertThat(Subscriptions.setIfEmptyAndRequest(container, requests, sub2)).isFalse();
        verify(sub1, never()).cancel();
        verify(sub2, times(1)).cancel();
        verify(sub1, never()).request(anyLong());
        assertThat(requests).hasValue(2);

        container.set(null);
        requests.set(30);

        assertThat(Subscriptions.setIfEmptyAndRequest(container, requests, sub1)).isTrue();
        verify(sub1, never()).cancel();
        verify(sub1, times(1)).request(30);
        assertThat(requests).hasValue(0);
    }

    @Test
    public void testThatSetIfEmptyAndRequestCannotHandleNegativeRequests() {
        AtomicReference<Subscription> container = new AtomicReference<>();
        AtomicLong requests = new AtomicLong(-23);
        Subscription sub = mock(Subscription.class);

        assertThatThrownBy(() -> Subscriptions.setIfEmptyAndRequest(container, requests, sub))
                .isInstanceOf(IllegalArgumentException.class);

        verify(sub, never()).request(anyLong());
        verify(sub, never()).cancel();
    }

    @Test
    public void testThatSetIfEmptyAndRequestCanHandle0Requests() {
        AtomicReference<Subscription> container = new AtomicReference<>();
        AtomicLong requests = new AtomicLong(0);
        Subscription sub = mock(Subscription.class);

        Subscriptions.setIfEmptyAndRequest(container, requests, sub);

        verify(sub, never()).request(anyLong());
        verify(sub, never()).cancel();
        assertThat(requests).hasValue(0);
        assertThat(container).hasValue(sub);
    }

    @Test
    public void testCancelledSubscriber() {
        Subscriptions.CancelledSubscriber<Integer> subscriber = new Subscriptions.CancelledSubscriber<>();
        Subscription subscription = mock(Subscription.class);
        subscriber.onSubscribe(subscription);
        verify(subscription).cancel();
        verify(subscription, never()).request(anyLong());

        // Verify that other method are noop
        subscriber.onNext(1);
        subscriber.onComplete();
        subscriber.onError(new Exception("boom"));

        assertThatThrownBy(() -> subscriber.onSubscribe(null))
                .isInstanceOf(NullPointerException.class);
    }

    private void shuffleAndRun(Runnable r1, Runnable r2) {
        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);
        runnables.forEach(runnable -> new Thread(runnable).start());
    }

}
