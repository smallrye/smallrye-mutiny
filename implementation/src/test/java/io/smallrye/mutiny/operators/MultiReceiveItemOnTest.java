package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiReceiveItemOnTest {

    private ExecutorService executor;

    @BeforeEach
    public void init() {
        executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
            final AtomicInteger count = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("test-" + count.incrementAndGet());
                return thread;
            }
        });
    }

    @AfterEach
    public void cleanup() {
        executor.shutdownNow();
    }

    @Test
    public void testThatExecutorCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().item(1).emitOn(null));
    }

    @Test
    public void testThatRunSubscriptionOnExecutorCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().item(1).runSubscriptionOn(null));
    }

    @Test
    public void testThatItemsAreDispatchedOnTheRightThread() {
        Set<String> itemThread = ConcurrentHashMap.newKeySet();
        Set<String> completionThread = ConcurrentHashMap.newKeySet();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4)
                .emitOn(executor)
                .onItem().invoke(i -> itemThread.add(Thread.currentThread().getName()))
                .onCompletion().invoke(() -> completionThread.add(Thread.currentThread().getName()))
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .awaitCompletion();

        await().until(() -> subscriber.getItems().size() == 4);
        assertThat(itemThread).allSatisfy(s -> assertThat(s).startsWith("test-"));
        assertThat(completionThread).hasSizeGreaterThanOrEqualTo(1).allSatisfy(s -> assertThat(s).startsWith("test-"));
    }

    @Test
    public void testThatFailureAreDispatchedOnExecutor() {
        Set<String> itemThread = new LinkedHashSet<>();
        Set<String> failureThread = new LinkedHashSet<>();
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .emitOn(executor)
                .onItem().invoke(i -> itemThread.add(Thread.currentThread().getName()))
                .onFailure().invoke(f -> failureThread.add(Thread.currentThread().getName()))
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .awaitFailure()
                .assertFailedWith(IOException.class, "boom");

        assertThat(itemThread).isEmpty();
        assertThat(failureThread).hasSizeGreaterThanOrEqualTo(1).allSatisfy(s -> assertThat(s).startsWith("test-"));
    }

    @Test
    public void testWithImmediate() {
        Multi.createFrom().items(1, 2, 3, 4)
                .emitOn(Runnable::run)
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .awaitCompletion()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testWithLargeNumberOfItems() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().range(0, 100_000)
                .emitOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .awaitCompletion();

        assertThat(subscriber.getItems()).hasSize(100_000);
        int current = -1;
        for (Integer i : subscriber.getItems()) {
            assertThat(i).isGreaterThan(current);
            current = i;
        }
    }

    @Test
    public void testRunSubscriptionOn() {
        Multi.createFrom().items(1, 2, 3, 4)
                .runSubscriptionOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .awaitCompletion()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testRunSubscriptionOnWithInvalidRequestCancelsUpstream() {
        AtomicBoolean cancelled = new AtomicBoolean();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(emitter -> {
        })
                .onCancellation().invoke(() -> cancelled.set(true))
                .runSubscriptionOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create(0));

        subscriber.awaitSubscription();
        subscriber.request(-1L);
        subscriber.awaitFailure()
                .assertFailedWith(IllegalArgumentException.class, "Invalid number of request, must be greater than 0");
        assertThat(cancelled).isTrue();
    }

    @Test
    public void testRunSubscriptionOnWithRejectedRequestCancelsUpstream() {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicInteger executions = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(emitter -> {
        })
                .onCancellation().invoke(() -> cancelled.set(true))
                .runSubscriptionOn(task -> {
                    if (executions.getAndIncrement() == 0) {
                        task.run();
                    } else {
                        throw new RejectedExecutionException("rejected");
                    }
                })
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(1);
        subscriber.assertFailedWith(RejectedExecutionException.class, "rejected");
        assertThat(cancelled).isTrue();
    }

    @Test
    public void testRunSubscriptionOnWithRejectedSubscriptionSignalsOnSubscribeFirst() {
        List<String> signals = new CopyOnWriteArrayList<>();

        Multi.createFrom().items(1)
                .runSubscriptionOn(command -> {
                    throw new RejectedExecutionException("rejected");
                })
                .subscribe().withSubscriber(new MultiSubscriber<Integer>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        signals.add("subscribe");
                    }

                    @Override
                    public void onItem(Integer item) {
                        signals.add("item");
                    }

                    @Override
                    public void onFailure(Throwable failure) {
                        signals.add("failure:" + failure.getClass().getSimpleName());
                    }

                    @Override
                    public void onCompletion() {
                        signals.add("completion");
                    }
                });

        assertThat(signals).containsExactly("subscribe", "failure:RejectedExecutionException");
    }

}
