package io.smallrye.reactive.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.subscription.UniEmitter;
import io.smallrye.reactive.subscription.UniSubscriber;
import io.smallrye.reactive.subscription.UniSubscription;

public class UniCreateFromEmitterTest {
    @Test(expected = IllegalArgumentException.class)
    public void testThatConsumerCannotBeNull() {
        Uni.createFrom().emitter(null);
    }

    @Test
    public void testThatOnlyTheFirstSignalIsConsidered() {
        AtomicReference<UniEmitter> reference = new AtomicReference<>();
        Uni<Integer> uni = Uni.createFrom().emitter(emitter -> {
            reference.set(emitter);
            emitter.complete(1);
            emitter.fail(new Exception());
            emitter.complete(2);
            emitter.complete(null);
        });
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        uni.subscribe().withSubscriber(subscriber);

        subscriber.assertCompletedSuccessfully().assertItem(1);
        // Other signals are dropped
        assertThat(((DefaultUniEmitter) reference.get()).isTerminated()).isTrue();
    }

    @Test
    public void testWithOnTerminationActionWithCancellation() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicInteger onTerminationCalled = new AtomicInteger();
        Uni.createFrom().<Integer> emitter(emitter -> emitter.onTermination(onTerminationCalled::incrementAndGet)).subscribe()
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

        subscriber.assertCompletedSuccessfully();
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

        subscriber.assertFailure(IOException.class, "boom");
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

        subscriber.assertFailure(Exception.class, "boom");
    }

    @Test
    public void testWhenTheCallbackThrowsAnException() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().<Integer> emitter(emitter -> {
            throw new NullPointerException("boom");
        }).subscribe().withSubscriber(subscriber);

        subscriber.assertFailure(NullPointerException.class, "boom");

    }

    @Test
    public void testThatEmitterIsDisposed() {
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        AtomicReference<UniEmitter<? super Void>> reference = new AtomicReference<>();
        Uni.createFrom().<Void> emitter(emitter -> {
            reference.set(emitter);
            emitter.complete(null);
        }).subscribe().withSubscriber(subscriber);

        subscriber.assertCompletedSuccessfully();
        assertThat(reference.get()).isInstanceOf(DefaultUniEmitter.class)
                .satisfies(e -> ((DefaultUniEmitter) e).isTerminated());
    }

    @Test
    public void testThatFailuresCannotBeNull() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().<Integer> emitter(emitter -> emitter.fail(null)).subscribe().withSubscriber(subscriber);

        subscriber.assertFailure(IllegalArgumentException.class, "");
    }

    @Test
    public void testFailureThrownBySubscribersOnResult() {
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
}
