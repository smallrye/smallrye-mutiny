package io.smallrye.mutiny.operators;

import java.io.IOException;
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
}
