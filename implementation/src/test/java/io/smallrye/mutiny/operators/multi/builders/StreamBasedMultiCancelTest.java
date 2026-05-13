package io.smallrye.mutiny.operators.multi.builders;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;

public class StreamBasedMultiCancelTest {

    @Test
    void cancelFromOnNextPreventsSubsequentDelivery() {
        AtomicInteger counter = new AtomicInteger();
        Stream<Integer> stream = Stream.generate(counter::getAndIncrement).limit(1_000_000);

        AtomicReference<Flow.Subscription> subRef = new AtomicReference<>();
        AtomicInteger itemCount = new AtomicInteger();

        Multi.createFrom().items(() -> stream)
                .subscribe().withSubscriber(new Flow.Subscriber<>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subRef.set(subscription);
                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(Integer item) {
                        itemCount.incrementAndGet();
                        if (item == 0) {
                            subRef.get().cancel();
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });

        assertThat(itemCount.get())
                .isEqualTo(1);
    }

    @Test
    void cancelDuringIteratorFetchPreventsDelivery() throws InterruptedException {
        CountDownLatch inSupplier = new CountDownLatch(1);
        CountDownLatch proceedSupplier = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger();

        Stream<Integer> stream = Stream.generate(() -> {
            int val = counter.getAndIncrement();
            if (val == 2) {
                inSupplier.countDown();
                try {
                    proceedSupplier.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return val;
        }).limit(100);

        AtomicInteger itemCount = new AtomicInteger();
        AtomicReference<Flow.Subscription> subRef = new AtomicReference<>();
        CountDownLatch subscribed = new CountDownLatch(1);

        Multi.createFrom().items(() -> stream)
                .subscribe().withSubscriber(new Flow.Subscriber<>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subRef.set(subscription);
                        subscribed.countDown();
                    }

                    @Override
                    public void onNext(Integer item) {
                        itemCount.incrementAndGet();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });

        subscribed.await(5, TimeUnit.SECONDS);

        Thread requester = new Thread(() -> subRef.get().request(10), "requester-thread");
        requester.start();

        assertThat(inSupplier.await(5, TimeUnit.SECONDS)).isTrue();

        subRef.get().cancel();
        proceedSupplier.countDown();

        requester.join(5000);

        assertThat(itemCount.get()).isEqualTo(2);
    }
}
