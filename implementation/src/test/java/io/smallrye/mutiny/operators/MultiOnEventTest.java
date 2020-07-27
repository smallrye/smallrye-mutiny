package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

@SuppressWarnings("deprecation")
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
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f == null && !c))
                .onTermination().invoke(() -> termination2.set(true))
                .on().request(requests::set)
                .onCancellation().invoke(() -> cancellation.set(true))
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
    public void testCallbacksWhenItemIsEmittedUsingOnAndThenGroup() {
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
                .on().subscribe().invoke(subscription::set)
                .on().item().invoke(item::set)
                .on().failure().invoke(failure::set)
                .on().termination().invoke(() -> completion.set(true))
                .on().termination().invoke((f, c) -> termination.set(f == null && !c))
                .on().termination().invoke(() -> termination2.set(true))
                .on().request(requests::set)
                .on().cancellation().invoke(() -> cancellation.set(true))
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
    public void testCallbacksWhenItemIsEmittedWithDeprecatedApis() {
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
                .onTermination().invoke((f, c) -> termination.set(f == null && !c))
                .onTermination().invoke(() -> termination2.set(true))
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
    public void testCallbacksWhenItemIsEmittedWithDeprecatedOnTermination() {
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
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f != null))
                .onTermination().invoke(() -> termination2.set(true))
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
    public void testCallbacksOnFailureWithDeprecatedOnTermination() {
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
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f != null))
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
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f != null))
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
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f != null))
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
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f == null && !c))
                .onTermination().invoke(() -> termination2.set(true))
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
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f == null && c))
                .onTermination().invoke(() -> termination2.set(true))
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
                .onCompletion().invoke(() -> {
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
                .onTermination().invoke((f, c) -> {
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
                .onTermination().invoke((f, c) -> {
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
                .onTermination().invoke(() -> {
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
                .onTermination().invoke(invocations::incrementAndGet);

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
                .onTermination().invoke(invocations::incrementAndGet);
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
                .onTermination().invoke(invocations::incrementAndGet);
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
                .onTermination().invoke(invocations::incrementAndGet);
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

    @Test
    public void testThatPredicateFailureProduceCompositeException() {
        AtomicBoolean called = new AtomicBoolean();
        MultiAssertSubscriber<Object> subscriber = Multi.createFrom().failure(new IOException("boom"))
                .onFailure(t -> {
                    throw new NullPointerException();
                }).invoke(t -> called.set(true))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(CompositeException.class, "boom");
        CompositeException failure = (CompositeException) subscriber.failures().get(0);
        assertThat(failure.getCauses()).hasSize(2);
        assertThat(called).isFalse();
    }

    private final Multi<Integer> numbers = Multi.createFrom().items(1, 2);
    private final Multi<Integer> failed = Multi.createBy().concatenating()
            .streams(numbers, Multi.createFrom().failure(new IOException("boom")));
    private final Uni<Void> sub = Uni.createFrom().nullItem();

    @Test
    public void testInvokeUniOnItem() {
        AtomicInteger res = new AtomicInteger();
        AtomicInteger twoGotCalled = new AtomicInteger();

        List<Integer> r = numbers.onItem().invokeUni(i -> {
            res.set(i);
            return sub.onItem().invoke(c -> twoGotCalled.incrementAndGet());
        })
                .collectItems().asList().await().indefinitely();

        assertThat(r).containsExactly(1, 2);
        assertThat(twoGotCalled).hasValue(2);
        assertThat(res).hasValue(2);
    }

    @Test
    public void testInvokeUniOnItemWithShortcut() {
        AtomicInteger res = new AtomicInteger();
        AtomicInteger twoGotCalled = new AtomicInteger();

        List<Integer> r = numbers.invokeUni(i -> {
            res.set(i);
            return sub.invoke(c -> twoGotCalled.incrementAndGet());
        })
                .collectItems().asList().await().indefinitely();

        assertThat(r).containsExactly(1, 2);
        assertThat(twoGotCalled).hasValue(2);
        assertThat(res).hasValue(2);
    }

    @Test
    public void testInvokeUniOnFailure() {
        AtomicInteger res = new AtomicInteger(-1);
        AtomicInteger twoGotCalled = new AtomicInteger();

        assertThatThrownBy(() -> failed.onItem().invokeUni(
                i -> {
                    res.set(i);
                    return sub.onItem().invoke(c -> twoGotCalled.incrementAndGet());
                })
                .collectItems().asList().await().indefinitely())
                        .isInstanceOf(CompletionException.class)
                        .hasCauseInstanceOf(IOException.class)
                        .hasMessageContaining("boom");

        assertThat(twoGotCalled).hasValue(2);
        assertThat(res).hasValue(2);
    }

    @Test
    public void testFailureInAsyncCallback() {
        AtomicInteger res = new AtomicInteger();
        Multi<Integer> more = Multi.createFrom().items(1, 2, 3);
        assertThatThrownBy(() -> more.onItem().invokeUni(i -> {
            res.set(i);
            if (i == 2) {
                throw new RuntimeException("boom");
            }
            return Uni.createFrom().nullItem();
        })
                .collectItems().asList()
                .await().indefinitely()).isInstanceOf(RuntimeException.class).hasMessageContaining("boom");

        assertThat(res).hasValue(2);
    }

    @Test
    public void testNullReturnedByAsyncCallback() {
        AtomicInteger res = new AtomicInteger();
        Multi<Integer> more = Multi.createFrom().items(1, 2, 3);
        assertThatThrownBy(() -> more.onItem().invokeUni(i -> {
            res.set(i);
            if (i == 2) {
                return null;
            }
            return Uni.createFrom().nullItem();
        })
                .collectItems().asList()
                .await().indefinitely()).isInstanceOf(NullPointerException.class).hasMessageContaining("null");
        assertThat(res).hasValue(2);
    }

    @Test
    public void testInvokeUniWithSubFailure() {
        AtomicInteger res = new AtomicInteger(-1);
        AtomicInteger twoGotCalled = new AtomicInteger(-1);
        Multi<Integer> more = Multi.createFrom().items(1, 2, 3);
        Uni<Integer> failing = Uni.createFrom().item(23).onItem().invoke(twoGotCalled::set).onItem()
                .failWith(k -> new IllegalStateException("boom-" + k));

        assertThatThrownBy(() -> more.onItem().invokeUni(i -> {
            res.set(i);
            if (i == 2) {
                return failing;
            }
            return Uni.createFrom().nullItem();
        })
                .collectItems().asList()
                .await().indefinitely()).isInstanceOf(IllegalStateException.class).hasMessageContaining("boom-2");

        assertThat(twoGotCalled).hasValue(23);
        assertThat(res).hasValue(2);
    }

    @Test
    public void testCancellationBeforeActionCompletes() {
        AtomicBoolean terminated = new AtomicBoolean();
        Uni<Object> uni = Uni.createFrom().emitter(e -> e.onTermination(() -> terminated.set(true)));

        AtomicInteger result = new AtomicInteger();
        Cancellable cancellable = numbers
                .onItem().invokeUni(i -> uni).subscribe().with(result::set);

        cancellable.cancel();
        assertThat(result).hasValue(0);
        //noinspection ConstantConditions
        assertThat(terminated).isTrue();
    }

    @Test
    public void testInvokeOnItemWithShortcut() {
        AtomicInteger res = new AtomicInteger();

        List<Integer> r = numbers.invoke(res::set)
                .collectItems().asList().await().indefinitely();

        assertThat(r).containsExactly(1, 2);
        assertThat(res).hasValue(2);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatInvokeConsumerMustNotBeNull() {
        Multi.createFrom().item(1).onItem().invoke(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatInvokeUniMapperMustNotBeNull() {
        Multi.createFrom().item(1).onItem().invokeUni(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatInvokeConsumerMustNotBeNullWithShortcut() {
        Multi.createFrom().item(1).invoke(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatInvokeUniMapperMustNotBeNullWithShortcut() {
        Multi.createFrom().item(1).invokeUni(null);
    }

}
