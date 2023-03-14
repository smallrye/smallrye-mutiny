package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.subscription.Cancellable;

public class MultiOnEventTest {

    @Test
    public void testCallbacksWhenItemIsEmitted() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicBoolean invokedOnItemRunnable = new AtomicBoolean();
        AtomicBoolean invokedOnItemSupplier = new AtomicBoolean();
        AtomicBoolean calledOnItemSupplier = new AtomicBoolean();
        AtomicLong requests = new AtomicLong();
        AtomicBoolean termination = new AtomicBoolean();
        AtomicBoolean termination2 = new AtomicBoolean();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().item(1)
                .onSubscription().invoke(subscription::set)
                .onItem().invoke(item::set)
                .onFailure().invoke(failure::set)
                .onItem().invoke(() -> invokedOnItemRunnable.set(true))
                .onItem().call(() -> {
                    calledOnItemSupplier.set(true);
                    return Uni.createFrom().item("yo");
                })
                .onItem().call(ignored -> {
                    invokedOnItemSupplier.set(true);
                    return Uni.createFrom().item("yo");
                })
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f == null && !c))
                .onTermination().invoke(() -> termination2.set(true))
                .onRequest().invoke(requests::set)
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe(subscriber);

        subscriber
                .request(20)
                .assertCompleted()
                .assertItems(1);

        assertThat(subscription.get()).isNotNull();
        assertThat(item.get()).isEqualTo(1);
        assertThat(failure.get()).isNull();
        assertThat(completion.get()).isTrue();
        assertThat(invokedOnItemRunnable.get()).isTrue();
        assertThat(invokedOnItemSupplier.get()).isTrue();
        assertThat(calledOnItemSupplier.get()).isTrue();
        assertThat(termination.get()).isTrue();
        assertThat(termination2.get()).isTrue();
        assertThat(requests.get()).isEqualTo(20);
        assertThat(cancellation.get()).isFalse();
    }

    @Test
    public void testCallbacksWhenItemIsEmittedUsingOnAndThenGroup() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicLong requests = new AtomicLong();
        AtomicBoolean termination = new AtomicBoolean();
        AtomicBoolean termination2 = new AtomicBoolean();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().item(1)
                .onSubscription().invoke(subscription::set)
                .onItem().invoke(item::set)
                .onFailure().invoke(failure::set)
                .onTermination().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f == null && !c))
                .onTermination().invoke(() -> termination2.set(true))
                .onRequest().invoke(requests::set)
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe(subscriber);

        subscriber
                .request(20)
                .assertCompleted()
                .assertItems(1);

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
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicLong requests = new AtomicLong();
        AtomicBoolean termination = new AtomicBoolean();
        AtomicBoolean termination2 = new AtomicBoolean();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().item(1)
                .onSubscription().invoke(subscription::set)
                .onItem().invoke(item::set)
                .onFailure().invoke(failure::set)
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f == null && !c))
                .onTermination().invoke(() -> termination2.set(true))
                .onRequest().invoke(requests::set)
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe(subscriber);

        subscriber
                .request(20)
                .assertCompleted()
                .assertItems(1);

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
                .onSubscription().invoke(subscription::set)
                .onItem().invoke(item::set)
                .onFailure().invoke(failure::set)
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f != null))
                .onTermination().invoke(() -> termination2.set(true))
                .onRequest().invoke(requests::set)
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe().withSubscriber(AssertSubscriber.create())
                .assertFailedWith(IOException.class, "boom");

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
                .onSubscription().invoke(subscription::set)
                .onItem().invoke(item::set)
                .onFailure(IOException.class).invoke(failure::set)
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f != null))
                .onRequest().invoke(requests::set)
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe().withSubscriber(AssertSubscriber.create())
                .assertFailedWith(IOException.class, "boom");

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
                .onSubscription().invoke(subscription::set)
                .onItem().invoke(item::set)
                .onFailure(f -> f.getMessage().contains("missing")).invoke(failure::set)
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f != null))
                .onRequest().invoke(requests::set)
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe().withSubscriber(AssertSubscriber.create())
                .assertFailedWith(IOException.class, "boom");

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
                .onSubscription().invoke(subscription::set)
                .onItem().invoke(item::set)
                .onFailure(boom).invoke(failure::set)
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f != null))
                .onRequest().invoke(requests::set)
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe().withSubscriber(AssertSubscriber.create())
                .assertFailedWith(CompositeException.class, "bigboom")
                .assertFailedWith(CompositeException.class, "smallboom");

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
                .onSubscription().invoke(subscription::set)
                .onItem().invoke(item::set)
                .onFailure().invoke(failure::set)
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f == null && !c))
                .onTermination().invoke(() -> termination2.set(true))
                .onRequest().invoke(requests::set)
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe().withSubscriber(AssertSubscriber.create())
                .assertCompleted()
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

        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> nothing()
                .onSubscription().invoke(subscription::set)
                .onItem().invoke(item::set)
                .onFailure().invoke(failure::set)
                .onCompletion().invoke(() -> completion.set(true))
                .onTermination().invoke((f, c) -> termination.set(f == null && c))
                .onTermination().invoke(() -> termination2.set(true))
                .onRequest().invoke(requests::set)
                .onCancellation().invoke(() -> cancellation.set(true))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
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
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);

        Multi.createFrom().item(1)
                .onItem().invoke(i -> {
                    throw new IllegalArgumentException("boom");
                })
                .subscribe().withSubscriber(subscriber)
                .assertTerminated()
                .assertFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testWhenOnFailurePeekThrowsExceptions() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);

        Multi.createFrom().<Integer> failure(new IOException("source"))
                .onFailure().invoke(f -> {
                    throw new IllegalArgumentException("boom");
                })
                .subscribe().withSubscriber(subscriber)
                .assertTerminated()
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "source");
    }

    @Test
    public void testWhenOnCompletionPeekThrowsExceptions() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);

        Multi.createFrom().items(1, 2)
                .onCompletion().invoke(() -> {
                    throw new IllegalArgumentException("boom");
                })
                .subscribe().withSubscriber(subscriber)
                .assertNotTerminated()
                .assertItems(1)
                .request(1)
                .assertTerminated()
                .assertFailedWith(IllegalArgumentException.class, "boom")
                .assertItems(1, 2);
    }

    @Test
    public void testWhenBothOnItemAndOnFailureThrowsException() {
        Multi.createFrom().item(1)
                .onItem().invoke(i -> {
                    throw new IllegalArgumentException("boom1");
                }).onFailure().invoke(t -> {
                    throw new IllegalArgumentException("boom2");
                }).subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertTerminated()
                .assertFailedWith(CompositeException.class, "boom1")
                .assertFailedWith(CompositeException.class, "boom2");
    }

    @Test
    public void testThatAFailureInTerminationDoesNotRunTerminationTwice() {
        AtomicInteger called = new AtomicInteger();
        Multi.createFrom().item(1)
                .onTermination().invoke((f, c) -> {
                    called.incrementAndGet();
                    throw new IllegalArgumentException("boom");
                }).subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(IllegalArgumentException.class, "boom");

        assertThat(called).hasValue(1);
    }

    @Test
    public void testThatAFailureInTerminationAfterAFailureDoesNotRunTerminationTwice() {
        AtomicInteger called = new AtomicInteger();
        Multi.createFrom().<Integer> failure(new IOException("IO"))
                .onTermination().invoke((f, c) -> {
                    called.incrementAndGet();
                    throw new IllegalArgumentException("boom");
                }).subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "IO");

        assertThat(called).hasValue(1);
    }

    @Test
    public void testThatAFailureInTerminationDoesNotRunTerminationTwice2() {
        AtomicInteger called = new AtomicInteger();
        Multi.createFrom().item(1)
                .onTermination().invoke(() -> {
                    called.incrementAndGet();
                    throw new IllegalArgumentException("boom");
                }).subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(IllegalArgumentException.class, "boom");

        assertThat(called).hasValue(1);
    }

    @Test
    public void testThatTerminationIsNotCalledOnCancellationAfterCompletion() {
        AtomicInteger invocations = new AtomicInteger(0);
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        Multi<Integer> multi = Multi.createFrom().publisher(processor)
                .onTermination().invoke(invocations::incrementAndGet);

        AssertSubscriber<Integer> subscriber = multi
                .subscribe().withSubscriber(AssertSubscriber.create(10));
        assertThat(invocations).hasValue(0);
        processor.onNext(1);
        assertThat(invocations).hasValue(0);
        processor.onComplete();
        subscriber.assertCompleted().assertItems(1);
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
        AssertSubscriber<Integer> subscriber = multi
                .subscribe().withSubscriber(AssertSubscriber.create(10));
        assertThat(invocations).hasValue(0);
        processor.onNext(1);
        assertThat(invocations).hasValue(0);
        processor.onError(new Exception("boom"));
        subscriber.assertFailedWith(Exception.class, "boom").assertItems(1);
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
        AssertSubscriber<Integer> subscriber = multi
                .subscribe().withSubscriber(AssertSubscriber.create(10));
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
        AssertSubscriber<Integer> subscriber = multi
                .subscribe().withSubscriber(AssertSubscriber.create(10));
        assertThat(invocations).hasValue(0);
        processor.onNext(1);
        subscriber.cancel();
        assertThat(invocations).hasValue(1);
        processor.onError(new Exception("boom"));
        subscriber.assertNotTerminated().assertItems(1);
        assertThat(invocations).hasValue(1);
        subscriber.cancel();
        assertThat(invocations).hasValue(1);
    }

    @Test
    public void testThatPredicateFailureProduceCompositeException() {
        AtomicBoolean called = new AtomicBoolean();
        AssertSubscriber<Object> subscriber = Multi.createFrom().failure(new IOException("boom"))
                .onFailure(t -> {
                    throw new NullPointerException();
                }).invoke(t -> called.set(true))
                .subscribe().withSubscriber(AssertSubscriber.create(1));

        subscriber.assertFailedWith(CompositeException.class, "boom");
        CompositeException failure = (CompositeException) subscriber.getFailure();
        assertThat(failure.getCauses()).hasSize(2);
        assertThat(called).isFalse();
    }

    private final Multi<Integer> numbers = Multi.createFrom().items(1, 2);
    private final Multi<Integer> failed = Multi.createBy().concatenating()
            .streams(numbers, Multi.createFrom().failure(new IOException("boom")));
    private final Uni<Void> sub = Uni.createFrom().nullItem();

    @Test
    public void testCallOnItem() {
        AtomicInteger res = new AtomicInteger();
        AtomicInteger twoGotCalled = new AtomicInteger();

        List<Integer> r = numbers.onItem().call(i -> {
            res.set(i);
            return sub.onItem().invoke(c -> twoGotCalled.incrementAndGet());
        })
                .collect().asList().await().indefinitely();

        assertThat(r).containsExactly(1, 2);
        assertThat(twoGotCalled).hasValue(2);
        assertThat(res).hasValue(2);
    }

    @Test
    public void testCallOnItemWithShortcut() {
        AtomicInteger res = new AtomicInteger();
        AtomicInteger twoGotCalled = new AtomicInteger();

        List<Integer> r = numbers.call(i -> {
            res.set(i);
            return sub.invoke(c -> twoGotCalled.incrementAndGet());
        })
                .collect().asList().await().indefinitely();

        assertThat(r).containsExactly(1, 2);
        assertThat(twoGotCalled).hasValue(2);
        assertThat(res).hasValue(2);
    }

    @Test
    public void testCallOnFailure() {
        AtomicInteger res = new AtomicInteger(-1);
        AtomicInteger twoGotCalled = new AtomicInteger();

        assertThatThrownBy(() -> failed.onItem().call(
                i -> {
                    res.set(i);
                    return sub.onItem().invoke(c -> twoGotCalled.incrementAndGet());
                })
                .collect().asList().await().indefinitely())
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
        assertThatThrownBy(() -> more.onItem().call(i -> {
            res.set(i);
            if (i == 2) {
                throw new RuntimeException("boom");
            }
            return Uni.createFrom().nullItem();
        })
                .collect().asList()
                .await().indefinitely()).isInstanceOf(RuntimeException.class).hasMessageContaining("boom");

        assertThat(res).hasValue(2);
    }

    @Test
    public void testNullReturnedByAsyncCallback() {
        AtomicInteger res = new AtomicInteger();
        Multi<Integer> more = Multi.createFrom().items(1, 2, 3);
        assertThatThrownBy(() -> more.onItem().call(i -> {
            res.set(i);
            if (i == 2) {
                return null;
            }
            return Uni.createFrom().nullItem();
        })
                .collect().asList()
                .await().indefinitely()).isInstanceOf(NullPointerException.class).hasMessageContaining("null");
        assertThat(res).hasValue(2);
    }

    @Test
    public void testCallWithSubFailure() {
        AtomicInteger res = new AtomicInteger(-1);
        AtomicInteger twoGotCalled = new AtomicInteger(-1);
        Multi<Integer> more = Multi.createFrom().items(1, 2, 3);
        Uni<Integer> failing = Uni.createFrom().item(23).onItem().invoke(twoGotCalled::set).onItem()
                .failWith(k -> new IllegalStateException("boom-" + k));

        assertThatThrownBy(() -> more.onItem().call(i -> {
            res.set(i);
            if (i == 2) {
                return failing;
            }
            return Uni.createFrom().nullItem();
        })
                .collect().asList()
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
                .onItem().call(i -> uni).subscribe().with(result::set);

        cancellable.cancel();
        assertThat(result).hasValue(0);
        assertThat(terminated).isTrue();
    }

    @Test
    public void testInvokeOnItemWithShortcut() {
        AtomicInteger res = new AtomicInteger();

        List<Integer> r = numbers.invoke(res::set)
                .collect().asList().await().indefinitely();

        assertThat(r).containsExactly(1, 2);
        assertThat(res).hasValue(2);
    }

    @Test
    public void testInvokeOnItemWithRunnableShortcut() {
        AtomicInteger called = new AtomicInteger();

        List<Integer> r = numbers.invoke(called::incrementAndGet)
                .collect().asList().await().indefinitely();

        assertThat(r).containsExactly(1, 2);
        assertThat(called).hasValue(2);
    }

    @Test
    public void testThatInvokeConsumerMustNotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().item(1).onItem().invoke((Consumer<? super Integer>) null));
    }

    @Test
    public void testThatCallMapperMustNotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().item(1).onItem().call((Function<? super Integer, Uni<?>>) null));
    }

    @Test
    public void testThatInvokeConsumerMustNotBeNullWithShortcut() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().item(1).invoke((Consumer<? super Integer>) null));
    }

    @Test
    public void testThatCallMapperMustNotBeNullWithShortcut() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().item(1).call((Function<? super Integer, Uni<?>>) null));
    }

}
