package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class MultiOnFailureTest {

    @Test
    public void testThatRecoverWithMultiNotCalledWhenNoFailure() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(20);

        Multi.createFrom().range(1, 10)
                .onFailure().recoverWithMulti(v -> Multi.createFrom().range(50, 100))
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testRecoverWithMultiWithFailure() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(20);

        Multi.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure().recoverWithMulti(v -> Multi.createFrom().range(50, 52))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertItems(50, 51)
                .assertCompleted();
    }

    @Test
    public void testRecoverWithMultiWithPredicate() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);

        Multi.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure(IllegalStateException.class).recoverWithMulti(v -> Multi.createFrom().item(42))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertItems(42)
                .assertCompleted();
    }

    @Test
    public void testRecoverWithMultiWithPredicateNotPassing() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);

        Multi.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure(IOException.class).recoverWithMulti(v -> Multi.createFrom().item(42))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testRecoverWithMultiWithPredicateThrowingException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);

        Multi.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure(f -> {
                    throw new IllegalArgumentException("bad");
                }).recoverWithMulti(v -> Multi.createFrom().item(42))
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "bad");
    }

    @Test
    public void testOnFailureTransform() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        Multi.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure().transform(f -> new IOException("kaboom!"))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertHasNotReceivedAnyItem()
                .assertTerminated()
                .assertFailedWith(IOException.class, "kaboom!");
    }

    @Test
    public void testOnFailureTransformWithMapperReturningNull() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        Multi.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure().transform(f -> null)
                .subscribe().withSubscriber(subscriber);

        subscriber.assertHasNotReceivedAnyItem()
                .assertTerminated()
                .assertFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testOnFailureTransformWithMapperThrowingException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        Multi.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure().transform(f -> {
                    throw new TestException("no");
                })
                .subscribe().withSubscriber(subscriber);

        subscriber.assertHasNotReceivedAnyItem()
                .assertTerminated()
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "no");
    }

    @Test
    public void testOnFailureTransformWithPassingPredicate() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        Multi.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure(t -> t instanceof IllegalStateException)
                .transform(f -> new IOException("kaboom!"))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertHasNotReceivedAnyItem()
                .assertTerminated()
                .assertFailedWith(IOException.class, "kaboom!");
    }

    @Test
    public void testOnFailureTransformWithNotPassingPredicate() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        Multi.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure(t -> t instanceof TestException)
                .transform(f -> new IOException("kaboom!"))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertHasNotReceivedAnyItem()
                .assertTerminated()
                .assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testRequestOnTheMultiReturnedByRecoverWithMulti() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(0);

        Multi.createFrom()
                .<Integer> failure(new IllegalStateException("boom"))
                .onFailure().recoverWithMulti(v -> Multi.createFrom().range(50, 61))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertHasNotReceivedAnyItem()
                .assertNotTerminated();

        subscriber.request(4)
                .assertItems(50, 51, 52, 53)
                .assertNotTerminated();

        subscriber.request(5)
                .assertItems(50, 51, 52, 53, 54, 55, 56, 57, 58)
                .assertNotTerminated();

        subscriber.request(5)
                .assertItems(50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60)
                .assertCompleted();
    }

    @Test
    public void testRecoverWithMultiWithSomeResulsubscriberBeforeFailing() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(20);

        AtomicReference<MultiEmitter<? super Integer>> reference = new AtomicReference<>();
        Multi.createFrom().<Integer> emitter(reference::set)
                .onFailure().recoverWithMulti(v -> Multi.createFrom().range(50, 55))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertSubscribed();

        reference.get().emit(1)
                .emit(2)
                .emit(3)
                .emit(4)
                .emit(5)
                .fail(new IllegalStateException("boom"));

        subscriber.assertItems(1, 2, 3, 4, 5, 50, 51, 52, 53, 54)
                .assertCompleted();
    }

    @Test
    public void testRecoverWithMultiWithSomeResulsubscriberBeforeFailingWithRequessubscriber() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(3);

        AtomicReference<MultiEmitter<? super Integer>> reference = new AtomicReference<>();
        Multi.createFrom().<Integer> emitter(reference::set)
                .onFailure().recoverWithMulti(v -> Multi.createFrom().range(50, 55))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertSubscribed();

        reference.get().emit(1)
                .emit(2)
                .emit(3)
                .emit(4)
                .emit(5)
                .fail(new IllegalStateException("boom"));

        subscriber.assertItems(1, 2, 3)
                .request(5)
                .assertItems(1, 2, 3, 4, 5, 50, 51, 52)
                .request(10)
                .assertCompleted();
    }

    @Test
    public void testWhenRecoverWithMultiIsAlsoAFailure() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(0);

        Multi.createFrom().<Integer> failure(new IOException("karambar"))
                .onFailure().recoverWithMulti(v -> {
                    throw new IllegalStateException("kaboom!");
                })
                .subscribe().withSubscriber(subscriber);

        subscriber.assertHasNotReceivedAnyItem()
                .assertTerminated()
                .assertFailedWith(CompositeException.class, "kaboom!")
                .assertFailedWith(CompositeException.class, "karambar");
    }

    @Test
    public void testWhenRecoverWithMultiReturnsNull() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(0);

        Multi.createFrom().<Integer> failure(new IOException("karambar"))
                .onFailure().recoverWithMulti(v -> null)
                .subscribe().withSubscriber(subscriber);

        subscriber.assertHasNotReceivedAnyItem()
                .assertTerminated()
                .assertFailedWith(NullPointerException.class, "mapper");
    }

    @Test
    public void testRecoverWithItem() {
        Multi.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure().recoverWithItem(42)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompleted()
                .assertItems(42);
    }

    @Test
    public void testRecoverWithItemWithSupplier() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure().recoverWithItem(count::incrementAndGet);
        multi
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompleted()
                .assertItems(1);

        multi
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompleted()
                .assertItems(2);
    }

    @Test
    public void testRecoverWithItemWithNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().<String> failure(new IllegalStateException("boom"))
                        .onFailure().recoverWithItem((String) null));
    }

    @Test
    public void testRecoverWithItemWithNullSupplier() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().<String> failure(new IllegalStateException("boom"))
                        .onFailure().recoverWithItem((Supplier<String>) null));
    }

    @Test
    public void testRecoverWithItemAndSupplierReturningNull() {
        Multi.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure().recoverWithItem(() -> null)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "supplier");
    }

    @Test
    public void testRecoverWithCompletion() {
        Multi.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure().recoverWithCompletion()
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testRecoverWithMultiUsingEmitterAsFallback() {
        Multi<Integer> multi = Multi.createFrom().emitter(emitter -> emitter
                .emit(3)
                .emit(2)
                .emit(1)
                .fail(new IOException("boom")));

        AtomicInteger subscribed = new AtomicInteger();
        Multi<Integer> fallback = Multi.createFrom()
                .<Integer> emitter(s -> s.emit(42).emit(43).complete())
                .onSubscription().invoke(s -> subscribed.incrementAndGet());

        multi.onFailure()
                .recoverWithMulti(fallback)
                .subscribe().withSubscriber(AssertSubscriber.create(0))
                .assertSubscribed()
                .assertHasNotReceivedAnyItem()
                .request(2)
                .run(() -> assertThat(subscribed).hasValue(0))
                .assertItems(3, 2)
                .request(2)
                .assertItems(3, 2, 1, 42)
                .run(() -> assertThat(subscribed).hasValue(1))
                .request(2)
                .assertItems(3, 2, 1, 42, 43)
                .assertCompleted();
    }

    @Test
    public void testRecoverWithItem2() {
        Multi<Integer> multi = Multi.createFrom().emitter(emitter -> emitter
                .emit(3)
                .emit(2)
                .emit(1)
                .fail(new IOException("boom")));

        AtomicInteger subscribed = new AtomicInteger();
        Multi<Integer> fallback = Multi.createFrom()
                .item(0)
                .onSubscription().invoke(s -> subscribed.incrementAndGet());

        multi.onFailure()
                .recoverWithMulti(fallback)
                .subscribe().withSubscriber(AssertSubscriber.create(0))
                .assertSubscribed()
                .assertHasNotReceivedAnyItem()
                .request(2)
                .run(() -> assertThat(subscribed).hasValue(0))
                .assertItems(3, 2)
                .request(2)
                .assertItems(3, 2, 1, 0)
                .run(() -> assertThat(subscribed).hasValue(1))
                .assertCompleted();
    }

    @Test
    public void testOnFailureTransformWithPredicate() {
        Multi.createFrom().<Integer> failure(new IOException())
                .onFailure(IOException.class::isInstance)
                .transform(e -> new Exception("BOOM!!!"))
                .subscribe().withSubscriber(AssertSubscriber.create(0))
                .assertFailedWith(Exception.class, "BOOM!!!");
    }

    @Test
    public void testOnFailureTransformWithNonPassingPredicate() {
        Multi.createFrom().<Integer> failure(new RuntimeException("first"))
                .onFailure(IOException.class::isInstance)
                .transform(e -> new Exception("BOOM!!!"))
                .subscribe().withSubscriber(AssertSubscriber.create(0))
                .assertFailedWith(RuntimeException.class, "first");
    }

    @Test
    public void testOnFailureTransformWithPredicateThrowingException() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> failure(new RuntimeException("first"))
                .onFailure(f -> {
                    throw new IllegalArgumentException("bad");
                })
                .transform(e -> new Exception("BOOM"))
                .subscribe().withSubscriber(AssertSubscriber.create(0))
                .assertFailedWith(CompositeException.class, "first")
                .assertFailedWith(CompositeException.class, "bad");

        assertThat(subscriber.getFailure()).hasMessageNotContaining("BOOM");
    }

    @Test
    public void testOnFailureRecoverWithItemAndPredicate() {
        Multi.createFrom().<Integer> failure(new IOException())
                .onFailure(IOException.class::isInstance).recoverWithItem(42)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompleted()
                .assertItems(42);
    }

    @Test
    public void testOnFailureRecoverWithItemAndPredicateNotPassing() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .onFailure(IllegalStateException.class::isInstance).recoverWithItem(42)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testOnFailureRecoverWithCompletionAndPredicate() {
        Multi.createFrom().<Integer> failure(new IOException())
                .onFailure(IOException.class::isInstance).recoverWithCompletion()
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }
}
