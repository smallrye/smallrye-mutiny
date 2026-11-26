package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class UniEmitOnTest {

    @Test
    void emitOnWithShutdownExecutorOnItem() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.shutdown();

        UniAssertSubscriber<String> subscriber = Uni.createFrom().item("hello")
                .emitOn(executor)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitFailure().assertFailedWith(RejectedExecutionException.class);
    }

    @Test
    void emitOnWithShutdownExecutorOnFailure() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.shutdown();

        UniAssertSubscriber<String> subscriber = Uni.createFrom().<String> failure(new IOException("boom"))
                .emitOn(executor)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitFailure().assertFailedWith(CompositeException.class);
    }

    @Test
    void testItemNotForwardedWhenCancelledBeforeDispatchRuns() {
        List<Runnable> tasks = new ArrayList<>();

        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(1)
                .emitOn(tasks::add)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        assertThat(tasks).hasSize(1);
        subscriber.cancel();
        tasks.forEach(Runnable::run);

        subscriber.assertNotTerminated();
    }

    @Test
    void testFailureNotForwardedWhenCancelledBeforeDispatchRuns() {
        List<Runnable> tasks = new ArrayList<>();

        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().<Integer> failure(new IOException("boom"))
                .emitOn(tasks::add)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        assertThat(tasks).hasSize(1);
        subscriber.cancel();
        tasks.forEach(Runnable::run);

        subscriber.assertNotTerminated();
    }
}
