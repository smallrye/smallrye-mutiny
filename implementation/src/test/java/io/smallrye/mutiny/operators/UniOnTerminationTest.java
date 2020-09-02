package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

@SuppressWarnings("ConstantConditions")
public class UniOnTerminationTest {

    @Test
    public void testTerminationAfterImmediateItem() {
        AtomicInteger terminate = new AtomicInteger();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().item(1)
                .onTermination().invoke((r, f, c) -> terminate.set(r))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertItem(1);
        assertThat(terminate).hasValue(1);
    }

    @Test
    public void testTerminationAfterDelayedItem() {
        AtomicInteger terminate = new AtomicInteger();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().item(1)
                .emitOn(Infrastructure.getDefaultExecutor())
                .onTermination().invoke((r, f, c) -> terminate.set(r))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber
                .await()
                .assertItem(1);
        assertThat(terminate).hasValue(1);
    }

    @Test
    public void testTerminationWithoutParamsAfterImmediateItem() {
        AtomicBoolean terminate = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().item(1)
                .onTermination().invoke(() -> terminate.set(true))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertItem(1);
        assertThat(terminate).isTrue();
    }

    @Test
    public void testTerminationWithoutParamsAfterDelayedItem() {
        AtomicBoolean terminate = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().item(1)
                .emitOn(Infrastructure.getDefaultExecutor())
                .onTermination().invoke(() -> terminate.set(true))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.await().assertItem(1);
        assertThat(terminate).isTrue();
    }

    @Test
    public void testTerminationAfterImmediateFailure() {
        AtomicReference<Throwable> terminate = new AtomicReference<>();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> failure(new TestException("boom"))
                .onTermination().invoke((r, f, c) -> terminate.set(f))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailure(TestException.class, "boom");
        assertThat(terminate.get()).hasMessageContaining("boom").isInstanceOf(TestException.class);
    }

    @Test
    public void testTerminationAfterDelayedFailure() {
        AtomicReference<Throwable> terminate = new AtomicReference<>();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> failure(new TestException("boom"))
                .emitOn(Infrastructure.getDefaultExecutor())
                .onTermination().invoke((r, f, c) -> terminate.set(f))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.await().assertFailure(TestException.class, "boom");
        assertThat(terminate.get()).hasMessageContaining("boom").isInstanceOf(TestException.class);
    }

    @Test
    public void testTerminationWithoutParameterAfterImmediateFailure() {
        AtomicBoolean terminate = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> failure(new TestException("boom"))
                .onTermination().invoke(() -> terminate.set(true))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailure(TestException.class, "boom");
        assertThat(terminate).isTrue();
    }

    @Test
    public void testTerminationWithoutParameterAfterDelayedFailure() {
        AtomicBoolean terminate = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> failure(new TestException("boom"))
                .emitOn(Infrastructure.getDefaultExecutor())
                .onTermination().invoke(() -> terminate.set(true))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.await().assertFailure(TestException.class, "boom");
        assertThat(terminate).isTrue();
    }

    @Test
    public void testTerminationAfterCancellation() {
        AtomicBoolean terminate = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> emitter(e -> {
        })
                .onTermination().invoke((r, f, c) -> terminate.set(c))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertSubscribed()
                .cancel();
        assertThat(terminate).isTrue();
    }

    @Test
    public void testTerminationWithImmediateCancellation() {
        AtomicBoolean terminate = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> emitter(e -> {
        })
                .onTermination().invoke((r, f, c) -> terminate.set(c))
                .subscribe().withSubscriber(new UniAssertSubscriber<>(true));
        subscriber.assertSubscribed();
        assertThat(terminate).isTrue();
    }

    @Test
    public void testTerminationThrowingExceptionOnItem() {
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().item(1)
                .onTermination().invoke((r, f, c) -> {
                    throw new TestException("boom");
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailure(TestException.class, "boom");
    }

    @Test
    public void testTerminationThrowingExceptionOnFailure() {
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> failure(new IOException("I/O"))
                .onTermination().invoke((r, f, c) -> {
                    throw new TestException("boom");
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailure(CompositeException.class, "boom");
        subscriber.assertFailure(CompositeException.class, "I/O");
    }
}
