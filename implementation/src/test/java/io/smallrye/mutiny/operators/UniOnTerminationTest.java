package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
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
                .awaitSubscription()
                .awaitItem()
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
        subscriber.awaitItem().assertItem(1);
        assertThat(terminate).isTrue();
    }

    @Test
    public void testTerminationAfterImmediateFailure() {
        AtomicReference<Throwable> terminate = new AtomicReference<>();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> failure(new TestException("boom"))
                .onTermination().invoke((r, f, c) -> terminate.set(f))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailedWith(TestException.class, "boom");
        assertThat(terminate.get()).hasMessageContaining("boom").isInstanceOf(TestException.class);
    }

    @Test
    public void testTerminationAfterDelayedFailure() {
        AtomicReference<Throwable> terminate = new AtomicReference<>();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> failure(new TestException("boom"))
                .emitOn(Infrastructure.getDefaultExecutor())
                .onTermination().invoke((r, f, c) -> terminate.set(f))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitFailure().assertFailedWith(TestException.class, "boom");
        assertThat(terminate.get()).hasMessageContaining("boom").isInstanceOf(TestException.class);
    }

    @Test
    public void testTerminationWithoutParameterAfterImmediateFailure() {
        AtomicBoolean terminate = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> failure(new TestException("boom"))
                .onTermination().invoke(() -> terminate.set(true))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailedWith(TestException.class, "boom");
        assertThat(terminate).isTrue();
    }

    @Test
    public void testTerminationWithoutParameterAfterDelayedFailure() {
        AtomicBoolean terminate = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> failure(new TestException("boom"))
                .emitOn(Infrastructure.getDefaultExecutor())
                .onTermination().invoke(() -> terminate.set(true))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitFailure().assertFailedWith(TestException.class, "boom");
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
        subscriber.assertFailedWith(TestException.class, "boom");
    }

    @Test
    public void testTerminationThrowingExceptionOnFailure() {
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer> failure(new IOException("I/O"))
                .onTermination().invoke((r, f, c) -> {
                    throw new TestException("boom");
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailedWith(CompositeException.class, "boom");
        subscriber.assertFailedWith(CompositeException.class, "I/O");
    }

    @Test
    void testTerminationOnFailureAndCallReturningAnItem() {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Object> result = new AtomicReference<>();

        AtomicBoolean subCancelled = new AtomicBoolean();
        AtomicReference<Throwable> subFailure = new AtomicReference<>();
        AtomicReference<Object> subResult = new AtomicReference<>();

        UniAssertSubscriber<?> subscriber = Uni.createFrom().failure(new IOException("boom"))
                .onTermination().call((r, f, c) -> {
                    result.set(r);
                    failure.set(f);
                    cancelled.set(c);
                    return Uni.createFrom().item("yolo")
                            .onTermination().invoke((sr, sf, sc) -> {
                                subResult.set(sr);
                                subFailure.set(sf);
                                subCancelled.set(sc);
                            });
                })
                .subscribe().withSubscriber(new UniAssertSubscriber<>());

        subscriber.assertFailedWith(IOException.class, "boom");

        assertThat(result.get()).isNull();
        assertThat(failure.get()).isInstanceOf(IOException.class).hasMessage("boom");
        assertThat(cancelled.get()).isFalse();

        assertThat(subResult.get()).isEqualTo("yolo");
        assertThat(subFailure.get()).isNull();
        assertThat(subCancelled.get()).isFalse();
    }
}
