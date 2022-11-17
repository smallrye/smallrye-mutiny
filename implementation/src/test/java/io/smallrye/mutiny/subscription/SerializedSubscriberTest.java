package io.smallrye.mutiny.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.test.Mocks;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ_WRITE)
public class SerializedSubscriberTest {

    Subscriber<Integer> subscriber;

    @BeforeEach
    public void before() {
        subscriber = Mocks.subscriber(Long.MAX_VALUE);
    }

    @AfterEach
    public void clean() {
        Infrastructure.resetDroppedExceptionHandler();
    }

    private <T> Subscriber<T> serialized(Subscriber<T> subscriber) {
        return new SerializedSubscriber<>(subscriber);
    }

    @Test
    public void testNormalSingleThreadedEmission() {
        SingleThreadedPublisher publisher = new SingleThreadedPublisher(1, 2, 3);
        Subscriber<Integer> serialized = serialized(subscriber);

        publisher.subscribe(serialized);
        publisher.await();

        verify(subscriber, times(1)).onNext(1);
        verify(subscriber, times(1)).onNext(2);
        verify(subscriber, times(1)).onNext(3);
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void testNormalMultiThreadedEmission() {
        MultiThreadedPublisher publisher = new MultiThreadedPublisher(1, 2, 3);
        BusySubscriber busy = new BusySubscriber();
        Subscriber<Integer> serialized = serialized(busy);

        publisher.subscribe(serialized);
        publisher.await();

        assertThat(busy.onNextCount).hasValue(3);
        assertThat(busy.onError).isFalse();
        assertThat(busy.onComplete).isTrue();

        // we can have concurrency, but the onNext execution should be single threaded
        assertThat(publisher.maxConcurrentThreads.get()).isGreaterThan(1);
        assertThat(busy.maxConcurrentThreads.get()).isEqualTo(1);
    }

    @Test
    public void testMultiThreadedEmissionWithFailureAtTheEnd() throws InterruptedException {
        MultiThreadedPublisher publisher = new MultiThreadedPublisher(1, 2, 3, -1);

        BusySubscriber busy = new BusySubscriber();
        Subscriber<Integer> serialized = serialized(busy);

        publisher.subscribe(serialized);
        publisher.await();
        busy.terminalEvent.await();

        // we can't know how many onNext calls will occur since they each run on a separate thread
        // that depends on thread scheduling so 0, 1, 2 and 3 are all valid options
        assertThat(busy.onNextCount.get()).isLessThan(4);
        assertThat(busy.onError).isTrue();
        assertThat(busy.onComplete).isFalse();

        // we can have concurrency, but the onNext execution should be single threaded
        assertThat(publisher.maxConcurrentThreads.get()).isGreaterThan(1);
        assertThat(busy.maxConcurrentThreads.get()).isEqualTo(1);
    }

    @RepeatedTest(10)
    public void testMultiThreadedEmissionWithFailureInTheMiddleOfTheStream() throws InterruptedException {
        MultiThreadedPublisher publisher = new MultiThreadedPublisher(1, 2, 3, -1, 4, 5, 6, 7, 8, 9);
        BusySubscriber busy = new BusySubscriber();
        Subscriber<Integer> serialized = serialized(busy);

        publisher.subscribe(serialized);
        publisher.await();
        busy.terminalEvent.await();

        assertThat(busy.onNextCount.get()).isLessThan(9);
        assertThat(busy.onError).isTrue();
        assertThat(busy.onComplete).isFalse();

        // we can have concurrency, but the onNext execution should be single threaded
        assertThat(publisher.maxConcurrentThreads.get()).isGreaterThan(1);
        assertThat(busy.maxConcurrentThreads.get()).isEqualTo(1);
    }

    /**
     * A non-realistic use case that tries to expose thread-safety issues by throwing lots of out-of-order
     * events on many threads.
     */
    @Test
    public void runOutOfOrderConcurrencyTest() {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        Infrastructure.setDroppedExceptionHandler(failures::add);

        try {
            ConcurrentSubscriber sub = new ConcurrentSubscriber();
            Subscriber<String> serialized = serialized(new SafeSubscriber<>(sub));

            Future<?> f1 = executor.submit(new OnNextThread(serialized, 12000));
            Future<?> f2 = executor.submit(new OnNextThread(serialized, 5000));
            Future<?> f3 = executor.submit(new OnNextThread(serialized, 75000));
            Future<?> f4 = executor.submit(new OnNextThread(serialized, 13500));
            Future<?> f5 = executor.submit(new OnNextThread(serialized, 22000));
            Future<?> f6 = executor.submit(new OnNextThread(serialized, 15000));
            Future<?> f7 = executor.submit(new OnNextThread(serialized, 7500));
            Future<?> f8 = executor.submit(new OnNextThread(serialized, 23500));
            Future<?> f10 = executor
                    .submit(new CompletionThread(serialized, TestConcurrencySubscriberEvent.onComplete, f1, f2, f3,
                            f4));

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // ignore
            }
            Future<?> f11 = executor
                    .submit(new CompletionThread(serialized, TestConcurrencySubscriberEvent.onComplete, f4, f6, f7));
            Future<?> f12 = executor
                    .submit(new CompletionThread(serialized, TestConcurrencySubscriberEvent.onComplete, f4, f6, f7));
            Future<?> f13 = executor
                    .submit(new CompletionThread(serialized, TestConcurrencySubscriberEvent.onComplete, f4, f6, f7));
            Future<?> f14 = executor
                    .submit(new CompletionThread(serialized, TestConcurrencySubscriberEvent.onComplete, f4, f6, f7));
            // // the next 4 onError events should wait on same as f10
            Future<?> f15 = executor
                    .submit(new CompletionThread(serialized, TestConcurrencySubscriberEvent.onError, f1, f2, f3, f4));
            Future<?> f16 = executor
                    .submit(new CompletionThread(serialized, TestConcurrencySubscriberEvent.onError, f1, f2, f3, f4));
            Future<?> f17 = executor
                    .submit(new CompletionThread(serialized, TestConcurrencySubscriberEvent.onError, f1, f2, f3, f4));
            Future<?> f18 = executor
                    .submit(new CompletionThread(serialized, TestConcurrencySubscriberEvent.onError, f1, f2, f3, f4));

            waitOnThreads(f1, f2, f3, f4, f5, f6, f7, f8, f10, f11, f12, f13, f14, f15, f16, f17, f18);
            @SuppressWarnings("unused")
            int numNextEvents = sub.assertEvents(null);

            assertThat(failures).allSatisfy(t -> assertThat(t).isInstanceOf(RuntimeException.class));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testConcurrency() {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        try {
            ConcurrentSubscriber sub = new ConcurrentSubscriber();
            Subscriber<String> serialized = serialized(new SafeSubscriber<>(sub));
            serialized.onSubscribe(mock(Subscription.class));

            Future<?> f1 = executor.submit(new OnNextThread(serialized, 12000));
            Future<?> f2 = executor.submit(new OnNextThread(serialized, 5000));
            Future<?> f3 = executor.submit(new OnNextThread(serialized, 75000));
            Future<?> f4 = executor.submit(new OnNextThread(serialized, 13500));
            Future<?> f5 = executor.submit(new OnNextThread(serialized, 22000));
            Future<?> f6 = executor.submit(new OnNextThread(serialized, 15000));
            Future<?> f7 = executor.submit(new OnNextThread(serialized, 7500));
            Future<?> f8 = executor.submit(new OnNextThread(serialized, 23500));

            Future<?> f10 = executor
                    .submit(new CompletionThread(serialized, TestConcurrencySubscriberEvent.onComplete, f1, f2, f3, f4,
                            f5, f6, f7, f8));
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // ignore
            }

            waitOnThreads(f1, f2, f3, f4, f5, f6, f7, f8, f10);
            int numNextEvents = sub.assertEvents(null);
            assertThat(numNextEvents).isEqualTo(12000 + 5000 + 75000 + 13500 + 22000 + 15000 + 7500 + 23500);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testOnFailureReentry() {
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        Infrastructure.setDroppedExceptionHandler(failures::add);

        final AtomicReference<Subscriber<Integer>> reference = new AtomicReference<>();

        AssertSubscriber<Integer> subscriber = new AssertSubscriber<Integer>(Long.MAX_VALUE) {
            @Override
            public void onNext(Integer v) {
                reference.get().onError(new TestException("boom-1"));
                reference.get().onError(new TestException("boom-2"));
                super.onNext(v);
            }
        };
        SerializedSubscriber<Integer> serialized = new SerializedSubscriber<>(subscriber);
        serialized.onSubscribe(mock(Subscription.class));
        reference.set(serialized);

        serialized.onNext(1);

        subscriber
                .assertItems(1)
                .assertFailedWith(TestException.class, "boom-1");

        assertThat(failures).hasSize(1).allSatisfy(f -> assertThat(f).isInstanceOf(TestException.class)
                .hasMessageContaining("boom-2"));
    }

    @Test
    public void testOnCompleteReentry() {
        final AtomicReference<Subscriber<Integer>> reference = new AtomicReference<>();

        AssertSubscriber<Integer> subscriber = new AssertSubscriber<Integer>(Long.MAX_VALUE) {
            @Override
            public void onNext(Integer v) {
                reference.get().onComplete();
                reference.get().onComplete();
                super.onNext(v);
            }
        };
        SerializedSubscriber<Integer> serialized = new SerializedSubscriber<>(subscriber);
        serialized.onSubscribe(mock(Subscription.class));
        reference.set(serialized);

        subscriber.onNext(1);

        subscriber
                .assertItems(1)
                .assertCompleted();
    }

    @Test
    public void testCancellation() {
        AssertSubscriber<Integer> subscriber = new AssertSubscriber<>(Long.MAX_VALUE);
        SerializedSubscriber<Integer> serialized = new SerializedSubscriber<>(subscriber);

        Subscription subscription = mock(Subscription.class);
        serialized.onSubscribe(subscription);

        subscriber.cancel();
        verify(subscription).cancel();
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @RepeatedTest(10)
    public void testOnCompleteRace() {
        AssertSubscriber<Integer> subscriber = new AssertSubscriber<>(Long.MAX_VALUE);
        SerializedSubscriber<Integer> serialized = new SerializedSubscriber<>(subscriber);
        Subscription subscription = mock(Subscription.class);
        serialized.onSubscribe(subscription);

        CountDownLatch start = new CountDownLatch(2);
        Runnable runnable = () -> {
            start.countDown();
            await(start);
            serialized.onCompletion();
        };
        Arrays.asList(runnable, runnable).forEach(r -> new Thread(r).start());

        subscriber.awaitCompletion();
    }

    @RepeatedTest(10)
    public void testOnNextOnCompleteRace() {
        AssertSubscriber<Integer> subscriber = new AssertSubscriber<>(Long.MAX_VALUE);
        SerializedSubscriber<Integer> serialized = new SerializedSubscriber<>(subscriber);
        Subscription subscription = mock(Subscription.class);
        serialized.onSubscribe(subscription);

        CountDownLatch start = new CountDownLatch(2);

        Runnable r1 = () -> {
            start.countDown();
            await(start);
            serialized.onComplete();
        };

        Runnable r2 = () -> {
            start.countDown();
            await(start);
            serialized.onNext(1);
        };

        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);
        runnables.forEach(r -> new Thread(r).start());

        subscriber.awaitCompletion();
        assertThat(subscriber.getItems()).hasSizeBetween(0, 1);
    }

    @RepeatedTest(10)
    public void testOnNextOnErrorRace() {
        AssertSubscriber<Integer> subscriber = new AssertSubscriber<>(Long.MAX_VALUE);
        SerializedSubscriber<Integer> serialized = new SerializedSubscriber<>(subscriber);
        Subscription subscription = mock(Subscription.class);
        serialized.onSubscribe(subscription);

        CountDownLatch start = new CountDownLatch(2);

        Runnable r1 = () -> {
            start.countDown();
            await(start);
            serialized.onError(new TestException("boom"));
        };

        Runnable r2 = () -> {
            start.countDown();
            await(start);
            serialized.onNext(1);
        };

        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);
        runnables.forEach(r -> new Thread(r).start());

        subscriber.awaitFailure().assertFailedWith(TestException.class, "boom");
        assertThat(subscriber.getItems()).hasSizeBetween(0, 1);
    }

    @RepeatedTest(10)
    public void testOnCompleteOnErrorRace() {
        AssertSubscriber<Integer> subscriber = new AssertSubscriber<>(Long.MAX_VALUE);
        SerializedSubscriber<Integer> serialized = new SerializedSubscriber<>(subscriber);
        Subscription subscription = mock(Subscription.class);
        serialized.onSubscribe(subscription);

        CountDownLatch start = new CountDownLatch(2);

        Runnable r1 = () -> {
            start.countDown();
            await(start);
            serialized.onError(new TestException("boom"));
        };

        Runnable r2 = () -> {
            start.countDown();
            await(start);
            serialized.onComplete();
        };

        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);
        runnables.forEach(r -> new Thread(r).start());

        Awaitility.await().until(() -> subscriber.hasCompleted() || subscriber.getFailure() != null);
        if (subscriber.hasCompleted()) {
            subscriber.assertCompleted().assertHasNotReceivedAnyItem();
        } else {
            subscriber.assertFailedWith(TestException.class, "boom");
        }
    }

    @Test
    public void testThatTheSerializedSubscriberAcceptOnlyOnSubscription() {
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        Infrastructure.setDroppedExceptionHandler(failures::add);

        AssertSubscriber<Integer> subscriber = new AssertSubscriber<>(Long.MAX_VALUE);
        SerializedSubscriber<Integer> serialized = new SerializedSubscriber<>(subscriber);

        serialized.onSubscribe(mock(Subscription.class));

        Subscription another = mock(Subscription.class);
        serialized.onSubscribe(another);

        verify(another).cancel();
        assertThat(failures).hasSize(1)
                .allSatisfy(i -> assertThat(i).isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Subscription already set"));
    }

    /**
     * Publish elements on a single thread.
     */
    static class SingleThreadedPublisher implements Publisher<Integer> {

        List<Integer> values;
        Thread thread;

        SingleThreadedPublisher(final Integer... values) {
            this.values = Arrays.asList(values);
        }

        @Override
        public void subscribe(final Subscriber<? super Integer> subscriber) {
            subscriber.onSubscribe(mock(Subscription.class));
            thread = new Thread(() -> {
                try {
                    for (int i : values) {
                        subscriber.onNext(i);
                    }
                    subscriber.onComplete();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
        }

        public void await() {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Publish elements on a multiple threads.
     */
    static class MultiThreadedPublisher implements Publisher<Integer> {

        List<Integer> values;
        Thread t;
        AtomicInteger threadsRunning = new AtomicInteger();
        AtomicInteger maxConcurrentThreads = new AtomicInteger();
        ExecutorService threadPool;

        MultiThreadedPublisher(Integer... values) {
            this.values = Arrays.asList(values);
            this.threadPool = Executors.newCachedThreadPool();
        }

        @Override
        public void subscribe(final Subscriber<? super Integer> subscriber) {
            subscriber.onSubscribe(mock(Subscription.class));
            final NullPointerException npe = new NullPointerException();
            t = new Thread(() -> {
                try {
                    int j = 0;
                    for (final int i : values) {
                        final int fj = ++j;
                        threadPool.execute(() -> {
                            threadsRunning.incrementAndGet();
                            try {
                                if (i == -1) {
                                    // force an error on -1
                                    throw npe;
                                } else {
                                    // allow the exception to queue up
                                    int sleep = (fj % 3) * 10;
                                    if (sleep != 0) {
                                        Thread.sleep(sleep);
                                    }
                                }
                                subscriber.onNext(i);
                                // capture 'maxThreads'
                                int concurrentThreads = threadsRunning.get();
                                int maxThreads = maxConcurrentThreads.get();
                                if (concurrentThreads > maxThreads) {
                                    maxConcurrentThreads.compareAndSet(maxThreads, concurrentThreads);
                                }
                            } catch (Throwable e) {
                                subscriber.onError(e);
                            } finally {
                                threadsRunning.decrementAndGet();
                            }
                        });
                    }
                    // we are done spawning threads
                    threadPool.shutdown();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }

                // wait until all threads are done, then mark it as COMPLETED
                try {
                    // wait for all the threads to finish
                    threadPool.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                subscriber.onComplete();
            });
            t.start();
        }

        public void await() {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class BusySubscriber implements Subscriber<Integer> {
        volatile boolean onComplete;
        volatile boolean onError;
        AtomicInteger onNextCount = new AtomicInteger();
        AtomicInteger threadsRunning = new AtomicInteger();
        AtomicInteger maxConcurrentThreads = new AtomicInteger();
        final CountDownLatch terminalEvent = new CountDownLatch(1);

        @Override
        public void onComplete() {
            threadsRunning.incrementAndGet();
            try {
                onComplete = true;
            } finally {
                captureMaxThreads();
                threadsRunning.decrementAndGet();
                terminalEvent.countDown();
            }
        }

        @Override
        public void onError(Throwable e) {
            threadsRunning.incrementAndGet();
            try {
                onError = true;
            } finally {
                captureMaxThreads();
                threadsRunning.decrementAndGet();
                terminalEvent.countDown();
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(Integer item) {
            threadsRunning.incrementAndGet();
            try {
                onNextCount.incrementAndGet();
                try {
                    // simulate doing something computational
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } finally {
                // capture 'maxThreads'
                captureMaxThreads();
                threadsRunning.decrementAndGet();
            }
        }

        protected void captureMaxThreads() {
            int concurrentThreads = threadsRunning.get();
            int maxThreads = maxConcurrentThreads.get();
            if (concurrentThreads > maxThreads) {
                maxConcurrentThreads.compareAndSet(maxThreads, concurrentThreads);
                if (concurrentThreads > 1) {
                    new RuntimeException("should not be greater than 1").printStackTrace();
                }
            }
        }

    }

    /**
     * A thread that just pass data to onNext.
     */
    public static class OnNextThread implements Runnable {

        private final CountDownLatch latch;
        private final Subscriber<String> subscriber;
        private final int itemToSend;
        final AtomicInteger produced;
        private final CountDownLatch running;

        OnNextThread(Subscriber<String> subscriber, int itemToSend, AtomicInteger produced) {
            this(subscriber, itemToSend, produced, null, null);
        }

        OnNextThread(Subscriber<String> subscriber, int itemToSend, AtomicInteger produced, CountDownLatch latch,
                CountDownLatch running) {
            this.subscriber = subscriber;
            this.itemToSend = itemToSend;
            this.produced = produced;
            this.latch = latch;
            this.running = running;
        }

        OnNextThread(Subscriber<String> subscriber, int itemToSend) {
            this(subscriber, itemToSend, new AtomicInteger());
        }

        @Override
        public void run() {
            if (running != null) {
                running.countDown();
            }
            for (int i = 0; i < itemToSend; i++) {
                subscriber.onNext(Thread.currentThread().getId() + "-" + i);
                if (latch != null) {
                    latch.countDown();
                }
                produced.incrementAndGet();
            }
        }
    }

    /**
     * A thread that will call onError or onNext.
     */
    public static class CompletionThread implements Runnable {

        private final Subscriber<String> subscriber;
        private final TestConcurrencySubscriberEvent event;
        private final Future<?>[] waitOnThese;

        CompletionThread(Subscriber<String> Subscriber, TestConcurrencySubscriberEvent event,
                Future<?>... waitOnThese) {
            this.subscriber = Subscriber;
            this.event = event;
            this.waitOnThese = waitOnThese;
        }

        @Override
        public void run() {
            /* if we have 'waitOnThese' futures, we'll wait on them before proceeding */
            if (waitOnThese != null) {
                for (Future<?> f : waitOnThese) {
                    try {
                        f.get();
                    } catch (Throwable e) {
                        System.err.println("Error while waiting on future in CompletionThread");
                    }
                }
            }

            /* send the event */
            if (event == TestConcurrencySubscriberEvent.onError) {
                subscriber.onError(new RuntimeException("mocked exception"));
            } else if (event == TestConcurrencySubscriberEvent.onComplete) {
                subscriber.onComplete();

            } else {
                throw new IllegalArgumentException("Expecting either onError or onComplete");
            }
        }
    }

    enum TestConcurrencySubscriberEvent {
        onComplete,
        onError,
        onNext
    }

    private static class ConcurrentSubscriber implements Subscriber<String> {

        /**
         * Used to store the order and number of events received.
         */
        private final LinkedBlockingQueue<TestConcurrencySubscriberEvent> events = new LinkedBlockingQueue<>();
        private final int waitTime;

        @SuppressWarnings("unused")
        ConcurrentSubscriber(int waitTimeInNext) {
            this.waitTime = waitTimeInNext;
        }

        ConcurrentSubscriber() {
            this.waitTime = 0;
        }

        @Override
        public void onComplete() {
            events.add(TestConcurrencySubscriberEvent.onComplete);
        }

        @Override
        public void onError(Throwable e) {
            events.add(TestConcurrencySubscriberEvent.onError);
        }

        @Override
        public void onNext(String item) {
            events.add(TestConcurrencySubscriberEvent.onNext);
            // do some artificial work to make the thread scheduling/timing vary
            int s = 0;
            for (int i = 0; i < 20; i++) {
                s += s * i;
            }

            if (waitTime > 0) {
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        /**
         * Assert the order of events is correct and return the number of onNext executions.
         *
         * @param expectedEndingEvent the last event
         * @return int count of onNext calls
         * @throws IllegalStateException If order of events was invalid.
         */
        public int assertEvents(TestConcurrencySubscriberEvent expectedEndingEvent) throws IllegalStateException {
            int nextCount = 0;
            boolean finished = false;
            for (TestConcurrencySubscriberEvent e : events) {
                if (e == TestConcurrencySubscriberEvent.onNext) {
                    if (finished) {
                        // already finished, we shouldn't get this again
                        throw new IllegalStateException("Received onNext but we're already finished.");
                    }
                    nextCount++;
                } else if (e == TestConcurrencySubscriberEvent.onError) {
                    if (finished) {
                        // already finished, we shouldn't get this again
                        throw new IllegalStateException("Received onError but we're already finished.");
                    }
                    if (expectedEndingEvent != null && TestConcurrencySubscriberEvent.onError != expectedEndingEvent) {
                        throw new IllegalStateException(
                                "Received onError ending event but expected " + expectedEndingEvent);
                    }
                    finished = true;
                } else if (e == TestConcurrencySubscriberEvent.onComplete) {
                    if (finished) {
                        // already finished, we shouldn't get this again
                        throw new IllegalStateException("Received onComplete but we're already finished.");
                    }
                    if (expectedEndingEvent != null
                            && TestConcurrencySubscriberEvent.onComplete != expectedEndingEvent) {
                        throw new IllegalStateException(
                                "Received onComplete ending event but expected " + expectedEndingEvent);
                    }
                    finished = true;
                }
            }

            return nextCount;
        }

    }

    private static void waitOnThreads(Future<?>... futures) {
        for (Future<?> f : futures) {
            try {
                f.get(20, TimeUnit.SECONDS);
            } catch (Throwable e) {
                throw new RuntimeException("Failed while wiating", e);
            }
        }
    }

}
