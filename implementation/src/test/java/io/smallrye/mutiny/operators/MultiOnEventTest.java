package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiOnEventTest {
    @Test
    public void testCallbacksWhenItemIsEmitted() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();

        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicLong requests = new AtomicLong();
        AtomicBoolean termination = new AtomicBoolean();
        AtomicBoolean termination2 = new AtomicBoolean();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().item(1)
                .on().subscribed(subscription::set)
                .on().item().invoke(item::set)
                .on().failure().invoke(failure::set)
                .on().completion(() -> completion.set(true))
                .on().termination((f, c) -> termination.set(f == null && !c))
                .on().termination(() -> termination2.set(true))
                .on().request(requests::set)
                .on().cancellation(() -> cancellation.set(true))
                .subscribe(ts);

        ts
                .request(20)
                .assertCompletedSuccessfully()
                .assertReceived(1);

        assertThat(subscription.get()).isNotNull();
        assertThat(item.get()).isEqualTo(1);
        assertThat(failure.get()).isNull();
        assertThat(completion.get()).isTrue();
        assertThat(termination.get()).isTrue();
        assertThat(termination2.get()).isTrue();
        assertThat(requests.get()).isEqualTo(20);
        assertThat(cancellation.get()).isFalse();
    }

    @Test
    public void testCallbacksOnFailure() {
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicLong requests = new AtomicLong();
        AtomicBoolean termination = new AtomicBoolean();
        AtomicBoolean termination2 = new AtomicBoolean();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .on().subscribed(subscription::set)
                .on().item().invoke(item::set)
                .on().failure().invoke(failure::set)
                .on().completion(() -> completion.set(true))
                .on().termination((f, c) -> termination.set(f != null))
                .on().termination(() -> termination2.set(true))
                .on().request(requests::set)
                .on().cancellation(() -> cancellation.set(true))
                .subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasFailedWith(IOException.class, "boom");

        assertThat(subscription.get()).isNotNull();
        assertThat(item.get()).isNull();
        assertThat(failure.get()).isInstanceOf(IOException.class).hasMessageContaining("boom");
        assertThat(completion.get()).isFalse();
        assertThat(termination.get()).isTrue();
        assertThat(termination2.get()).isTrue();
        assertThat(requests.get()).isEqualTo(0);
        assertThat(cancellation.get()).isFalse();
    }

    @Test
    public void testCallbacksOnFailureWhenPredicateMatches() {
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicLong requests = new AtomicLong();
        AtomicBoolean termination = new AtomicBoolean();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .on().subscribed(subscription::set)
                .on().item().invoke(item::set)
                .on().failure(IOException.class).invoke(failure::set)
                .on().completion(() -> completion.set(true))
                .on().termination((f, c) -> termination.set(f != null))
                .on().request(requests::set)
                .on().cancellation(() -> cancellation.set(true))
                .subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasFailedWith(IOException.class, "boom");

        assertThat(subscription.get()).isNotNull();
        assertThat(item.get()).isNull();
        assertThat(failure.get()).isInstanceOf(IOException.class).hasMessageContaining("boom");
        assertThat(completion.get()).isFalse();
        assertThat(termination.get()).isTrue();
        assertThat(requests.get()).isEqualTo(0);
        assertThat(cancellation.get()).isFalse();
    }

    @Test
    public void testCallbacksOnFailureWhenPredicateDoesNotPass() {
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicLong requests = new AtomicLong();
        AtomicBoolean termination = new AtomicBoolean();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .on().subscribed(subscription::set)
                .on().item().invoke(item::set)
                .on().failure(f -> f.getMessage().contains("missing")).invoke(failure::set)
                .on().completion(() -> completion.set(true))
                .on().termination((f, c) -> termination.set(f != null))
                .on().request(requests::set)
                .on().cancellation(() -> cancellation.set(true))
                .subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasFailedWith(IOException.class, "boom");

        assertThat(subscription.get()).isNotNull();
        assertThat(item.get()).isNull();
        assertThat(failure.get()).isNull();
        assertThat(completion.get()).isFalse();
        assertThat(termination.get()).isTrue();
        assertThat(requests.get()).isEqualTo(0);
        assertThat(cancellation.get()).isFalse();
    }

    @Test
    public void testCallbacksOnFailureWhenPredicateThrowsAnException() {
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicLong requests = new AtomicLong();
        AtomicBoolean termination = new AtomicBoolean();
        AtomicBoolean cancellation = new AtomicBoolean();

        Predicate<? super Throwable> boom = f -> {
            throw new IllegalStateException("bigboom");
        };

        Multi.createFrom().<Integer> failure(new IOException("smallboom"))
                .on().subscribed(subscription::set)
                .on().item().invoke(item::set)
                .on().failure(boom).invoke(failure::set)
                .on().completion(() -> completion.set(true))
                .on().termination((f, c) -> termination.set(f != null))
                .on().request(requests::set)
                .on().cancellation(() -> cancellation.set(true))
                .subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasFailedWith(CompositeException.class, "bigboom")
                .assertHasFailedWith(CompositeException.class, "smallboom");

        assertThat(subscription.get()).isNotNull();
        assertThat(item.get()).isNull();
        assertThat(failure.get()).isNull();
        assertThat(completion.get()).isFalse();
        assertThat(termination.get()).isTrue();
        assertThat(requests.get()).isEqualTo(0);
        assertThat(cancellation.get()).isFalse();
    }

    @Test
    public void testCallbacksOnCompletion() {
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicLong requests = new AtomicLong();
        AtomicBoolean termination = new AtomicBoolean();
        AtomicBoolean termination2 = new AtomicBoolean();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().<Integer> empty()
                .on().subscribed(subscription::set)
                .on().item().invoke(item::set)
                .on().failure().invoke(failure::set)
                .on().completion(() -> completion.set(true))
                .on().termination((f, c) -> termination.set(f == null && !c))
                .on().termination(() -> termination2.set(true))
                .on().request(requests::set)
                .on().cancellation(() -> cancellation.set(true))
                .subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();

        assertThat(subscription.get()).isNotNull();
        assertThat(item.get()).isNull();
        assertThat(failure.get()).isNull();
        assertThat(completion.get()).isTrue();
        assertThat(termination.get()).isTrue();
        assertThat(termination2.get()).isTrue();
        assertThat(requests.get()).isEqualTo(0);
        assertThat(cancellation.get()).isFalse();
    }

    @Test
    public void testWithNoEvents() {
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicLong requests = new AtomicLong();
        AtomicBoolean termination = new AtomicBoolean();
        AtomicBoolean termination2 = new AtomicBoolean();
        AtomicBoolean cancellation = new AtomicBoolean();

        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> nothing()
                .on().subscribed(subscription::set)
                .on().item().invoke(item::set)
                .on().failure().invoke(failure::set)
                .on().completion(() -> completion.set(true))
                .on().termination((f, c) -> termination.set(f == null && c))
                .on().termination(() -> termination2.set(true))
                .on().request(requests::set)
                .on().cancellation(() -> cancellation.set(true))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        assertThat(subscription.get()).isNotNull();
        assertThat(item.get()).isNull();
        assertThat(failure.get()).isNull();
        assertThat(completion.get()).isFalse();
        assertThat(termination.get()).isFalse();
        assertThat(termination2.get()).isFalse();
        assertThat(requests.get()).isEqualTo(10);
        assertThat(cancellation.get()).isFalse();

        subscriber.cancel();
        assertThat(termination.get()).isTrue();
        assertThat(termination2.get()).isTrue();
        assertThat(cancellation.get()).isTrue();
    }

    @Test
    public void testWhenOnItemPeekThrowsExceptions() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create(1);

        Multi.createFrom().item(1)
                .on().item().invoke(i -> {
                    throw new IllegalArgumentException("boom");
                })
                .subscribe().withSubscriber(ts)
                .assertTerminated()
                .assertHasFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testWhenOnFailurePeekThrowsExceptions() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create(1);

        Multi.createFrom().<Integer> failure(new IOException("source"))
                .on().failure().invoke(f -> {
                    throw new IllegalArgumentException("boom");
                })
                .subscribe().withSubscriber(ts)
                .assertTerminated()
                .assertHasFailedWith(CompositeException.class, "boom")
                .assertHasFailedWith(CompositeException.class, "source");
    }

    @Test
    public void testWhenOnCompletionPeekThrowsExceptions() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create(1);

        Multi.createFrom().items(1, 2)
                .on().completion(() -> {
                    throw new IllegalArgumentException("boom");
                })
                .subscribe().withSubscriber(ts)
                .assertNotTerminated()
                .assertReceived(1)
                .request(1)
                .assertTerminated()
                .assertHasFailedWith(IllegalArgumentException.class, "boom")
                .assertReceived(1, 2);
    }

    @Test
    public void testWhenBothOnItemAndOnFailureThrowsException() {
        Multi.createFrom().item(1)
                .on().item().invoke(i -> {
                    throw new IllegalArgumentException("boom1");
                }).on().failure().invoke(t -> {
                    throw new IllegalArgumentException("boom2");
                }).subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertTerminated()
                .assertHasFailedWith(CompositeException.class, "boom1")
                .assertHasFailedWith(CompositeException.class, "boom2");
    }

    @Test
    public void testThatAFailureInTerminationDoesNotRunTerminationTwice() {
        AtomicInteger called = new AtomicInteger();
        Multi.createFrom().item(1)
                .on().termination((f, c) -> {
                    called.incrementAndGet();
                    throw new IllegalArgumentException("boom");
                }).subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertHasFailedWith(IllegalArgumentException.class, "boom");

        assertThat(called).hasValue(1);
    }

    @Test
    public void testThatAFailureInTerminationAfterAFailureDoesNotRunTerminationTwice() {
        AtomicInteger called = new AtomicInteger();
        Multi.createFrom().<Integer> failure(new IOException("IO"))
                .on().termination((f, c) -> {
                    called.incrementAndGet();
                    throw new IllegalArgumentException("boom");
                }).subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertHasFailedWith(CompositeException.class, "boom")
                .assertHasFailedWith(CompositeException.class, "IO");

        assertThat(called).hasValue(1);
    }

    @Test
    public void testThatAFailureInTerminationDoesNotRunTerminationTwice2() {
        AtomicInteger called = new AtomicInteger();
        Multi.createFrom().item(1)
                .on().termination(() -> {
                    called.incrementAndGet();
                    throw new IllegalArgumentException("boom");
                }).subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertHasFailedWith(IllegalArgumentException.class, "boom");

        assertThat(called).hasValue(1);
    }

    @Test
    public void testThatTerminationIsNotCalledOnCancellationAfterCompletion() {
        AtomicInteger invocations = new AtomicInteger(0);
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        Multi<Integer> multi = Multi.createFrom().publisher(processor)
                .on().termination(invocations::incrementAndGet);

        MultiAssertSubscriber<Integer> subscriber = multi
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10));
        assertThat(invocations).hasValue(0);
        processor.onNext(1);
        assertThat(invocations).hasValue(0);
        processor.onComplete();
        subscriber.assertCompletedSuccessfully().assertReceived(1);
        assertThat(invocations).hasValue(1);
        subscriber.cancel();
        assertThat(invocations).hasValue(1);
        subscriber.cancel();
        assertThat(invocations).hasValue(1);
    }

    @Test
    public void testThatTerminationIsNotCalledOnCancellationAfterFailure() {
        AtomicInteger invocations = new AtomicInteger(0);
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        Multi<Integer> multi = Multi.createFrom().publisher(processor)
                .on().termination(invocations::incrementAndGet);
        MultiAssertSubscriber<Integer> subscriber = multi
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10));
        assertThat(invocations).hasValue(0);
        processor.onNext(1);
        assertThat(invocations).hasValue(0);
        processor.onError(new Exception("boom"));
        subscriber.assertHasFailedWith(Exception.class, "boom").assertReceived(1);
        assertThat(invocations).hasValue(1);
        subscriber.cancel();
        assertThat(invocations).hasValue(1);
        subscriber.cancel();
        assertThat(invocations).hasValue(1);
    }

    @Test
    public void testThatTerminationIsNotCalledOnCompletionAfterCancellation() {
        AtomicInteger invocations = new AtomicInteger(0);
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        Multi<Integer> multi = Multi.createFrom().publisher(processor)
                .on().termination(invocations::incrementAndGet);
        MultiAssertSubscriber<Integer> subscriber = multi
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10));
        assertThat(invocations).hasValue(0);
        processor.onNext(1);
        assertThat(invocations).hasValue(0);
        subscriber.cancel();
        assertThat(invocations).hasValue(1);
        processor.onComplete();
        subscriber.assertNotTerminated();
        assertThat(invocations).hasValue(1);
        subscriber.cancel();
        assertThat(invocations).hasValue(1);
    }

    @Test
    public void testThatTerminationIsNotCalledOnFailureAfterCancellation() {
        AtomicInteger invocations = new AtomicInteger(0);
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        Multi<Integer> multi = Multi.createFrom().publisher(processor)
                .on().termination(invocations::incrementAndGet);
        MultiAssertSubscriber<Integer> subscriber = multi
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10));
        assertThat(invocations).hasValue(0);
        processor.onNext(1);
        subscriber.cancel();
        assertThat(invocations).hasValue(1);
        processor.onError(new Exception("boom"));
        subscriber.assertNotTerminated().assertReceived(1);
        assertThat(invocations).hasValue(1);
        subscriber.cancel();
        assertThat(invocations).hasValue(1);
    }

}
