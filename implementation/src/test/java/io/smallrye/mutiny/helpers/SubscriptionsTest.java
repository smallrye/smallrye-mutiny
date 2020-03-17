package io.smallrye.mutiny.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
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

    @Test(invocationCount = 50)
    public void deferredRequestSubscriptionRace() throws InterruptedException {
        AtomicReference<Subscription> container = new AtomicReference<>();
        Subscription subscription = mock(Subscription.class);
        AtomicLong requested = new AtomicLong();

        CountDownLatch latch = new CountDownLatch(2);
        Runnable r1 = () -> {
            Subscriptions.deferredRequest(container, requested, 20);
            latch.countDown();
        };
        Runnable r2 = () -> {
            container.set(subscription);
            latch.countDown();
        };

        new Thread(r1).start();
        new Thread(r2).start();

        latch.await();
    }

}
