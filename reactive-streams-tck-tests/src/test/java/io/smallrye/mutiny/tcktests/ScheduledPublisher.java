package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.testng.Assert;

/**
 * A publisher that publishes one element 100ms after being requested,
 * and then completes 100ms later. It also uses activePublishers to ensure
 * that it is the only publisher that is subscribed to at any one time.
 */
@SuppressWarnings("ReactiveStreamsPublisherImplementation")
class ScheduledPublisher implements Publisher<Integer> {
    private final int id;
    private final AtomicBoolean published = new AtomicBoolean(false);
    private final AtomicInteger activePublishers;
    private final Supplier<ScheduledExecutorService> supplier;

    ScheduledPublisher(int id, AtomicInteger activePublishers, Supplier<ScheduledExecutorService> supplier) {
        this.id = id;
        this.activePublishers = activePublishers;
        this.supplier = supplier;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
        Assert.assertEquals(activePublishers.incrementAndGet(), 1);
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                if (published.compareAndSet(false, true)) {
                    getExecutorService().schedule(() -> {
                        subscriber.onNext(id);
                        getExecutorService().schedule(() -> {
                            activePublishers.decrementAndGet();
                            subscriber.onComplete();
                        }, 100, TimeUnit.MILLISECONDS);
                    }, 100, TimeUnit.MILLISECONDS);
                }
            }

            @Override
            public void cancel() {
            }
        });
    }

    private ScheduledExecutorService getExecutorService() {
        return supplier.get();
    }
}
