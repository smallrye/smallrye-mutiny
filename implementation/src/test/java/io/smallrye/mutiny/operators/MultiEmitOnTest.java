package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiEmitOnTest {

    private ExecutorService executor;

    @BeforeEach
    public void init() {
        executor = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    public void shutdown() {
        executor.shutdown();
    }

    @RepeatedTest(10)
    public void testWithSequenceOfItems() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber
                .awaitNextItems(2, 2)
                .assertItems(1, 2)
                .awaitNextItems(8, 20)
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @RepeatedTest(10)
    public void testWithSequenceOfItemsAndBufferSize() {
        AtomicInteger requestSignalsCount = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .onRequest().invoke(requestSignalsCount::incrementAndGet)
                .emitOn(executor, 3)
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(Long.MAX_VALUE)
                .awaitCompletion()
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(requestSignalsCount.get()).isEqualTo(4);
    }

    @Test
    public void testWithRequest0() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(0);
        subscriber.awaitFailure(t -> assertThat(t)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request"));
    }

    @Test
    public void testWithShutdownExecutor() {
        ExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.shutdownNow();

        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertFailedWith(RejectedExecutionException.class, "");
    }

    @Test
    public void testWithRogueUpstreamSendingTooManyItems() {
        Multi<Integer> rogue = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(MultiSubscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(mock(Flow.Subscription.class));
                for (int i = 0; i < 10000; i++) {
                    subscriber.onItem(i);
                }
                subscriber.onComplete();
            }
        };

        AssertSubscriber<Integer> subscriber = rogue
                .emitOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create(1));

        await().untilAsserted(() -> subscriber.assertFailedWith(BackPressureFailure.class, ""));

        subscriber.assertFailedWith(BackPressureFailure.class, "");
    }

    @Test
    public void testSecondOnFailureDoesNotOverwriteBackPressureFailure() {
        // Use an executor that captures but does not immediately run tasks,
        // so items pile up and the queue overflows before draining.
        var tasks = new java.util.concurrent.CopyOnWriteArrayList<Runnable>();
        Executor capturingExecutor = tasks::add;

        Multi<Integer> rogue = new AbstractMulti<>() {
            @Override
            public void subscribe(MultiSubscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(Subscriptions.empty());
                for (int i = 0; i < 100; i++) {
                    subscriber.onItem(i);
                }
                subscriber.onFailure(new RuntimeException("upstream error"));
            }
        };

        AssertSubscriber<Integer> subscriber = rogue
                .emitOn(capturingExecutor, 1)
                .subscribe().withSubscriber(AssertSubscriber.create(1));

        // Now drain the captured tasks
        for (Runnable task : tasks) {
            task.run();
        }

        subscriber.assertFailedWith(BackPressureFailure.class, "");
        Throwable[] suppressed = subscriber.getFailure().getSuppressed();
        assertThat(suppressed).hasSize(1);
        assertThat(suppressed[0]).isInstanceOf(RuntimeException.class).hasMessage("upstream error");
    }

    @Test
    public void testBufferSizeValidation() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        assertThatThrownBy(() -> multi.emitOn(executor, -58))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bufferSize");

        assertThatThrownBy(() -> multi.emitOn(executor, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bufferSize");

        assertThatCode(() -> multi.emitOn(executor, 58)).doesNotThrowAnyException();
    }
}
