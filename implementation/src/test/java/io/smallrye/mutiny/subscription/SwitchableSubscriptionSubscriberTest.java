package io.smallrye.mutiny.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Subscription;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import io.smallrye.mutiny.test.AssertSubscriber;

public class SwitchableSubscriptionSubscriberTest {

    private AssertSubscriber<Integer> subscriber;
    private MultiSubscriber<Integer> downstream;
    private SwitchableSubscriptionSubscriber<Integer> switchable;

    @BeforeTest
    public void init() {
        subscriber = AssertSubscriber.create(10);
        downstream = new MultiSubscriber<Integer>() {
            @Override
            public void onItem(Integer item) {
                subscriber.onNext(item);
            }

            @Override
            public void onFailure(Throwable failure) {
                subscriber.onError(failure);
            }

            @Override
            public void onCompletion() {
                subscriber.onComplete();
            }

            @Override
            public void onSubscribe(Subscription subscription) {
                subscriber.onSubscribe(subscription);
            }
        };
    }

    @Test
    public void testInvalidRequests() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };
        switchable.request(-1);
        subscriber.assertHasFailedWith(IllegalArgumentException.class, "");
    }

    @Test
    public void testCancellationIfTheDownstreamSubscriptionIsCancelled() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };
        downstream.onSubscribe(switchable);
        subscriber.assertSubscribed();

        subscriber.cancel();
        Subscription second = mock(Subscription.class);
        switchable.onSubscribe(second);
        verify(second, times(1)).cancel();
        assertThat(switchable.isCancelled()).isTrue();
    }

    @Test
    public void testCancellationOnSwitchEnabled() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }

            @Override
            protected boolean cancelUpstreamOnSwitch() {
                return true;
            }
        };
        Subscription first = mock(Subscription.class);
        Subscription second = mock(Subscription.class);
        switchable.request(5);

        switchable.onSubscribe(first);
        switchable.onSubscribe(second);
        verify(first, times(1)).cancel();
        verify(first, times(1)).request(5);
        verify(second, times(0)).cancel();
        verify(second, times(1)).request(5);
    }

    @Test
    public void testCancellationOnSwitchDisabled() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };
        Subscription first = mock(Subscription.class);
        Subscription second = mock(Subscription.class);
        switchable.request(5);

        switchable.onSubscribe(first);
        switchable.onSubscribe(second);
        verify(first, times(0)).cancel();
        verify(first, times(1)).request(5);
        verify(second, times(0)).cancel();
        verify(second, times(1)).request(5);
    }

    @Test
    public void testRequests() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };
        Subscription first = mock(Subscription.class);
        Subscription second = mock(Subscription.class);
        Subscription third = mock(Subscription.class);
        switchable.request(5);
        switchable.onSubscribe(first);

        // Emit 5 - remaining 0
        switchable.emitted(5);
        switchable.onSubscribe(second);
        verify(first, times(0)).cancel();
        verify(first, times(1)).request(5);
        verify(second, times(0)).cancel();
        verify(second, times(0)).request(0);

        switchable.request(10);
        verify(second, times(1)).request(10);
        // Emit 2 - remaining 8
        switchable.emitted(2);
        switchable.onSubscribe(third);
        verify(third, times(1)).request(8);
    }

    @Test(invocationCount = 100)
    public void testRaceOnSwitch() throws InterruptedException {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };
        Subscription first = mock(Subscription.class);
        Subscription second = mock(Subscription.class);
        Subscription third = mock(Subscription.class);
        switchable.request(5);

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(3);
        Runnable r1 = () -> {
            try {
                start.await();
                switchable.onSubscribe(first);
                done.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        Runnable r2 = () -> {
            try {
                start.await();
                switchable.onSubscribe(second);
                done.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        Runnable r3 = () -> {
            try {
                start.await();
                switchable.onSubscribe(third);
                done.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        List<Runnable> list = Arrays.asList(r1, r2, r3);
        Collections.shuffle(list);
        for (Runnable r : list) {
            new Thread(r).start();
        }

        start.countDown();
        done.await();

        verify(first, times(0)).cancel();
        verify(second, times(0)).cancel();
        verify(third, times(0)).cancel();
        verify(first, atMost(1)).request(5);
        verify(second, atMost(1)).request(5);
        verify(third, atMost(1)).request(5);
    }

    @Test(invocationCount = 100)
    public void testRaceOnSwitchWithCancellationOnSwitch() throws InterruptedException {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }

            @Override
            protected boolean cancelUpstreamOnSwitch() {
                return true;
            }
        };
        Subscription first = mock(Subscription.class);
        Subscription second = mock(Subscription.class);
        Subscription third = mock(Subscription.class);
        switchable.request(5);

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(3);
        Runnable r1 = () -> {
            try {
                start.await();
                switchable.onSubscribe(first);
                done.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        Runnable r2 = () -> {
            try {
                start.await();
                switchable.onSubscribe(second);
                done.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        Runnable r3 = () -> {
            try {
                start.await();
                switchable.onSubscribe(third);
                done.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        List<Runnable> list = Arrays.asList(r1, r2, r3);
        Collections.shuffle(list);
        for (Runnable r : list) {
            new Thread(r).start();
        }

        start.countDown();
        done.await();

        verify(first, atMost(1)).cancel();
        verify(second, atMost(1)).cancel();
        verify(third, atMost(1)).cancel();
        verify(first, atMost(1)).request(5);
        verify(second, atMost(1)).request(5);
        verify(third, atMost(1)).request(5);
    }

    @Test(invocationCount = 50)
    public void testRaceOnSwitchAndEmitted() throws InterruptedException {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };
        switchable.request(1000);

        int threads = 1000;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Runnable> runnables = new ArrayList<>();
        List<Subscription> subscriptions = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int id = i;
            Subscription sub = mock(Subscription.class);
            Runnable runnable = () -> {
                try {
                    start.await();
                    if (id % 2 == 0) {
                        switchable.emitted(2);
                    } else {
                        switchable.onSubscribe(sub);
                    }
                    done.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };
            subscriptions.add(sub);
            runnables.add(runnable);
        }

        Collections.shuffle(runnables);
        for (Runnable r : runnables) {
            new Thread(r).start();
        }

        start.countDown();
        done.await();

        for (Subscription s : subscriptions) {
            verify(s, times(0)).cancel();
            verify(s, atMost(1)).request(anyLong());
        }
    }

    @Test
    public void testWhenSubscriptionIsMissed() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }

            @Override
            protected boolean cancelUpstreamOnSwitch() {
                return true;
            }
        };

        AtomicInteger wip = switchable.wip;

        wip.getAndIncrement();

        Subscription sub1 = mock(Subscription.class);
        Subscription sub2 = mock(Subscription.class);

        switchable.onSubscribe(sub1);
        switchable.onSubscribe(sub2);

        verify(sub1, times(1)).cancel();
        verify(sub2, times(0)).cancel();
    }

    @Test
    public void testUnboundedRequest() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }

            @Override
            protected boolean cancelUpstreamOnSwitch() {
                return true;
            }
        };

        switchable.request(Long.MAX_VALUE);

        assertThat(switchable.requested).isEqualTo(Long.MAX_VALUE);
        assertThat(switchable.unbounded).isTrue();

        switchable.unbounded = false;
        switchable.request(Long.MAX_VALUE);
        assertThat(switchable.requested).isEqualTo(Long.MAX_VALUE);

        switchable.emitted(1);
        assertThat(switchable.requested).isEqualTo(Long.MAX_VALUE);

        switchable.unbounded = false;

        switchable.emitted(Long.MAX_VALUE);
        assertThat(switchable.requested).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void testCancellationInDrainLoop() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }

            @Override
            protected boolean cancelUpstreamOnSwitch() {
                return true;
            }
        };
        switchable.cancel();

        Subscription sub = mock(Subscription.class);
        switchable.pendingSubscription.set(sub);
        switchable.wip.getAndIncrement();
        switchable.drainLoop();
        verify(sub, times(1)).cancel();
    }

    @Test
    public void testDrainLoopWithUnboundedRequests() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };

        switchable.wip.getAndIncrement();
        switchable.request(Long.MAX_VALUE);
        switchable.drainLoop();

        assertThat(switchable.wip).hasValue(0);
    }

    @Test
    public void testMissedRequests() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };

        switchable.wip.getAndIncrement();
        switchable.requested = 0;
        switchable.missedRequested.set(1);
        switchable.drainLoop();
        assertThat(switchable.requested).isEqualTo(1);
    }

    @Test
    public void testDrainLoopWithMissedRequestsAndItems() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };

        switchable.wip.getAndIncrement();
        switchable.requested = 0;
        switchable.missedRequested.set(Long.MAX_VALUE);
        switchable.missedItems.set(25);
        switchable.drainLoop();

        assertThat(switchable.requested).isEqualTo(Long.MAX_VALUE);

    }

    @Test
    public void testWhenWeEmitMoreThanRequested() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };
        switchable.wip.getAndIncrement();
        switchable.requested = 0;

        switchable.missedRequested.set(1);
        switchable.missedItems.set(2);
        switchable.drainLoop();

        assertThat(switchable.requested).isEqualTo(0);
        assertThat(switchable.missedRequested).hasValue(0);
        assertThat(switchable.missedItems).hasValue(0);
    }

    @Test
    public void testMissedFirstSubscription() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };

        switchable.wip.getAndIncrement();
        Subscription subscription = mock(Subscription.class);
        switchable.pendingSubscription.set(subscription);
        switchable.drainLoop();
        assertThat(subscription).isEqualTo(switchable.currentUpstream.get());
    }

    @Test
    public void testPendingSubscriptionWithoutCancellationOnSwitch() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };

        Subscription subscription1 = mock(Subscription.class);
        Subscription subscription2 = mock(Subscription.class);
        Subscription subscription3 = mock(Subscription.class);

        switchable.onSubscribe(subscription1);
        switchable.wip.getAndIncrement();

        switchable.onSubscribe(subscription2);
        switchable.onSubscribe(subscription3);

        switchable.drainLoop();
        verify(subscription1, times(0)).cancel();
        verify(subscription2, times(0)).cancel();
        verify(subscription3, times(0)).cancel();

        assertThat(subscription3).isEqualTo(switchable.currentUpstream.get());
    }

    @Test
    public void testPendingSubscriptionWithCancellationOnSwitch() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }

            @Override
            protected boolean cancelUpstreamOnSwitch() {
                return true;
            }
        };

        Subscription subscription1 = mock(Subscription.class);
        Subscription subscription2 = mock(Subscription.class);
        Subscription subscription3 = mock(Subscription.class);

        switchable.onSubscribe(subscription1);
        switchable.wip.getAndIncrement();

        switchable.onSubscribe(subscription2);
        switchable.onSubscribe(subscription3);

        switchable.drainLoop();
        verify(subscription1, times(1)).cancel();
        verify(subscription2, times(1)).cancel();
        verify(subscription3, times(0)).cancel();

        assertThat(subscription3).isEqualTo(switchable.currentUpstream.get());
    }

    @Test
    public void testEmittedAndUnbounded() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };

        switchable.request(Long.MAX_VALUE);
        switchable.emitted(100);
        switchable.request(10);

        assertThat(switchable.unbounded).isTrue();
        assertThat(switchable.requested).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void testDrainLoopAfterCancellation() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };
        Subscription subscription = mock(Subscription.class);
        switchable.onSubscribe(subscription);

        switchable.wip.getAndIncrement();
        switchable.cancel();

        verify(subscription, times(0)).cancel();
        switchable.drainLoop();
        verify(subscription, times(1)).cancel();
    }

    @Test
    public void testRequestHappeningDuringWIP() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };
        Subscription subscription = mock(Subscription.class);
        switchable.onSubscribe(subscription);
        switchable.request(10);

        switchable.wip.getAndIncrement();
        switchable.emitted(5);
        switchable.request(3);
        switchable.drainLoop();

        verify(subscription, times(0)).cancel();
        verify(subscription, times(1)).request(3);
    }

    @Test
    public void testRequestAndSwitchHappeningDuringWIP() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }
        };
        Subscription subscription1 = mock(Subscription.class);
        Subscription subscription2 = mock(Subscription.class);
        switchable.onSubscribe(subscription1);
        switchable.request(10);

        switchable.wip.getAndIncrement();
        switchable.requested = 10;
        switchable.missedRequested.set(3);
        switchable.emitted(5);
        switchable.pendingSubscription.set(subscription2);
        switchable.drainLoop();

        verify(subscription1, times(0)).cancel();
        verify(subscription2, times(0)).cancel();
        verify(subscription2, times(1)).request(8);
    }

    @Test
    public void testRequestAndSwitchHappeningDuringWIPWithCancellation() {
        switchable = new SwitchableSubscriptionSubscriber<Integer>(downstream) {
            @Override
            public void onItem(Integer item) {
                downstream.onItem(item);
            }

            @Override
            protected boolean cancelUpstreamOnSwitch() {
                return true;
            }
        };
        Subscription subscription1 = mock(Subscription.class);
        Subscription subscription2 = mock(Subscription.class);
        switchable.onSubscribe(subscription1);
        switchable.request(10);

        switchable.wip.getAndIncrement();
        switchable.requested = 10;
        switchable.missedRequested.set(3);
        switchable.emitted(5);
        switchable.pendingSubscription.set(subscription2);
        switchable.drainLoop();

        verify(subscription1, times(1)).cancel();
        verify(subscription2, times(0)).cancel();
        verify(subscription2, times(1)).request(8);
    }

}
