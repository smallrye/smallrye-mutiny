package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiOnTerminationUniInvokeTest {

    @Test
    public void testTerminationWhenErrorIsEmitted() {
        AssertSubscriber<Object> subscriber = AssertSubscriber.create();

        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Object> item = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicLong requests = new AtomicLong();
        AtomicBoolean cancellation = new AtomicBoolean();

        AtomicReference<Integer> uniItem = new AtomicReference<>();
        AtomicBoolean termination = new AtomicBoolean();
        AtomicReference<Throwable> terminationException = new AtomicReference<>();
        AtomicBoolean terminationCancelledFlag = new AtomicBoolean();

        Multi.createFrom().failure(new IOException("boom"))
                .onSubscribe().invoke(subscription::set)
                .on().item().invoke(item::set)
                .on().failure().invoke(failure::set)
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invokeUni((t, c) -> {
                    termination.set(true);
                    terminationException.set(t);
                    terminationCancelledFlag.set(c);
                    return Uni.createFrom().item(69).invoke(uniItem::set);
                })
                .onRequest().invoke(requests::set)
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe().withSubscriber(subscriber);

        subscriber
                .request(20)
                .assertHasNotReceivedAnyItem()
                .assertHasFailedWith(IOException.class, "boom");

        assertThat(subscription.get()).isNotNull();
        assertThat(item.get()).isNull();
        assertThat(failure.get()).isNotNull().isInstanceOf(IOException.class).hasMessageContaining("boom");
        assertThat(completion.get()).isFalse();
        assertThat(requests.get()).isEqualTo(0L);
        assertThat(cancellation.get()).isFalse();

        assertThat(termination.get()).isTrue();
        assertThat(terminationException.get()).isNotNull().isInstanceOf(IOException.class).hasMessageContaining("boom");
        assertThat(terminationCancelledFlag.get()).isFalse();
        assertThat(uniItem.get()).isEqualTo(69);
    }

    @Test
    public void testTerminationWhenItemIsEmittedButUniInvokeIsFailed() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicLong requests = new AtomicLong();
        AtomicBoolean cancellation = new AtomicBoolean();

        AtomicBoolean termination = new AtomicBoolean();
        AtomicReference<Throwable> terminationException = new AtomicReference<>();
        AtomicBoolean terminationCancelledFlag = new AtomicBoolean();

        Multi.createFrom().item(1)
                .onSubscribe().invoke(subscription::set)
                .on().item().invoke(item::set)
                .on().failure().invoke(failure::set)
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invokeUni((t, c) -> {
                    termination.set(true);
                    terminationException.set(t);
                    terminationCancelledFlag.set(c);
                    return Uni.createFrom().failure(new IOException("bam"));
                })
                .onRequest().invoke(requests::set)
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe().withSubscriber(subscriber);

        subscriber
                .request(20)
                .assertReceived(1)
                .assertHasFailedWith(IOException.class, "bam");

        assertThat(subscription.get()).isNotNull();
        assertThat(item.get()).isEqualTo(1);
        assertThat(failure.get()).isNull();
        assertThat(completion.get()).isTrue();
        assertThat(requests.get()).isEqualTo(20);
        assertThat(cancellation.get()).isFalse();

        assertThat(termination.get()).isTrue();
        assertThat(terminationException.get()).isNull();
        assertThat(terminationCancelledFlag.get()).isFalse();
    }

    @Test
    public void testTerminationWhenItemIsEmittedButUniInvokeThrowsException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicLong requests = new AtomicLong();
        AtomicBoolean cancellation = new AtomicBoolean();

        AtomicBoolean termination = new AtomicBoolean();
        AtomicReference<Throwable> terminationException = new AtomicReference<>();
        AtomicBoolean terminationCancelledFlag = new AtomicBoolean();

        Multi.createFrom().item(1)
                .onSubscribe().invoke(subscription::set)
                .on().item().invoke(item::set)
                .on().failure().invoke(failure::set)
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invokeUni((t, c) -> {
                    termination.set(true);
                    terminationException.set(t);
                    terminationCancelledFlag.set(c);
                    throw new RuntimeException("bam");
                })
                .onRequest().invoke(requests::set)
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe().withSubscriber(subscriber);

        subscriber
                .request(20)
                .assertReceived(1)
                .assertHasFailedWith(RuntimeException.class, "bam");

        assertThat(subscription.get()).isNotNull();
        assertThat(item.get()).isEqualTo(1);
        assertThat(failure.get()).isNull();
        assertThat(completion.get()).isTrue();
        assertThat(requests.get()).isEqualTo(20);
        assertThat(cancellation.get()).isFalse();

        assertThat(termination.get()).isTrue();
        assertThat(terminationException.get()).isNull();
        assertThat(terminationCancelledFlag.get()).isFalse();
    }

    @Test
    public void testTerminationWhenErrorIsEmittedButUniInvokeIsFailed() {
        AssertSubscriber<Object> subscriber = AssertSubscriber.create();

        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Object> item = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicLong requests = new AtomicLong();
        AtomicBoolean cancellation = new AtomicBoolean();

        AtomicBoolean termination = new AtomicBoolean();
        AtomicReference<Throwable> terminationException = new AtomicReference<>();
        AtomicBoolean terminationCancelledFlag = new AtomicBoolean();

        Multi.createFrom().failure(new IOException("boom"))
                .onSubscribe().invoke(subscription::set)
                .on().item().invoke(item::set)
                .on().failure().invoke(failure::set)
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invokeUni((t, c) -> {
                    termination.set(true);
                    terminationException.set(t);
                    terminationCancelledFlag.set(c);
                    return Uni.createFrom().failure(new RuntimeException("tada"));
                })
                .onRequest().invoke(requests::set)
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe().withSubscriber(subscriber);

        subscriber
                .request(20)
                .assertHasNotReceivedAnyItem()
                .assertHasFailedWith(CompositeException.class, "boom");

        assertThat(subscriber.failures()).hasSize(1);
        CompositeException compositeException = (CompositeException) subscriber.failures().get(0);
        assertThat(compositeException.getCauses()).hasSize(2);
        assertThat(compositeException.getCauses().get(0)).isInstanceOf(IOException.class).hasMessage("boom");
        assertThat(compositeException.getCauses().get(1)).isInstanceOf(RuntimeException.class).hasMessage("tada");

        assertThat(subscription.get()).isNotNull();
        assertThat(item.get()).isNull();
        assertThat(failure.get()).isNotNull().isInstanceOf(IOException.class).hasMessageContaining("boom");
        assertThat(completion.get()).isFalse();
        assertThat(requests.get()).isEqualTo(0L);
        assertThat(cancellation.get()).isFalse();

        assertThat(termination.get()).isTrue();
        assertThat(terminationException.get()).isNotNull().isInstanceOf(IOException.class).hasMessageContaining("boom");
        assertThat(terminationCancelledFlag.get()).isFalse();
    }

    @Test
    public void testTerminationWhenErrorIsEmittedButUniInvokeThrowsException() {
        AssertSubscriber<Object> subscriber = AssertSubscriber.create();

        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Object> item = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicLong requests = new AtomicLong();
        AtomicBoolean cancellation = new AtomicBoolean();

        AtomicBoolean termination = new AtomicBoolean();
        AtomicReference<Throwable> terminationException = new AtomicReference<>();
        AtomicBoolean terminationCancelledFlag = new AtomicBoolean();

        Multi.createFrom().failure(new IOException("boom"))
                .onSubscribe().invoke(subscription::set)
                .on().item().invoke(item::set)
                .on().failure().invoke(failure::set)
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invokeUni((t, c) -> {
                    termination.set(true);
                    terminationException.set(t);
                    terminationCancelledFlag.set(c);
                    throw new RuntimeException("tada");
                })
                .onRequest().invoke(requests::set)
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe().withSubscriber(subscriber);

        subscriber
                .request(20)
                .assertHasNotReceivedAnyItem()
                .assertHasFailedWith(CompositeException.class, "boom");

        assertThat(subscriber.failures()).hasSize(1);
        CompositeException compositeException = (CompositeException) subscriber.failures().get(0);
        assertThat(compositeException.getCauses()).hasSize(2);
        assertThat(compositeException.getCauses().get(0)).isInstanceOf(IOException.class).hasMessage("boom");
        assertThat(compositeException.getCauses().get(1)).isInstanceOf(RuntimeException.class).hasMessage("tada");

        assertThat(subscription.get()).isNotNull();
        assertThat(item.get()).isNull();
        assertThat(failure.get()).isNotNull().isInstanceOf(IOException.class).hasMessageContaining("boom");
        assertThat(completion.get()).isFalse();
        assertThat(requests.get()).isEqualTo(0L);
        assertThat(cancellation.get()).isFalse();

        assertThat(termination.get()).isTrue();
        assertThat(terminationException.get()).isNotNull().isInstanceOf(IOException.class).hasMessageContaining("boom");
        assertThat(terminationCancelledFlag.get()).isFalse();
    }

    @Test
    public void testTerminationWithCancellationAndNotItems() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicBoolean cancellation = new AtomicBoolean();

        AtomicReference<Integer> uniItem = new AtomicReference<>();
        AtomicBoolean termination = new AtomicBoolean();
        AtomicReference<Throwable> terminationException = new AtomicReference<>();
        AtomicBoolean terminationCancelledFlag = new AtomicBoolean();

        AtomicReference<Integer> subItem = new AtomicReference<>();
        AtomicReference<Throwable> subException = new AtomicReference<>();
        AtomicBoolean subCancellation = new AtomicBoolean();

        Multi.createFrom().item(1)
                .on().item().invoke(item::set)
                .onTermination().invokeUni((t, c) -> {
                    termination.set(true);
                    terminationException.set(t);
                    terminationCancelledFlag.set(c);
                    return Uni.createFrom().item(69).invoke(uniItem::set).onTermination().invoke((si, sc, sb) -> {
                        subItem.set(si);
                        subException.set(sc);
                        subCancellation.set(sb);
                    });
                })
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe().withSubscriber(subscriber);

        subscriber.cancel()
                .assertHasNotReceivedAnyItem()
                .assertHasNotCompleted();

        assertThat(item.get()).isNull();
        assertThat(cancellation.get()).isTrue();
        assertThat(termination.get()).isTrue();
        assertThat(terminationException.get()).isNull();
        assertThat(terminationCancelledFlag.get()).isTrue();
        assertThat(uniItem.get()).isEqualTo(69);

        assertThat(subItem.get()).isEqualTo(69);
        assertThat(subException.get()).isNull();
        assertThat(subCancellation.get()).isFalse();
    }

    @Test
    public void testTerminationWithCancellationAfterOneItem() {
        AssertSubscriber<Object> subscriber = AssertSubscriber.create();

        AtomicReference<Object> item = new AtomicReference<>();
        AtomicBoolean cancellation = new AtomicBoolean();

        AtomicReference<Object> uniItem = new AtomicReference<>();
        AtomicBoolean termination = new AtomicBoolean();
        AtomicReference<Throwable> terminationException = new AtomicReference<>();
        AtomicBoolean terminationCancelledFlag = new AtomicBoolean();

        AtomicReference<Object> subItem = new AtomicReference<>();
        AtomicReference<Throwable> subException = new AtomicReference<>();
        AtomicBoolean subCancellation = new AtomicBoolean();

        AtomicBoolean firstItemEmitted = new AtomicBoolean();
        AtomicBoolean cancellationSent = new AtomicBoolean();
        AtomicBoolean uniCompleted = new AtomicBoolean();
        Multi.createFrom().emitter(e -> {
            e.emit(1);
            e.complete();
            firstItemEmitted.set(true);
        })
                .onItem().invoke(item::set)
                .onTermination().invokeUni((t, c) -> { // Must be called for a completion
                    termination.set(true);
                    terminationException.set(t);
                    terminationCancelledFlag.set(c);
                    return Uni.createFrom().emitter(e -> {
                        new Thread(() -> {
                            await().untilTrue(cancellationSent);
                            e.complete("yo");
                            uniCompleted.set(true);
                        }).start();
                    })
                            .invoke(uniItem::set) // Must be null since cancelled
                            .onTermination().invoke((si, sc, sb) -> { // Must be called for a cancellation
                                subItem.set(si);
                                subException.set(sc);
                                subCancellation.set(sb);
                            });
                })
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe().withSubscriber(subscriber);

        subscriber.request(10);
        await().untilTrue(firstItemEmitted);
        subscriber.cancel();
        cancellationSent.set(true);
        await().untilTrue(uniCompleted);

        subscriber.assertReceived(1).assertHasNotCompleted();

        assertThat(item.get()).isEqualTo(1);
        assertThat(cancellation.get()).isTrue();
        assertThat(termination.get()).isTrue();
        assertThat(terminationException.get()).isNull();
        assertThat(terminationCancelledFlag.get()).isFalse();
        assertThat(uniItem.get()).isNull();

        assertThat(subItem.get()).isNull();
        assertThat(subException.get()).isNull();
        assertThat(subCancellation.get()).isTrue();
    }
}
