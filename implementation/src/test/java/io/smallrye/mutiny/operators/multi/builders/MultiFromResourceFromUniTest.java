package io.smallrye.mutiny.operators.multi.builders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.spies.Spy;
import io.smallrye.mutiny.helpers.spies.UniOnCancellationSpy;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class MultiFromResourceFromUniTest {

    @Test
    public void testWithResourceSupplierThrowingException() {
        Supplier<Uni<String>> supplier = () -> {
            throw new IllegalArgumentException("boom");
        };
        Multi<String> multi = Multi.createFrom().resourceFromUni(supplier,
                s -> Multi.createFrom().item(s))
                .withFinalizer(r -> {
                });
        AssertSubscriber<String> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create(10));
        subscriber
                .assertFailedWith(IllegalArgumentException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithResourceSupplierEmittingFailure() {
        Supplier<Uni<String>> supplier = () -> Uni.createFrom().failure(new IllegalArgumentException("boom"));
        Multi<String> multi = Multi.createFrom().resourceFromUni(supplier,
                s -> Multi.createFrom().item(s))
                .withFinalizer(r -> {
                });
        AssertSubscriber<String> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create(10));
        subscriber
                .assertFailedWith(IllegalArgumentException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithResourceSupplierProducingNull() {
        Supplier<Uni<String>> supplier = () -> null;
        Multi<String> multi = Multi.createFrom().resourceFromUni(supplier,
                s -> Multi.createFrom().items(s))
                .withFinalizer(r -> {
                });
        AssertSubscriber<String> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create(10));
        subscriber
                .assertFailedWith(IllegalArgumentException.class, "")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithNullAsResourceSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().resourceFromUni(null,
                s -> Multi.createFrom().items(s))
                .withFinalizer(r -> {
                }));
    }

    @Test
    public void testWithStreamSupplierThrowingException() {
        Supplier<Uni<String>> supplier = () -> Uni.createFrom().item("Hello");
        Function<String, Publisher<String>> stream = s -> {
            throw new IllegalArgumentException("boom");
        };
        Multi<String> multi = Multi.createFrom().resourceFromUni(supplier, stream)
                .withFinalizer(r -> {
                });
        AssertSubscriber<String> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create(10));
        subscriber
                .assertFailedWith(IllegalArgumentException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithStreamSupplierProducingNull() {
        Supplier<Uni<String>> supplier = () -> null;
        Multi<String> multi = Multi.createFrom().resourceFromUni(supplier,
                s -> (Publisher<String>) null)
                .withFinalizer(r -> {
                });
        AssertSubscriber<String> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create(10));
        subscriber
                .assertFailedWith(IllegalArgumentException.class, "")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithNullAsStreamSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom()
                .resourceFromUni(() -> Uni.createFrom().item("hello"), null)
                .withFinalizer(r -> {
                }));
    }

    @Test
    public void testWithNullAsFinalizer() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().resourceFromUni(() -> Uni.createFrom().item("hello"), null)
                        .withFinalizer((Consumer<String>) null));
    }

    @Test
    public void testWithNullAsFinalizer2() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().resourceFromUni(() -> Uni.createFrom().item("hello"), null)
                        .withFinalizer((Function<String, Uni<Void>>) null));
    }

    @Test
    public void testWithNullAsFinalizer3() {
        assertThrows(IllegalArgumentException.class, () -> {
            Function<String, Uni<Void>> function = s -> Uni.createFrom().item(() -> null);
            Multi.createFrom().resourceFromUni(() -> Uni.createFrom().item("hello"), null)
                    .withFinalizer(function, null, function);
        });
    }

    @Test
    public void testWithNullAsFinalizer4() {
        assertThrows(IllegalArgumentException.class, () -> {
            Function<String, Uni<Void>> function = s -> Uni.createFrom().item(() -> null);
            BiFunction<String, Throwable, Uni<Void>> onFailure = (s, f) -> Uni.createFrom().item(() -> null);
            Multi.createFrom().resourceFromUni(() -> Uni.createFrom().item("hello"), null)
                    .withFinalizer(null, onFailure, function);
        });
    }

    @Test
    public void testWithNullAsFinalizer5() {
        assertThrows(IllegalArgumentException.class, () -> {
            Function<String, Uni<Void>> function = s -> Uni.createFrom().item(() -> null);
            BiFunction<String, Throwable, Uni<Void>> onFailure = (s, f) -> Uni.createFrom().item(() -> null);
            Multi.createFrom().resourceFromUni(() -> Uni.createFrom().item("hello"), null)
                    .withFinalizer(function, onFailure, null);
        });
    }

    @Test
    public void simpleSynchronousTest() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);
        AtomicInteger cleanup = new AtomicInteger();
        Multi.createFrom().resourceFromUni(() -> Uni.createFrom().item(1), r -> Multi.createFrom().range(r, 11))
                .withFinalizer(cleanup::set)
                .subscribe(subscriber);
        subscriber
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .assertCompleted();
        assertThat(cleanup.get()).isEqualTo(1);
    }

    @Test
    public void simpleSynchronousTestWithMultipleSubscribers() {
        AssertSubscriber<Integer> subscriber1 = AssertSubscriber.create(10);
        AssertSubscriber<Integer> subscriber2 = AssertSubscriber.create(10);
        List<Integer> list = new ArrayList<>();
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().resourceFromUni(() -> Uni.createFrom().item(count::incrementAndGet),
                r -> Multi.createFrom().range(r, 11))
                .withFinalizer((Consumer<Integer>) list::add);
        multi.subscribe(subscriber1);
        multi.subscribe(subscriber2);

        subscriber1
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .assertCompleted();
        subscriber2
                .assertItems(2, 3, 4, 5, 6, 7, 8, 9, 10)
                .assertCompleted();
        assertThat(list).containsExactly(1, 2);
    }

    @Test
    public void testCleanupCalledOnCompletionWithSynchronousFinalizer() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(9);
        AtomicInteger cleanup = new AtomicInteger();
        Multi.createFrom().resourceFromUni(() -> Uni.createFrom().item(1), r -> Multi.createFrom().range(r, 11))
                .withFinalizer(cleanup::set)
                .subscribe(subscriber);
        subscriber
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .run(() -> assertThat(cleanup).hasValue(0))
                .request(1)
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .run(() -> assertThat(cleanup).hasValue(1))
                .assertCompleted();
    }

    @Test
    public void testCleanupCalledOnCancellationWithSynchronousFinalizer() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(4);
        AtomicInteger cleanup = new AtomicInteger();
        Multi.createFrom().resourceFromUni(() -> Uni.createFrom().item(1), r -> Multi.createFrom().range(r, 11))
                .withFinalizer(cleanup::set)
                .subscribe(subscriber);
        subscriber
                .assertItems(1, 2, 3, 4)
                .run(() -> assertThat(cleanup).hasValue(0))
                .cancel()
                .assertItems(1, 2, 3, 4)
                .run(() -> assertThat(cleanup).hasValue(1))
                .assertNotTerminated();
    }

    @Test
    public void testCleanupCalledOnFailureWithSynchronousFinalizer() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);
        AtomicInteger cleanup = new AtomicInteger();
        Multi.createFrom()
                .resourceFromUni(() -> Uni.createFrom().item(1), r -> Multi.createFrom().<Integer> emitter(
                        e -> e.emit(1).emit(2).fail(new IOException("boom"))))
                .withFinalizer(cleanup::set)
                .subscribe(subscriber);
        subscriber
                .assertItems(1)
                .run(() -> assertThat(cleanup).hasValue(0))
                .request(3)
                .assertItems(1, 2)
                .run(() -> assertThat(cleanup).hasValue(1))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testThatFinalizerIsNotCalledWhenResourceSupplierThrowsAnException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);
        Supplier<Uni<Integer>> supplier = () -> {
            throw new IllegalArgumentException("boom");
        };
        AtomicInteger cleanup = new AtomicInteger();
        Multi.createFrom()
                .resourceFromUni(supplier,
                        r -> Multi.createFrom().<Integer> emitter(e -> e.emit(1).emit(2).fail(new IOException("boom"))))
                .withFinalizer(cleanup::set)
                .subscribe(subscriber);
        subscriber
                .assertFailedWith(IllegalArgumentException.class, "boom");
        assertThat(cleanup).hasValue(0);
    }

    @Test
    public void testThatFinalizerIsCalledWhenStreamSupplierThrowsAnException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);
        AtomicInteger cleanup = new AtomicInteger();
        Multi.createFrom().<Integer, Integer> resourceFromUni(() -> Uni.createFrom().item(1), s -> {
            throw new IllegalArgumentException("boom");
        })
                .withFinalizer(cleanup::set)
                .subscribe(subscriber);
        subscriber
                .assertFailedWith(IllegalArgumentException.class, "boom");
        assertThat(cleanup).hasValue(1);
    }

    @Test
    public void testThatFinalizerIsCalledWhenStreamSupplierReturnsNull() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);
        AtomicInteger cleanup = new AtomicInteger();
        Multi.createFrom().<Integer, Integer> resourceFromUni(() -> Uni.createFrom().item(1), s -> null)
                .withFinalizer(cleanup::set)
                .subscribe(subscriber);
        subscriber
                .assertFailedWith(IllegalArgumentException.class, "`null`");
        assertThat(cleanup).hasValue(1);
    }

    @Test
    public void testThatFinalizerThrowingException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(20);
        Consumer<Integer> fin = s -> {
            throw new IllegalStateException("boom");
        };
        Multi.createFrom().resourceFromUni(() -> Uni.createFrom().item(1), s -> Multi.createFrom().range(s, 11))
                .withFinalizer(fin)
                .subscribe(subscriber);
        subscriber
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testThatFinalizerThrowingExceptionAfterStreamFailure() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(20);
        Consumer<Integer> fin = s -> {
            throw new IllegalStateException("boom");
        };
        Multi.createFrom()
                .resourceFromUni(() -> Uni.createFrom().item(1),
                        r -> Multi.createFrom().<Integer> emitter(e -> e.emit(1).emit(2).fail(new IOException("no!"))))
                .withFinalizer(fin)
                .subscribe(subscriber);
        subscriber
                .assertItems(1, 2)
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "no!");

    }

    @Test
    public void testThatOnFailureFinalizerIsNotCallIfResourceSupplierThrowsAnException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(20);
        Supplier<Uni<Integer>> supplier = () -> {
            throw new NullPointerException("boom");
        };
        AtomicInteger onFailure = new AtomicInteger();
        AtomicInteger onComplete = new AtomicInteger();
        AtomicInteger onCancellation = new AtomicInteger();

        BiFunction<Integer, Throwable, Uni<Void>> onFailureCallback = (s, f) -> {
            onFailure.set(s);
            return Uni.createFrom().voidItem();
        };

        Function<Integer, Uni<Void>> onCompletionCallback = s -> {
            onComplete.set(s);
            return Uni.createFrom().voidItem();
        };

        Function<Integer, Uni<Void>> onCancellationCallback = s -> {
            onCancellation.set(s);
            return Uni.createFrom().voidItem();
        };

        Multi.createFrom().resourceFromUni(supplier,
                r -> Multi.createFrom().range(r, 11))
                .withFinalizer(onCompletionCallback, onFailureCallback, onCancellationCallback)
                .subscribe(subscriber);

        subscriber
                .assertFailedWith(NullPointerException.class, "boom");
        assertThat(onFailure).hasValue(0);
        assertThat(onCancellation).hasValue(0);
        assertThat(onComplete).hasValue(0);
    }

    @Test
    public void cancellationShouldBePossible() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(20);
        Supplier<Uni<Integer>> supplier = () -> Uni.createFrom().item(1);
        AtomicInteger onFailure = new AtomicInteger();
        AtomicInteger onComplete = new AtomicInteger();
        AtomicInteger onCancellation = new AtomicInteger();

        BiFunction<Integer, Throwable, Uni<Void>> onFailureCallback = (s, f) -> {
            onFailure.set(s);
            return Uni.createFrom().voidItem();
        };

        Function<Integer, Uni<Void>> onCompletionCallback = s -> {
            onComplete.set(s);
            return Uni.createFrom().voidItem();
        };

        Function<Integer, Uni<Void>> onCancellationCallback = s -> {
            onCancellation.set(s);
            return Uni.createFrom().voidItem();
        };

        Multi.createFrom().<Integer, Integer> resourceFromUni(supplier,
                r -> Multi.createFrom().nothing())
                .withFinalizer(onCompletionCallback, onFailureCallback, onCancellationCallback)
                .subscribe(subscriber);

        subscriber
                .cancel();
        assertThat(onFailure).hasValue(0);
        assertThat(onCancellation).hasValue(1);
        assertThat(onComplete).hasValue(0);
    }

    @Test
    public void testWithFakeTransactionalResource() {
        Multi<String> multi = Multi.createFrom().resourceFromUni(
                FakeTransactionalResource::create,
                FakeTransactionalResource::data)
                .withFinalizer(FakeTransactionalResource::commit, FakeTransactionalResource::rollback,
                        FakeTransactionalResource::cancel);

        multi.subscribe().withSubscriber(AssertSubscriber.create(20))
                .awaitCompletion()
                .assertItems("in transaction");

        assertThat(FakeTransactionalResource.last.subscribed).isTrue();
        assertThat(FakeTransactionalResource.last.onCompleteSubscribed).isTrue();
        assertThat(FakeTransactionalResource.last.onCancelSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onFailureSubscribed).isFalse();
    }

    @Test
    public void testThatStreamSupplierThrowingExceptionCallsOnFailure() {
        FakeTransactionalResource resource = new FakeTransactionalResource();

        Multi<String> multi = Multi.createFrom().<FakeTransactionalResource, String> resource(() -> resource, r -> {
            throw new IllegalStateException("boom");
        })
                .withFinalizer(FakeTransactionalResource::commit, FakeTransactionalResource::rollback,
                        FakeTransactionalResource::cancel);

        multi.subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertFailedWith(IllegalStateException.class, "boom");

        assertThat(resource.subscribed).isFalse();
        assertThat(resource.onCompleteSubscribed).isFalse();
        assertThat(resource.onCancelSubscribed).isFalse();
        assertThat(resource.onFailureSubscribed).isTrue();
        assertThat(resource.failure.get()).isInstanceOf(IllegalStateException.class).hasMessage("boom");
    }

    @Test
    public void testThatStreamSupplierReturningNullCallsOnFailure() {
        FakeTransactionalResource resource = new FakeTransactionalResource();

        Multi<String> multi = Multi.createFrom().<FakeTransactionalResource, String> resource(() -> resource, r -> null)
                .withFinalizer(FakeTransactionalResource::commit, FakeTransactionalResource::rollback,
                        FakeTransactionalResource::cancel);

        multi.subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertFailedWith(IllegalArgumentException.class, "`null`");

        assertThat(resource.subscribed).isFalse();
        assertThat(resource.onCompleteSubscribed).isFalse();
        assertThat(resource.onCancelSubscribed).isFalse();
        assertThat(resource.onFailureSubscribed).isTrue();
        assertThat(resource.failure.get()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testThatStreamSupplierEmittingAFailureCallsOnFailure() {
        FakeTransactionalResource resource = new FakeTransactionalResource();

        Multi<String> multi = Multi.createFrom().<FakeTransactionalResource, String> resource(() -> resource,
                r -> Multi.createFrom().failure(new IOException("boom")))
                .withFinalizer(FakeTransactionalResource::commit, FakeTransactionalResource::rollback,
                        FakeTransactionalResource::cancel);

        multi.subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertFailedWith(IOException.class, "boom");

        assertThat(resource.subscribed).isFalse();
        assertThat(resource.onCompleteSubscribed).isFalse();
        assertThat(resource.onCancelSubscribed).isFalse();
        assertThat(resource.onFailureSubscribed).isTrue();
        assertThat(resource.failure.get()).isInstanceOf(IOException.class);
    }

    @Test
    public void testThatCancellationDueToPartialConsumptionCallsOnCancel() {

        Multi<String> multi = Multi.createFrom().resourceFromUni(
                FakeTransactionalResource::create,
                FakeTransactionalResource::infinite)
                .withFinalizer(FakeTransactionalResource::commit, FakeTransactionalResource::rollback,
                        FakeTransactionalResource::cancel)
                .select().first(3);

        multi.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .awaitCompletion()
                .assertItems("0", "1", "2");

        assertThat(FakeTransactionalResource.last.subscribed).isTrue();
        assertThat(FakeTransactionalResource.last.onCompleteSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onCancelSubscribed).isTrue();
        assertThat(FakeTransactionalResource.last.onFailureSubscribed).isFalse();
    }

    @Test
    public void testThatCancellationFailureAreNotPropagated() {
        Multi<String> multi = Multi.createFrom()
                .resourceFromUni(FakeTransactionalResource::create, FakeTransactionalResource::infinite)
                .withFinalizer(FakeTransactionalResource::commit, FakeTransactionalResource::rollback,
                        r -> r.cancel().onItem().failWith(x -> new IOException("boom")))
                .select().first(3);

        multi.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .awaitCompletion()
                .assertItems("0", "1", "2");

        assertThat(FakeTransactionalResource.last.subscribed).isTrue();
        assertThat(FakeTransactionalResource.last.onCompleteSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onCancelSubscribed).isTrue();
        assertThat(FakeTransactionalResource.last.onFailureSubscribed).isFalse();
    }

    @Test
    public void testThatCancellationReturningNullAreNotPropagated() {
        Multi<String> multi = Multi.createFrom()
                .resourceFromUni(FakeTransactionalResource::create, FakeTransactionalResource::infinite)
                .withFinalizer(FakeTransactionalResource::commit, FakeTransactionalResource::rollback,
                        r -> null)
                .select().first(3);

        multi.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .awaitCompletion()
                .assertItems("0", "1", "2");

        assertThat(FakeTransactionalResource.last.subscribed).isTrue();
        assertThat(FakeTransactionalResource.last.onCompleteSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onCancelSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onFailureSubscribed).isFalse();
    }

    @Test
    public void testThatCompletionFailureArePropagated() {
        Multi<String> multi = Multi.createFrom()
                .resourceFromUni(FakeTransactionalResource::create, FakeTransactionalResource::data)
                .withFinalizer(r -> r.commit().onItem().failWith(x -> new IOException("boom")),
                        FakeTransactionalResource::rollback,
                        FakeTransactionalResource::cancel)
                .select().first(3);

        multi.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .awaitFailure()
                .assertItems("in transaction")
                .assertFailedWith(IOException.class, "boom");

        assertThat(FakeTransactionalResource.last.subscribed).isTrue();
        assertThat(FakeTransactionalResource.last.onCompleteSubscribed).isTrue();
        assertThat(FakeTransactionalResource.last.onCancelSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onFailureSubscribed).isFalse();
    }

    @Test
    public void testThatCompletionFailureArePropagated2() {
        Multi<String> multi = Multi.createFrom().resourceFromUni(FakeTransactionalResource::create,
                FakeTransactionalResource::data)
                .withFinalizer(FakeTransactionalResource::commitFailure,
                        FakeTransactionalResource::rollback,
                        FakeTransactionalResource::cancel)
                .select().first(3);

        multi.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .awaitFailure()
                .assertItems("in transaction")
                .assertFailedWith(IOException.class, "commit failed");

        assertThat(FakeTransactionalResource.last.subscribed).isTrue();
        assertThat(FakeTransactionalResource.last.onCompleteSubscribed).isTrue();
        assertThat(FakeTransactionalResource.last.onCancelSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onFailureSubscribed).isFalse();
    }

    @Test
    public void testWithOnCompletionReturningNull() {
        Multi<String> multi = Multi.createFrom().resourceFromUni(FakeTransactionalResource::create,
                FakeTransactionalResource::data)
                .withFinalizer(
                        FakeTransactionalResource::commitReturningNull,
                        FakeTransactionalResource::rollback,
                        FakeTransactionalResource::cancel)
                .select().first(3);

        multi.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .awaitFailure()
                .assertItems("in transaction")
                .assertFailedWith(NullPointerException.class, "`null`");

        assertThat(FakeTransactionalResource.last.subscribed).isTrue();
        assertThat(FakeTransactionalResource.last.onCompleteSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onCancelSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onFailureSubscribed).isFalse();
    }

    @Test
    public void testThatOnFailureFailureArePropagated() {
        Multi<String> multi = Multi.createFrom().resourceFromUni(FakeTransactionalResource::create,
                r -> r.data().onCompletion().failWith(new IOException("boom")))
                .withFinalizer(FakeTransactionalResource::commit, FakeTransactionalResource::rollbackFailure,
                        FakeTransactionalResource::cancel);

        multi.subscribe().withSubscriber(AssertSubscriber.create(20))
                .awaitFailure()
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "rollback failed");

        assertThat(FakeTransactionalResource.last.subscribed).isTrue();
        assertThat(FakeTransactionalResource.last.onCompleteSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onCancelSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onFailureSubscribed).isTrue();
        assertThat(FakeTransactionalResource.last.failure.get()).isInstanceOf(IOException.class);
    }

    @Test
    public void testWithOnFailureReturningNull() {
        Multi<String> multi = Multi.createFrom().resourceFromUni(FakeTransactionalResource::create,
                r -> r.data().onCompletion().failWith(new IOException("boom")))
                .withFinalizer(FakeTransactionalResource::commit, FakeTransactionalResource::rollbackReturningNull,
                        FakeTransactionalResource::cancel);

        multi.subscribe().withSubscriber(AssertSubscriber.create(20))
                .awaitFailure()
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "`null`");

        assertThat(FakeTransactionalResource.last.subscribed).isTrue();
        assertThat(FakeTransactionalResource.last.onCompleteSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onCancelSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onFailureSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.failure.get()).isInstanceOf(IOException.class);
    }

    @Test
    public void testOnCompletionWithSingleFinalizer() {
        AtomicBoolean subscribed = new AtomicBoolean();
        Multi<Integer> multi = Multi.createFrom()
                .resourceFromUni(() -> Uni.createFrom().item(1), x -> Multi.createFrom().range(x, 11))
                .withFinalizer(r -> {
                    return Uni.createFrom().item("ok")
                            .onSubscription().invoke(s -> subscribed.set(true))
                            .onItem().ignore().andContinueWithNull();
                });
        multi.subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(subscribed).isTrue();
    }

    @Test
    public void testOnFailureWithSingleFinalizer() {
        AtomicBoolean subscribed = new AtomicBoolean();
        Multi<Integer> multi = Multi.createFrom()
                .resourceFromUni(() -> Uni.createFrom().item(1),
                        x -> Multi.createFrom().range(x, 11).onCompletion().failWith(new IOException("boom")))
                .withFinalizer(r -> {
                    return Uni.createFrom().item("ok")
                            .onSubscription().invoke(s -> subscribed.set(true))
                            .onItem().ignore().andContinueWithNull();
                });
        multi.subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertFailedWith(IOException.class, "boom")
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(subscribed).isTrue();
    }

    @Test
    public void testOnCancellationWithSingleFinalizer() {
        AtomicBoolean subscribed = new AtomicBoolean();
        Multi<Long> multi = Multi.createFrom()
                .resourceFromUni(() -> Uni.createFrom().item(1),
                        x -> Multi.createFrom().ticks().every(Duration.ofMillis(10)))
                .withFinalizer(r -> {
                    return Uni.createFrom().item("ok")
                            .onSubscription().invoke(s -> subscribed.set(true))
                            .onItem().ignore().andContinueWithNull();
                })
                .select().first(5);
        multi.subscribe().withSubscriber(AssertSubscriber.create(20))
                .awaitCompletion()
                .assertItems(0L, 1L, 2L, 3L, 4L);
        assertThat(subscribed).isTrue();
    }

    @Test
    public void testThatOnCancellationIsNotCalledAfterCompletion() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create(4);
        Multi.createFrom().resourceFromUni(FakeTransactionalResource::create, FakeTransactionalResource::data)
                .withFinalizer(
                        FakeTransactionalResource::commit,
                        FakeTransactionalResource::rollback,
                        FakeTransactionalResource::cancel)
                .subscribe(subscriber);
        subscriber
                .awaitCompletion()
                .cancel();

        assertThat(FakeTransactionalResource.last.onCompleteSubscribed).isTrue();
        assertThat(FakeTransactionalResource.last.onCancelSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onFailureSubscribed).isFalse();
    }

    @Test
    public void testThatOnCancellationIsNotCalledAfterFailure() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create(4);
        Multi.createFrom().resourceFromUni(FakeTransactionalResource::create,
                r -> r.data().onCompletion().failWith(new IOException("boom")))
                .withFinalizer(
                        FakeTransactionalResource::commit,
                        FakeTransactionalResource::rollbackDelay,
                        FakeTransactionalResource::cancel)
                .subscribe(subscriber);
        subscriber
                .awaitFailure()
                .assertFailedWith(IOException.class, "boom")
                .cancel();

        assertThat(FakeTransactionalResource.last.onCompleteSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onCancelSubscribed).isFalse();
        assertThat(FakeTransactionalResource.last.onFailureSubscribed).isTrue();
    }

    @Test
    public void testThatFinalizerIsNotCalledIfTheUniDoesNothing() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);
        AtomicInteger cleanup = new AtomicInteger();
        UniOnCancellationSpy<Integer> nothing = Spy.onCancellation(Uni.createFrom().nothing());
        Multi.createFrom().resourceFromUni(() -> nothing, r -> Multi.createFrom().range(r, 11))
                .withFinalizer(cleanup::set)
                .subscribe(subscriber);
        subscriber
                .assertSubscribed()
                .cancel();

        subscriber.assertNotTerminated();
        assertThat(cleanup.get()).isEqualTo(0);
        assertThat(nothing.invoked()).isTrue();
    }

    static class FakeTransactionalResource {

        private static final Duration DELAY = Duration.ofMillis(100);

        static volatile FakeTransactionalResource last;

        static Uni<FakeTransactionalResource> create() {
            FakeTransactionalResource item = new FakeTransactionalResource();
            last = item;
            return Uni.createFrom().item(item)
                    .onItem().delayIt().by(Duration.ofMillis(10));
        }

        AtomicBoolean subscribed = new AtomicBoolean();
        AtomicBoolean onFailureSubscribed = new AtomicBoolean();
        AtomicBoolean onCompleteSubscribed = new AtomicBoolean();
        AtomicBoolean onCancelSubscribed = new AtomicBoolean();

        AtomicReference<Throwable> failure = new AtomicReference<>();

        public Multi<String> data() {
            return Multi.createFrom().item("in transaction")
                    .onSubscription().invoke(s -> subscribed.set(true));
        }

        public Multi<String> infinite() {
            return Multi.createFrom().ticks().every(Duration.ofMillis(10))
                    .onItem().transform(l -> Long.toString(l))
                    .onSubscription().invoke(s -> subscribed.set(true));
        }

        public Uni<Void> commit() {
            return Uni.createFrom().voidItem()
                    .onSubscription().invoke(s -> onCompleteSubscribed.set(true));
        }

        public Uni<Void> commitFailure() {
            return Uni.createFrom().voidItem()
                    .onItem().delayIt().by(DELAY)
                    .onItem().failWith(x -> new IOException("commit failed"))
                    .onSubscription().invoke(s -> onCompleteSubscribed.set(true));
        }

        public Uni<Void> commitReturningNull() {
            return null;
        }

        public Uni<Void> rollback(Throwable failure) {
            return Uni.createFrom().voidItem()
                    .onItem().invoke(x -> this.failure.set(failure))
                    .onSubscription().invoke(s -> onFailureSubscribed.set(true));
        }

        public Uni<Void> rollbackDelay(Throwable failure) {
            return Uni.createFrom().voidItem()
                    .onItem().invoke(x -> this.failure.set(failure))
                    .onItem().delayIt().by(DELAY)
                    .onSubscription().invoke(s -> onFailureSubscribed.set(true));
        }

        public Uni<Void> rollbackFailure(Throwable failure) {
            return Uni.createFrom().voidItem()
                    .onItem().invoke(x -> this.failure.set(failure))
                    .onItem().delayIt().by(DELAY)
                    .onItem().failWith(x -> new IOException("rollback failed"))
                    .onSubscription().invoke(s -> onFailureSubscribed.set(true));
        }

        public Uni<Void> rollbackReturningNull(Throwable f) {
            failure.set(f);
            return null;
        }

        public Uni<Void> cancel() {
            return Uni.createFrom().voidItem()
                    .onSubscription().invoke(s -> onCancelSubscribed.set(true));
        }
    }
}
