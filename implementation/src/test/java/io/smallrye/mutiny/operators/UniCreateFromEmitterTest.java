package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniCreateFromEmitterTest {
    @Test
    public void testThatConsumerCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().emitter(null));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testThatOnlyTheFirstSignalIsConsidered() {
        AtomicReference<UniEmitter<? super Integer>> reference = new AtomicReference<>();
        Uni<Integer> uni = Uni.createFrom().emitter(emitter -> {
            reference.set(emitter);
            emitter.complete(1);
            emitter.fail(new Exception());
            emitter.complete(2);
            emitter.complete(null);
        });
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        uni.subscribe().withSubscriber(subscriber);

        subscriber.assertCompleted().assertItem(1);
        // Other signals are dropped
        assertThat(((DefaultUniEmitter) reference.get()).isTerminated()).isTrue();
    }

    @Test
    public void testWithOnTerminationActionWithCancellation() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicInteger onTerminationCalled = new AtomicInteger();
        Uni.createFrom().<Integer> emitter(emitter -> emitter.onTermination(onTerminationCalled::incrementAndGet))
                .subscribe()
                .withSubscriber(subscriber);

        assertThat(onTerminationCalled).hasValue(0);
        subscriber.cancel();
        assertThat(onTerminationCalled).hasValue(1);
    }

    @Test
    public void testWithOnTerminationActionWithResult() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicInteger onTerminationCalled = new AtomicInteger();
        Uni.createFrom().<Integer> emitter(emitter -> {
            emitter.onTermination(onTerminationCalled::incrementAndGet);
            emitter.complete(1);
        }).subscribe().withSubscriber(subscriber);

        assertThat(onTerminationCalled).hasValue(1);
        subscriber.cancel();
        assertThat(onTerminationCalled).hasValue(1);

        subscriber.assertCompleted();
    }

    @Test
    public void testWithOnTerminationActionWithFailure() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicInteger onTerminationCalled = new AtomicInteger();
        Uni.createFrom().<Integer> emitter(emitter -> {
            emitter.onTermination(onTerminationCalled::incrementAndGet);
            emitter.fail(new IOException("boom"));
        }).subscribe().withSubscriber(subscriber);

        assertThat(onTerminationCalled).hasValue(1);
        subscriber.cancel();
        assertThat(onTerminationCalled).hasValue(1);

        subscriber.assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testWithOnTerminationCallback() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicInteger onTerminationCalled = new AtomicInteger();
        Uni.createFrom().<Integer> emitter(emitter -> emitter.onTermination(onTerminationCalled::incrementAndGet))
                .subscribe().withSubscriber(subscriber);

        assertThat(onTerminationCalled).hasValue(0);
        subscriber.cancel();
        assertThat(onTerminationCalled).hasValue(1);
    }

    @Test
    public void testWithFailure() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().<Integer> emitter(emitter -> emitter.fail(new Exception("boom"))).subscribe()
                .withSubscriber(subscriber);

        subscriber.assertFailedWith(Exception.class, "boom");
    }

    @Test
    public void testWhenTheCallbackThrowsAnException() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().<Integer> emitter(emitter -> {
            throw new NullPointerException("boom");
        }).subscribe().withSubscriber(subscriber);

        subscriber.assertFailedWith(NullPointerException.class, "boom");

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testThatEmitterIsDisposed() {
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        AtomicReference<UniEmitter<? super Void>> reference = new AtomicReference<>();
        Uni.createFrom().<Void> emitter(emitter -> {
            reference.set(emitter);
            emitter.complete(null);
        }).subscribe().withSubscriber(subscriber);

        subscriber.assertCompleted();
        assertThat(reference.get()).isInstanceOf(DefaultUniEmitter.class)
                .satisfies(e -> ((DefaultUniEmitter<? super Void>) e).isTerminated());
    }

    @Test
    public void testThatFailuresCannotBeNull() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().<Integer> emitter(emitter -> emitter.fail(null)).subscribe().withSubscriber(subscriber);

        subscriber.assertFailedWith(IllegalArgumentException.class, "");
    }

    @Test
    public void testFailureThrownBySubscribersOnItem() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicBoolean onTerminationCalled = new AtomicBoolean();
        Uni.createFrom().<Integer> emitter(emitter -> {
            emitter.onTermination(() -> onTerminationCalled.set(true));
            try {
                emitter.complete(1);
                fail("Exception expected");
            } catch (Exception ex) {
                // expected
            }
        })
                .subscribe().withSubscriber(new UniSubscriber<Integer>() {

                    @Override
                    public void onSubscribe(UniSubscription subscription) {

                    }

                    @Override
                    public void onItem(Integer ignored) {
                        throw new NullPointerException("boom");
                    }

                    @Override
                    public void onFailure(Throwable failure) {
                        called.set(true);
                    }
                });

        assertThat(called).isFalse();
        assertThat(onTerminationCalled).isFalse();
    }

    @Test
    public void testFailureThrownBySubscribersOnFailure() {
        AtomicBoolean onTerminationCalled = new AtomicBoolean();
        Uni.createFrom().<Integer> emitter(emitter -> {
            emitter.onTermination(() -> onTerminationCalled.set(true));
            try {
                emitter.fail(new Exception("boom"));
                fail("Exception expected");
            } catch (Exception ex) {
                // expected
            }
        })
                .subscribe().withSubscriber(new UniSubscriber<Integer>() {

                    @Override
                    public void onSubscribe(UniSubscription subscription) {

                    }

                    @Override
                    public void onItem(Integer ignored) {
                    }

                    @Override
                    public void onFailure(Throwable failure) {
                        throw new NullPointerException("boom");
                    }
                });

        assertThat(onTerminationCalled).isFalse();
    }

    @Test
    public void testWithSharedState() {
        UniAssertSubscriber<Integer> s1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> s2 = UniAssertSubscriber.create();
        AtomicInteger shared = new AtomicInteger();
        Uni<Integer> uni = Uni.createFrom().emitter(() -> shared,
                (state, emitter) -> emitter.complete(state.incrementAndGet()));

        assertThat(shared).hasValue(0);
        uni.subscribe().withSubscriber(s1);
        assertThat(shared).hasValue(1);
        s1.assertCompleted().assertItem(1);
        uni.subscribe().withSubscriber(s2);
        assertThat(shared).hasValue(2);
        s2.assertCompleted().assertItem(2);
    }

    @Test
    public void testWithSharedStateProducingFailure() {
        UniAssertSubscriber<Integer> s1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> s2 = UniAssertSubscriber.create();
        Supplier<AtomicInteger> boom = () -> {
            throw new IllegalStateException("boom");
        };

        Uni<Integer> uni = Uni.createFrom().emitter(boom,
                (state, emitter) -> emitter.complete(state.incrementAndGet()));

        uni.subscribe().withSubscriber(s1);
        s1.assertFailedWith(IllegalStateException.class, "boom");
        uni.subscribe().withSubscriber(s2);
        s2.assertFailedWith(IllegalStateException.class, "Invalid shared state");
    }

    @Test
    public void testWithSharedStateProducingNull() {
        UniAssertSubscriber<Integer> s1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> s2 = UniAssertSubscriber.create();
        Supplier<AtomicInteger> boom = () -> null;

        Uni<Integer> uni = Uni.createFrom().emitter(boom,
                (state, emitter) -> emitter.complete(state.incrementAndGet()));

        uni.subscribe().withSubscriber(s1);
        s1.assertFailedWith(NullPointerException.class, "supplier");
        uni.subscribe().withSubscriber(s2);
        s2.assertFailedWith(IllegalStateException.class, "Invalid shared state");
    }

    @Test
    public void testThatStateSupplierCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().emitter(null,
                (x, emitter) -> {
                }));
    }

    @Test
    public void testThatFunctionCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().emitter(() -> "hello",
                null));
    }
}
