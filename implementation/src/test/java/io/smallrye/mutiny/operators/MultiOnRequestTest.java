package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiOnRequestTest {

    @Test
    public void testInvoke() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        AtomicLong requested = new AtomicLong();

        Multi.createFrom().item(1)
                .onRequest().invoke(requested::set)
                .subscribe().withSubscriber(subscriber);

        subscriber.request(10);

        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).containsExactly(1);
        assertThat(requested.get()).isEqualTo(10);
    }

    @Test
    public void testInvokeRunnable() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        AtomicBoolean called = new AtomicBoolean();

        Multi.createFrom().item(1)
                .onRequest().invoke(() -> called.set(true))
                .subscribe().withSubscriber(subscriber);

        subscriber.request(10);

        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).containsExactly(1);
        assertThat(called).isTrue();
    }

    @Test
    public void testInvokeThrowingException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        AtomicLong requested = new AtomicLong();

        Multi.createFrom().item(1)
                .onRequest().invoke(count -> {
                    requested.set(count);
                    throw new RuntimeException("woops");
                })
                .subscribe().withSubscriber(subscriber);

        subscriber.request(10);
        subscriber.assertFailedWith(RuntimeException.class, "woops");
        subscriber.assertHasNotReceivedAnyItem();
        assertThat(requested.get()).isEqualTo(10);
    }

    @Test
    public void testCall() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        AtomicLong requested = new AtomicLong();

        Multi.createFrom().item(1)
                .onRequest().call(count -> {
                    requested.set(count);
                    return Uni.createFrom().item("ok");
                })
                .subscribe().withSubscriber(subscriber);

        subscriber.request(10);

        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).containsExactly(1);
        assertThat(requested.get()).isEqualTo(10);
    }

    @Test
    public void testCallWithSupplier() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();

        Multi.createFrom().item(1)
                .onRequest().call(() -> Uni.createFrom().item("ok")
                        .onSubscription().invoke(() -> called.set(true)))
                .subscribe().withSubscriber(subscriber);

        subscriber.request(10);

        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).containsExactly(1);
        assertThat(called).isTrue();
    }

    @Test
    public void testCallError() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        AtomicLong requested = new AtomicLong();

        Multi.createFrom().item(1)
                .onRequest().call(count -> {
                    requested.set(count);
                    return Uni.createFrom().failure(new RuntimeException("woops"));
                })
                .subscribe().withSubscriber(subscriber);

        subscriber.request(10);

        subscriber.request(10);
        subscriber.assertFailedWith(RuntimeException.class, "woops");
        subscriber.assertHasNotReceivedAnyItem();
        assertThat(requested.get()).isEqualTo(10);
    }

    @Test
    public void testCallThrowingException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        AtomicLong requested = new AtomicLong();

        Multi.createFrom().item(1)
                .onRequest().call(count -> {
                    requested.set(count);
                    throw new RuntimeException("woops");
                })
                .subscribe().withSubscriber(subscriber);

        subscriber.request(10);

        subscriber.request(10);
        subscriber.assertFailedWith(RuntimeException.class, "woops");
        subscriber.assertHasNotReceivedAnyItem();
        assertThat(requested.get()).isEqualTo(10);
    }

    @Test
    public void testCallAndCancellation() {
        AssertSubscriber<Object> subscriber = AssertSubscriber.create();

        AtomicLong requested = new AtomicLong();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().emitter(e -> {
            // Do nothing
        })
                .onRequest().call(count -> {
                    requested.set(count);
                    return Uni.createFrom().emitter(ue -> {
                        // Do nothing
                    })
                            .onCancellation().invoke(() -> cancellation.set(true));
                }).subscribe().withSubscriber(subscriber);

        subscriber.request(10);
        subscriber.cancel();

        subscriber.assertNotTerminated();
        subscriber.assertHasNotReceivedAnyItem();
        assertThat(requested.get()).isEqualTo(10);
        assertThat(cancellation.get()).isTrue();
    }
}
