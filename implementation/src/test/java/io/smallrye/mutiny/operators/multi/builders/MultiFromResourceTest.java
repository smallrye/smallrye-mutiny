package io.smallrye.mutiny.operators.multi.builders;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiFromResourceTest {

    @Test
    public void testWithResourceSupplierThrowingException() {
        Supplier<String> supplier = () -> {
            throw new IllegalArgumentException("boom");
        };
        Multi<String> multi = Multi.createFrom().resource(supplier,
                s -> Multi.createFrom().items(s))
                .withFinalizer(r -> {
                });
        AssertSubscriber<String> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create(10));
        subscriber
                .assertHasFailedWith(IllegalArgumentException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithResourceSupplierProducingNull() {
        Supplier<String> supplier = () -> null;
        Multi<String> multi = Multi.createFrom().resource(supplier,
                s -> Multi.createFrom().items(s))
                .withFinalizer(r -> {
                });
        AssertSubscriber<String> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create(10));
        subscriber
                .assertHasFailedWith(IllegalArgumentException.class, "")
                .assertHasNotReceivedAnyItem();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithNullAsResourceSupplier() {
        Multi.createFrom().resource(null,
                s -> Multi.createFrom().items(s))
                .withFinalizer(r -> {
                });
    }

    @Test
    public void testWithStreamSupplierThrowingException() {
        Supplier<String> supplier = () -> "Hello";
        Function<String, Publisher<String>> stream = s -> {
            throw new IllegalArgumentException("boom");
        };
        Multi<String> multi = Multi.createFrom().resource(supplier, stream)
                .withFinalizer(r -> {
                });
        AssertSubscriber<String> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create(10));
        subscriber
                .assertHasFailedWith(IllegalArgumentException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithStreamSupplierProducingNull() {
        Supplier<String> supplier = () -> null;
        Multi<String> multi = Multi.createFrom().resource(supplier,
                s -> (Publisher<String>) null)
                .withFinalizer(r -> {
                });
        AssertSubscriber<String> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create(10));
        subscriber
                .assertHasFailedWith(IllegalArgumentException.class, "")
                .assertHasNotReceivedAnyItem();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithNullAsStreamSupplier() {
        Multi.createFrom().resource(() -> "hello", null)
                .withFinalizer(r -> {
                });
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithNullAsFinalizer() {
        Multi.createFrom().resource(() -> "hello", null)
                .withFinalizer((Consumer<String>) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithNullAsFinalizer2() {
        Multi.createFrom().resource(() -> "hello", null)
                .withFinalizer((Function<String, Uni<Void>>) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithNullAsFinalizer3() {
        Function<String, Uni<Void>> function = s -> Uni.createFrom().item(() -> null);
        Multi.createFrom().resource(() -> "hello", null)
                .withFinalizer(function, null, function);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithNullAsFinalizer4() {
        Function<String, Uni<Void>> function = s -> Uni.createFrom().item(() -> null);
        BiFunction<String, Throwable, Uni<Void>> onFailure = (s, f) -> Uni.createFrom().item(() -> null);
        Multi.createFrom().resource(() -> "hello", null)
                .withFinalizer(null, onFailure, function);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithNullAsFinalizer5() {
        Function<String, Uni<Void>> function = s -> Uni.createFrom().item(() -> null);
        BiFunction<String, Throwable, Uni<Void>> onFailure = (s, f) -> Uni.createFrom().item(() -> null);
        Multi.createFrom().resource(() -> "hello", null)
                .withFinalizer(function, onFailure, null);
    }

    @Test
    public void simpleSynchronousTest() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);
        AtomicInteger cleanup = new AtomicInteger();
        Multi.createFrom().resource(() -> 1, r -> Multi.createFrom().range(r, 11))
                .withFinalizer(cleanup::set)
                .subscribe(subscriber);
        subscriber
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .assertCompletedSuccessfully()
                .assertHasNotFailed();
        assertThat(cleanup.get()).isEqualTo(1);
    }

    @Test
    public void simpleSynchronousTestWithMultipleSubscribers() {
        AssertSubscriber<Integer> subscriber1 = AssertSubscriber.create(10);
        AssertSubscriber<Integer> subscriber2 = AssertSubscriber.create(10);
        List<Integer> list = new ArrayList<>();
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().resource(count::incrementAndGet,
                r -> Multi.createFrom().range(r, 11))
                .withFinalizer((Consumer<Integer>) list::add);
        multi.subscribe(subscriber1);
        multi.subscribe(subscriber2);

        subscriber1
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .assertCompletedSuccessfully()
                .assertHasNotFailed();
        subscriber2
                .assertReceived(2, 3, 4, 5, 6, 7, 8, 9, 10)
                .assertCompletedSuccessfully()
                .assertHasNotFailed();
        assertThat(list).containsExactly(1, 2);
    }

    @Test
    public void testCleanupCalledOnCompletionWithSynchronousFinalizer() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(9);
        AtomicInteger cleanup = new AtomicInteger();
        Multi.createFrom().resource(() -> 1, r -> Multi.createFrom().range(r, 11))
                .withFinalizer(cleanup::set)
                .subscribe(subscriber);
        subscriber
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .run(() -> assertThat(cleanup).hasValue(0))
                .request(1)
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .run(() -> assertThat(cleanup).hasValue(1))
                .assertCompletedSuccessfully()
                .assertHasNotFailed();
    }

    @Test
    public void testCleanupCalledOnCancellationWithSynchronousFinalizer() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(4);
        AtomicInteger cleanup = new AtomicInteger();
        Multi.createFrom().resource(() -> 1, r -> Multi.createFrom().range(r, 11))
                .withFinalizer(cleanup::set)
                .subscribe(subscriber);
        subscriber
                .assertReceived(1, 2, 3, 4)
                .run(() -> assertThat(cleanup).hasValue(0))
                .cancel()
                .assertReceived(1, 2, 3, 4)
                .run(() -> assertThat(cleanup).hasValue(1))
                .assertHasNotCompleted()
                .assertHasNotFailed();
    }

    @Test
    public void testCleanupCalledOnFailureWithSynchronousFinalizer() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);
        AtomicInteger cleanup = new AtomicInteger();
        Multi.createFrom().resource(() -> 1, r -> Multi.createFrom().<Integer> emitter(e -> {
            e.emit(1).emit(2).fail(new IOException("boom"));
        }))
                .withFinalizer(cleanup::set)
                .subscribe(subscriber);
        subscriber
                .assertReceived(1)
                .run(() -> assertThat(cleanup).hasValue(0))
                .request(3)
                .assertReceived(1, 2)
                .run(() -> assertThat(cleanup).hasValue(1))
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testThatFinalizerIsNotCalledWhenResourceSupplierThrowsAnException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);
        Supplier<Integer> supplier = () -> {
            throw new IllegalArgumentException("boom");
        };
        AtomicInteger cleanup = new AtomicInteger();
        Multi.createFrom().resource(supplier, r -> Multi.createFrom().<Integer> emitter(e -> {
            e.emit(1).emit(2).fail(new IOException("boom"));
        }))
                .withFinalizer(cleanup::set)
                .subscribe(subscriber);
        subscriber
                .assertHasFailedWith(IllegalArgumentException.class, "boom");
        assertThat(cleanup).hasValue(0);
    }

    @Test
    public void testThatFinalizerIsCalledWhenStreamSupplierThrowsAnException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);
        AtomicInteger cleanup = new AtomicInteger();
        Multi.createFrom().<Integer, Integer> resource(() -> 1, s -> {
            throw new IllegalArgumentException("boom");
        })
                .withFinalizer(cleanup::set)
                .subscribe(subscriber);
        subscriber
                .assertHasFailedWith(IllegalArgumentException.class, "boom");
        assertThat(cleanup).hasValue(1);
    }

    @Test
    public void testThatFinalizerIsCalledWhenStreamSupplierReturnsNull() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);
        AtomicInteger cleanup = new AtomicInteger();
        Multi.createFrom().<Integer, Integer> resource(() -> 1, s -> null)
                .withFinalizer(cleanup::set)
                .subscribe(subscriber);
        subscriber
                .assertHasFailedWith(IllegalArgumentException.class, "`null`");
        assertThat(cleanup).hasValue(1);
    }

    @Test
    public void testThatFinalizerThrowingException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(20);
        Consumer<Integer> fin = s -> {
            throw new IllegalStateException("boom");
        };
        Multi.createFrom().resource(() -> 1, s -> Multi.createFrom().range(s, 11))
                .withFinalizer(fin)
                .subscribe(subscriber);
        subscriber
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .assertHasNotCompleted()
                .assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testThatFinalizerThrowingExceptionAfterStreamFailure() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(20);
        Consumer<Integer> fin = s -> {
            throw new IllegalStateException("boom");
        };
        Multi.createFrom().resource(() -> 1, r -> Multi.createFrom().<Integer> emitter(e -> {
            e.emit(1).emit(2).fail(new IOException("no!"));
        }))
                .withFinalizer(fin)
                .subscribe(subscriber);
        subscriber
                .assertReceived(1, 2)
                .assertHasNotCompleted()
                .assertHasFailedWith(CompositeException.class, "boom")
                .assertHasFailedWith(CompositeException.class, "no!");

    }

    @Test
    public void testThatOnFailureFinalizerIsNotCallIfResourceSupplierThrowsAnException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(20);
        Supplier<Integer> supplier = () -> {
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

        Multi.createFrom().resource(supplier,
                r -> Multi.createFrom().range(r, 11))
                .withFinalizer(onCompletionCallback, onFailureCallback, onCancellationCallback)
                .subscribe(subscriber);

        subscriber
                .assertHasFailedWith(NullPointerException.class, "boom");
        assertThat(onFailure).hasValue(0);
        assertThat(onCancellation).hasValue(0);
        assertThat(onComplete).hasValue(0);
    }

    @Test
    public void cancellationShouldBePossible() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(20);
        Supplier<Integer> supplier = () -> 1;
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

        Multi.createFrom().<Integer, Integer> resource(supplier,
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
        FakeTransactionalResource resource = new FakeTransactionalResource();

        Multi<String> multi = Multi.createFrom().resource(() -> resource, FakeTransactionalResource::data)
                .withFinalizer(FakeTransactionalResource::commit, FakeTransactionalResource::rollback,
                        FakeTransactionalResource::cancel);

        multi.subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertReceived("in transaction")
                .assertCompletedSuccessfully();

        assertThat(resource.subscribed).isTrue();
        assertThat(resource.onCompleteSubscribed).isTrue();
        assertThat(resource.onCancelSubscribed).isFalse();
        assertThat(resource.onFailureSubscribed).isFalse();
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
                .assertHasFailedWith(IllegalStateException.class, "boom");

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
                .assertHasFailedWith(IllegalArgumentException.class, "`null`");

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
                .assertHasFailedWith(IOException.class, "boom");

        assertThat(resource.subscribed).isFalse();
        assertThat(resource.onCompleteSubscribed).isFalse();
        assertThat(resource.onCancelSubscribed).isFalse();
        assertThat(resource.onFailureSubscribed).isTrue();
        assertThat(resource.failure.get()).isInstanceOf(IOException.class);
    }

    @Test
    public void testThatCancellationDueToPartialConsumptionCallsOnCancel() {
        FakeTransactionalResource resource = new FakeTransactionalResource();

        Multi<String> multi = Multi.createFrom().resource(() -> resource, FakeTransactionalResource::infinite)
                .withFinalizer(FakeTransactionalResource::commit, FakeTransactionalResource::rollback,
                        FakeTransactionalResource::cancel)
                .transform().byTakingFirstItems(3);

        multi.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .await()
                .assertReceived("0", "1", "2")
                .assertCompletedSuccessfully();

        assertThat(resource.subscribed).isTrue();
        assertThat(resource.onCompleteSubscribed).isFalse();
        assertThat(resource.onCancelSubscribed).isTrue();
        assertThat(resource.onFailureSubscribed).isFalse();
    }

    @Test
    public void testThatCancellationFailureAreNotPropagated() {
        FakeTransactionalResource resource = new FakeTransactionalResource();

        Multi<String> multi = Multi.createFrom().resource(() -> resource, FakeTransactionalResource::infinite)
                .withFinalizer(FakeTransactionalResource::commit, FakeTransactionalResource::rollback,
                        r -> r.cancel().onItem().failWith(x -> new IOException("boom")))
                .transform().byTakingFirstItems(3);

        multi.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .await()
                .assertReceived("0", "1", "2")
                .assertCompletedSuccessfully();

        assertThat(resource.subscribed).isTrue();
        assertThat(resource.onCompleteSubscribed).isFalse();
        assertThat(resource.onCancelSubscribed).isTrue();
        assertThat(resource.onFailureSubscribed).isFalse();
    }

    @Test
    public void testThatCancellationReturningNullAreNotPropagated() {
        FakeTransactionalResource resource = new FakeTransactionalResource();

        Multi<String> multi = Multi.createFrom().resource(() -> resource, FakeTransactionalResource::infinite)
                .withFinalizer(FakeTransactionalResource::commit, FakeTransactionalResource::rollback,
                        r -> null)
                .transform().byTakingFirstItems(3);

        multi.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .await()
                .assertReceived("0", "1", "2")
                .assertCompletedSuccessfully();

        assertThat(resource.subscribed).isTrue();
        assertThat(resource.onCompleteSubscribed).isFalse();
        assertThat(resource.onCancelSubscribed).isFalse();
        assertThat(resource.onFailureSubscribed).isFalse();
    }

    @Test
    public void testThatCompletionFailureArePropagated() {
        FakeTransactionalResource resource = new FakeTransactionalResource();

        Multi<String> multi = Multi.createFrom().resource(() -> resource, FakeTransactionalResource::data)
                .withFinalizer(r -> r.commit().onItem().failWith(x -> new IOException("boom")),
                        FakeTransactionalResource::rollback,
                        FakeTransactionalResource::cancel)
                .transform().byTakingFirstItems(3);

        multi.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .await()
                .assertReceived("in transaction")
                .assertHasFailedWith(IOException.class, "boom");

        assertThat(resource.subscribed).isTrue();
        assertThat(resource.onCompleteSubscribed).isTrue();
        assertThat(resource.onCancelSubscribed).isFalse();
        assertThat(resource.onFailureSubscribed).isFalse();
    }

    @Test
    public void testThatCompletionFailureArePropagated2() {
        FakeTransactionalResource resource = new FakeTransactionalResource();

        Multi<String> multi = Multi.createFrom().resource(() -> resource, FakeTransactionalResource::data)
                .withFinalizer(FakeTransactionalResource::commitFailure,
                        FakeTransactionalResource::rollback,
                        FakeTransactionalResource::cancel)
                .transform().byTakingFirstItems(3);

        multi.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .await()
                .assertReceived("in transaction")
                .assertHasFailedWith(IOException.class, "commit failed");

        assertThat(resource.subscribed).isTrue();
        assertThat(resource.onCompleteSubscribed).isTrue();
        assertThat(resource.onCancelSubscribed).isFalse();
        assertThat(resource.onFailureSubscribed).isFalse();
    }

    @Test
    public void testWithOnCompletionReturningNull() {
        FakeTransactionalResource resource = new FakeTransactionalResource();

        Multi<String> multi = Multi.createFrom().resource(() -> resource, FakeTransactionalResource::data)
                .withFinalizer(
                        FakeTransactionalResource::commitReturningNull,
                        FakeTransactionalResource::rollback,
                        FakeTransactionalResource::cancel)
                .transform().byTakingFirstItems(3);

        multi.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .await()
                .assertReceived("in transaction")
                .assertHasFailedWith(NullPointerException.class, "`null`");

        assertThat(resource.subscribed).isTrue();
        assertThat(resource.onCompleteSubscribed).isFalse();
        assertThat(resource.onCancelSubscribed).isFalse();
        assertThat(resource.onFailureSubscribed).isFalse();
    }

    @Test
    public void testThatOnFailureFailureArePropagated() {
        FakeTransactionalResource resource = new FakeTransactionalResource();

        Multi<String> multi = Multi.createFrom().resource(() -> resource,
                r -> r.data().onCompletion().failWith(new IOException("boom")))
                .withFinalizer(FakeTransactionalResource::commit, FakeTransactionalResource::rollbackFailure,
                        FakeTransactionalResource::cancel);

        multi.subscribe().withSubscriber(AssertSubscriber.create(20))
                .await()
                .assertHasFailedWith(CompositeException.class, "boom")
                .assertHasFailedWith(CompositeException.class, "rollback failed");

        assertThat(resource.subscribed).isTrue();
        assertThat(resource.onCompleteSubscribed).isFalse();
        assertThat(resource.onCancelSubscribed).isFalse();
        assertThat(resource.onFailureSubscribed).isTrue();
        assertThat(resource.failure.get()).isInstanceOf(IOException.class);
    }

    @Test
    public void testWithOnFailureReturningNull() {
        FakeTransactionalResource resource = new FakeTransactionalResource();

        Multi<String> multi = Multi.createFrom().resource(() -> resource,
                r -> r.data().onCompletion().failWith(new IOException("boom")))
                .withFinalizer(FakeTransactionalResource::commit, FakeTransactionalResource::rollbackReturningNull,
                        FakeTransactionalResource::cancel);

        multi.subscribe().withSubscriber(AssertSubscriber.create(20))
                .await()
                .assertHasFailedWith(CompositeException.class, "boom")
                .assertHasFailedWith(CompositeException.class, "`null`");

        assertThat(resource.subscribed).isTrue();
        assertThat(resource.onCompleteSubscribed).isFalse();
        assertThat(resource.onCancelSubscribed).isFalse();
        assertThat(resource.onFailureSubscribed).isFalse();
        assertThat(resource.failure.get()).isInstanceOf(IOException.class);
    }

    @Test
    public void testOnCompletionWithSingleFinalizer() {
        AtomicBoolean subscribed = new AtomicBoolean();
        Multi<Integer> multi = Multi.createFrom()
                .resource(() -> 1, x -> Multi.createFrom().range(x, 11))
                .withFinalizer(r -> {
                    return Uni.createFrom().item("ok")
                            .onSubscribe().invoke(s -> subscribed.set(true))
                            .onItem().ignore().andContinueWithNull();
                });
        multi.subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(subscribed).isTrue();
    }

    @Test
    public void testOnFailureWithSingleFinalizer() {
        AtomicBoolean subscribed = new AtomicBoolean();
        Multi<Integer> multi = Multi.createFrom()
                .resource(() -> 1, x -> Multi.createFrom().range(x, 11).onCompletion().failWith(new IOException("boom")))
                .withFinalizer(r -> {
                    return Uni.createFrom().item("ok")
                            .onSubscribe().invoke(s -> subscribed.set(true))
                            .onItem().ignore().andContinueWithNull();
                });
        multi.subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertHasFailedWith(IOException.class, "boom")
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(subscribed).isTrue();
    }

    @Test
    public void testOnCancellationWithSingleFinalizer() {
        AtomicBoolean subscribed = new AtomicBoolean();
        Multi<Long> multi = Multi.createFrom()
                .resource(() -> 1, x -> Multi.createFrom().ticks().every(Duration.ofMillis(10)))
                .withFinalizer(r -> {
                    return Uni.createFrom().item("ok")
                            .onSubscribe().invoke(s -> subscribed.set(true))
                            .onItem().ignore().andContinueWithNull();
                })
                .transform().byTakingFirstItems(5);
        multi.subscribe().withSubscriber(AssertSubscriber.create(20))
                .await()
                .assertCompletedSuccessfully()
                .assertReceived(0L, 1L, 2L, 3L, 4L);
        assertThat(subscribed).isTrue();
    }

    @Test
    public void testThatOnCancellationIsNotCalledAfterCompletion() {
        FakeTransactionalResource resource = new FakeTransactionalResource();
        AssertSubscriber<String> subscriber = AssertSubscriber.create(4);
        Multi.createFrom().resource(() -> resource, FakeTransactionalResource::data)
                .withFinalizer(
                        FakeTransactionalResource::commit,
                        FakeTransactionalResource::rollback,
                        FakeTransactionalResource::cancel)
                .subscribe(subscriber);
        subscriber
                .await()
                .assertCompletedSuccessfully()
                .cancel();

        assertThat(resource.onCompleteSubscribed).isTrue();
        assertThat(resource.onCancelSubscribed).isFalse();
        assertThat(resource.onFailureSubscribed).isFalse();
    }

    @Test
    public void testThatOnCancellationIsNotCalledAfterFailure() {
        FakeTransactionalResource resource = new FakeTransactionalResource();
        AssertSubscriber<String> subscriber = AssertSubscriber.create(4);
        Multi.createFrom().resource(() -> resource, r -> r.data().onCompletion().failWith(new IOException("boom")))
                .withFinalizer(
                        FakeTransactionalResource::commit,
                        FakeTransactionalResource::rollbackDelay,
                        FakeTransactionalResource::cancel)
                .subscribe(subscriber);
        subscriber
                .await()
                .assertHasFailedWith(IOException.class, "boom")
                .cancel();

        assertThat(resource.onCompleteSubscribed).isFalse();
        assertThat(resource.onCancelSubscribed).isFalse();
        assertThat(resource.onFailureSubscribed).isTrue();
    }

    static class FakeTransactionalResource {

        private static final Duration DELAY = Duration.ofMillis(100);

        AtomicBoolean subscribed = new AtomicBoolean();
        AtomicBoolean onFailureSubscribed = new AtomicBoolean();
        AtomicBoolean onCompleteSubscribed = new AtomicBoolean();
        AtomicBoolean onCancelSubscribed = new AtomicBoolean();

        AtomicReference<Throwable> failure = new AtomicReference<>();

        public Multi<String> data() {
            return Multi.createFrom().item("in transaction")
                    .onSubscribe().invoke(s -> subscribed.set(true));
        }

        public Multi<String> infinite() {
            return Multi.createFrom().ticks().every(Duration.ofMillis(10))
                    .onItem().transform(l -> Long.toString(l))
                    .onSubscribe().invoke(s -> subscribed.set(true));
        }

        public Uni<Void> commit() {
            return Uni.createFrom().voidItem()
                    .onSubscribe().invoke(s -> onCompleteSubscribed.set(true));
        }

        public Uni<Void> commitFailure() {
            return Uni.createFrom().voidItem()
                    .onItem().delayIt().by(DELAY)
                    .onItem().failWith(x -> new IOException("commit failed"))
                    .onSubscribe().invoke(s -> onCompleteSubscribed.set(true));
        }

        public Uni<Void> commitReturningNull() {
            return null;
        }

        public Uni<Void> rollback(Throwable failure) {
            return Uni.createFrom().voidItem()
                    .onItem().invoke(x -> this.failure.set(failure))
                    .onSubscribe().invoke(s -> onFailureSubscribed.set(true));
        }

        public Uni<Void> rollbackDelay(Throwable failure) {
            return Uni.createFrom().voidItem()
                    .onItem().invoke(x -> this.failure.set(failure))
                    .onItem().delayIt().by(DELAY)
                    .onSubscribe().invoke(s -> onFailureSubscribed.set(true));
        }

        public Uni<Void> rollbackFailure(Throwable failure) {
            return Uni.createFrom().voidItem()
                    .onItem().invoke(x -> this.failure.set(failure))
                    .onItem().delayIt().by(DELAY)
                    .onItem().failWith(x -> new IOException("rollback failed"))
                    .onSubscribe().invoke(s -> onFailureSubscribed.set(true));
        }

        public Uni<Void> rollbackReturningNull(Throwable f) {
            failure.set(f);
            return null;
        }

        public Uni<Void> cancel() {
            return Uni.createFrom().voidItem()
                    .onSubscribe().invoke(s -> onCancelSubscribed.set(true));
        }
    }
}
