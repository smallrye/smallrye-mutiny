package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class UniOnCancellationTest {

    @AfterEach
    void cleanup() {
        Infrastructure.resetDroppedExceptionHandler();
    }

    @Test
    void chainedOperationIsNotStartedAfterDownstreamCancellation() {
        assertChainedOperation(true);
    }

    @Test
    void normalCompletionStartsChainedOperation() {
        assertChainedOperation(false);
    }

    private void assertChainedOperation(boolean cancel) {
        AtomicReference<Flow.Subscriber<? super Integer>> inner = new AtomicReference<>();
        Flow.Publisher<Integer> late = inner::set;
        AtomicBoolean chainedOperationCalled = new AtomicBoolean();

        AssertSubscriber<Integer> subscriber = Uni.createFrom().publisher(late)
                .chain(item -> {
                    chainedOperationCalled.set(true);
                    return Uni.createFrom().item(item);
                })
                .onCancellation().call(() -> Uni.createFrom().nothing())
                .toMulti()
                .subscribe().withSubscriber(AssertSubscriber.create(1));

        inner.get().onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
            }
        });

        if (cancel) {
            subscriber.cancel();
        }
        inner.get().onNext(1);

        assertThat(chainedOperationCalled.get()).isEqualTo(!cancel);
        if (!cancel) {
            subscriber.assertCompleted().assertItems(1);
        }
    }

    @Test
    void onCancellationCallbackThrowsInCancel() {
        AtomicBoolean upstreamCancelled = new AtomicBoolean(false);
        AtomicReference<Throwable> droppedException = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(droppedException::set);

        UniAssertSubscriber<String> subscriber = Uni.createFrom().<String> emitter(emitter -> {
        })
                .onCancellation().invoke(() -> upstreamCancelled.set(true))
                .onCancellation().invoke(() -> {
                    throw new RuntimeException("cancel callback failed");
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.cancel();

        assertThat(upstreamCancelled).isTrue();
        assertThat(droppedException.get()).isInstanceOf(RuntimeException.class).hasMessage("cancel callback failed");
    }
}
