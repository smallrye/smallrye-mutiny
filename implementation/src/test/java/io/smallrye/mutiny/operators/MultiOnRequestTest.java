package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiOnRequestTest {

    @Test
    public void testInvoke() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();

        AtomicLong requested = new AtomicLong();

        Multi.createFrom().item(1)
                .onRequest().invoke(requested::set)
                .subscribe().withSubscriber(ts);

        ts.request(10);

        ts.assertCompletedSuccessfully();
        assertThat(ts.items()).containsExactly(1);
        assertThat(requested.get()).isEqualTo(10);
    }

    @Test
    public void testInvokeThrowingException() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();

        AtomicLong requested = new AtomicLong();

        Multi.createFrom().item(1)
                .onRequest().invoke(count -> {
                    requested.set(count);
                    throw new RuntimeException("woops");
                })
                .subscribe().withSubscriber(ts);

        ts.request(10);
        ts.assertHasFailedWith(RuntimeException.class, "woops");
        ts.assertHasNotReceivedAnyItem();
        assertThat(requested.get()).isEqualTo(10);
    }

    @Test
    public void testInvokeUni() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();

        AtomicLong requested = new AtomicLong();

        Multi.createFrom().item(1)
                .onRequest().invokeUni(count -> {
                    requested.set(count);
                    return Uni.createFrom().item("ok");
                })
                .subscribe().withSubscriber(ts);

        ts.request(10);

        ts.assertCompletedSuccessfully();
        assertThat(ts.items()).containsExactly(1);
        assertThat(requested.get()).isEqualTo(10);
    }

    @Test
    public void testInvokeUniError() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();

        AtomicLong requested = new AtomicLong();

        Multi.createFrom().item(1)
                .onRequest().invokeUni(count -> {
                    requested.set(count);
                    return Uni.createFrom().failure(new RuntimeException("woops"));
                })
                .subscribe().withSubscriber(ts);

        ts.request(10);

        ts.request(10);
        ts.assertHasFailedWith(RuntimeException.class, "woops");
        ts.assertHasNotReceivedAnyItem();
        assertThat(requested.get()).isEqualTo(10);
    }

    @Test
    public void testInvokeUniThrowingException() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();

        AtomicLong requested = new AtomicLong();

        Multi.createFrom().item(1)
                .onRequest().invokeUni(count -> {
                    requested.set(count);
                    throw new RuntimeException("woops");
                })
                .subscribe().withSubscriber(ts);

        ts.request(10);

        ts.request(10);
        ts.assertHasFailedWith(RuntimeException.class, "woops");
        ts.assertHasNotReceivedAnyItem();
        assertThat(requested.get()).isEqualTo(10);
    }

    @Test
    public void testInvokeUniAndCancellation() {
        MultiAssertSubscriber<Object> ts = MultiAssertSubscriber.create();

        AtomicLong requested = new AtomicLong();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().emitter(e -> {
            // Do nothing
        })
                .onRequest().invokeUni(count -> {
                    requested.set(count);
                    return Uni.createFrom().emitter(ue -> {
                        // Do nothing
                    })
                            .onCancellation().invoke(() -> cancellation.set(true));
                }).subscribe().withSubscriber(ts);

        ts.request(10);
        ts.cancel();

        ts.assertHasNotCompleted();
        ts.assertHasNotReceivedAnyItem();
        assertThat(requested.get()).isEqualTo(10);
        assertThat(cancellation.get()).isTrue();
    }
}
