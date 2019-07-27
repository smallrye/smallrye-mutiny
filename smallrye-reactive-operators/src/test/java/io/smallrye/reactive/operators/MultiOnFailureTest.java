package io.smallrye.reactive.operators;

import io.smallrye.reactive.CompositeException;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.subscription.MultiEmitter;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiOnFailureTest {

    @Test
    public void testThatRecoverWithMultiNotCalledWhenNoFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(20);

        Multi.createFrom().range(1, 10)
                .onFailure().recoverWithMulti(v -> Multi.createFrom().range(50, 100))
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testRecoverWithMultiWithFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(20);

        Multi.createFrom().<Integer>failure(new IllegalStateException("boom"))
                .onFailure().recoverWithMulti(v -> Multi.createFrom().range(50, 52))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertReceived(50, 51)
                .assertHasNotFailed()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testRecoverWithMultiWithPredicate() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(1);

        Multi.createFrom().<Integer>failure(new IllegalStateException("boom"))
                .onFailure(IllegalStateException.class).recoverWithMulti(v -> Multi.createFrom().result(42))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertReceived(42)
                .assertHasNotFailed()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testRecoverWithMultiWithPredicateNotPassing() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(1);

        Multi.createFrom().<Integer>failure(new IllegalStateException("boom"))
                .onFailure(IOException.class).recoverWithMulti(v -> Multi.createFrom().result(42))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testRecoverWithMultiWithPredicateThrowingException() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(1);

        Multi.createFrom().<Integer>failure(new IllegalStateException("boom"))
                .onFailure(f -> {
                    throw new IllegalArgumentException("bad");
                }).recoverWithMulti(v -> Multi.createFrom().result(42))
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertHasFailedWith(CompositeException.class, "boom")
                .assertHasFailedWith(CompositeException.class, "bad");
    }

    @Test
    public void testOnFailureMap() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create();

        Multi.createFrom().<Integer>failure(new IllegalStateException("boom"))
                .onFailure().mapTo(f -> new IOException("kaboom!"))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertHasNoResults()
                .assertTerminated()
                .assertHasFailedWith(IOException.class, "kaboom!");
    }

    @Test
    public void testRequestOnTheMultiReturnedByRecoverWithMulti() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(0);

        Multi.createFrom()
                .<Integer>failure(new IllegalStateException("boom"))
                .onFailure().recoverWithMulti(v -> Multi.createFrom().range(50, 61))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertHasNoResults()
                .assertHasNotFailed()
                .assertNotTerminated();

        subscriber.request(4)
                .assertReceived(50, 51, 52, 53)
                .assertHasNotFailed()
                .assertNotTerminated();

        subscriber.request(5)
                .assertReceived(50, 51, 52, 53, 54, 55, 56, 57, 58)
                .assertHasNotFailed()
                .assertNotTerminated();

        subscriber.request(5)
                .assertReceived(50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60)
                .assertHasNotFailed()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testRecoverWithMultiWithSomeResulsubscriberBeforeFailing() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(20);

        AtomicReference<MultiEmitter<? super Integer>> reference = new AtomicReference<>();
        Multi.createFrom().<Integer>emitter(reference::set)
                .onFailure().recoverWithMulti(v -> Multi.createFrom().range(50, 55))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertSubscribed();

        reference.get().result(1)
                .result(2)
                .result(3)
                .result(4)
                .result(5)
                .failure(new IllegalStateException("boom"));

        subscriber.assertReceived(1, 2, 3, 4, 5, 50, 51, 52, 53, 54)
                .assertHasNotFailed()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testRecoverWithMultiWithSomeResulsubscriberBeforeFailingWithRequessubscriber() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(3);

        AtomicReference<MultiEmitter<? super Integer>> reference = new AtomicReference<>();
        Multi.createFrom().<Integer>emitter(reference::set)
                .onFailure().recoverWithMulti(v -> Multi.createFrom().range(50, 55))
                .subscribe().withSubscriber(subscriber);

        subscriber.assertSubscribed();

        reference.get().result(1)
                .result(2)
                .result(3)
                .result(4)
                .result(5)
                .failure(new IllegalStateException("boom"));

        subscriber.assertReceived(1, 2, 3)
                .request(5)
                .assertReceived(1, 2, 3, 4, 5, 50, 51, 52)
                .request(10)
                .assertHasNotFailed()
                .assertCompletedSuccessfully();
    }


    @Test
    public void testWhenRecoverWithMultiIsAlsoAFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(0);

        Multi.createFrom().<Integer>failure(new IOException("karambar"))
                .onFailure().recoverWithMulti(v -> {
            throw new IllegalStateException("kaboom!");
        })
                .subscribe().withSubscriber(subscriber);

        subscriber.assertHasNoResults()
                .assertTerminated()
                .assertHasFailedWith(CompositeException.class, "kaboom!")
                .assertHasFailedWith(CompositeException.class, "karambar");
    }

    @Test
    public void testWhenRecoverWithMultiReturnsNull() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(0);

        Multi.createFrom().<Integer>failure(new IOException("karambar"))
                .onFailure().recoverWithMulti(v -> null)
                .subscribe().withSubscriber(subscriber);

        subscriber.assertHasNoResults()
                .assertTerminated()
                .assertHasFailedWith(NullPointerException.class, "mapper");
    }


    @Test
    public void testRecoverWithResult() {
        Multi.createFrom().<Integer>failure(new IllegalStateException("boom"))
                .onFailure().recoverWithResult(42)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertCompletedSuccessfully()
                .assertReceived(42);
    }

    @Test
    public void testRecoverWithResultWithSupplier() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().<Integer>failure(new IllegalStateException("boom"))
                .onFailure().recoverWithResult(count::incrementAndGet);
        multi
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertCompletedSuccessfully()
                .assertReceived(1);

        multi
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertCompletedSuccessfully()
                .assertReceived(2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRecoverWithResultWithNull() {
        Multi.createFrom().<String>failure(new IllegalStateException("boom"))
                .onFailure().recoverWithResult((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRecoverWithResultWithNullSupplier() {
        Multi.createFrom().<String>failure(new IllegalStateException("boom"))
                .onFailure().recoverWithResult((Supplier<String>) null);
    }

    @Test
    public void testRecoverWithResultAndSupplierReturningNull() {
        Multi.createFrom().<Integer>failure(new IllegalStateException("boom"))
                .onFailure().recoverWithResult(() -> null)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertHasFailedWith(CompositeException.class, "boom")
                .assertHasFailedWith(CompositeException.class, "supplier");
    }

    @Test
    public void testRecoverWithCompletion() {
        Multi.createFrom().<Integer>failure(new IllegalStateException("boom"))
                .onFailure().recoverWithCompletion()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertCompletedSuccessfully()
                .assertHasNoResults();
    }


    @Test
    public void testRecoverWithMultiUsingEmitterAsFallback() {
        Multi<Integer> multi = Multi.createFrom().emitter(emitter ->
                emitter
                        .result(3)
                        .result(2)
                        .result(1)
                        .failure(new IOException("boom"))
        );

        AtomicInteger subscribed = new AtomicInteger();
        Multi<Integer> fallback = Multi.createFrom()
                .<Integer>emitter(s -> s.result(42).result(43).complete())
                .on().subscription(s -> subscribed.incrementAndGet());

        multi.onFailure()
                .recoverWithMulti(fallback)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(0))
                .assertSubscribed()
                .assertHasNoResults()
                .request(2)
                .run(() -> assertThat(subscribed).hasValue(0))
                .assertReceived(3, 2)
                .request(2)
                .assertReceived(3, 2, 1, 42)
                .run(() -> assertThat(subscribed).hasValue(1))
                .request(2)
                .assertReceived(3, 2, 1, 42, 43)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testRecoverWithResult2() {
        Multi<Integer> multi = Multi.createFrom().emitter(emitter ->
                emitter
                        .result(3)
                        .result(2)
                        .result(1)
                        .failure(new IOException("boom"))
        );

        AtomicInteger subscribed = new AtomicInteger();
        Multi<Integer> fallback = Multi.createFrom()
                .result(0)
                .on().subscription(s -> subscribed.incrementAndGet());

        multi.onFailure()
                .recoverWithMulti(fallback)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(0))
                .assertSubscribed()
                .assertHasNoResults()
                .request(2)
                .run(() -> assertThat(subscribed).hasValue(0))
                .assertReceived(3, 2)
                .request(2)
                .assertReceived(3, 2, 1, 0)
                .run(() -> assertThat(subscribed).hasValue(1))
                .assertCompletedSuccessfully();
    }


    @Test
    public void testOnFailureMapWithPredicate() {
        Multi.createFrom().<Integer>failure(new IOException())
                .onFailure(IOException.class::isInstance)
                .mapTo(e -> new Exception("BOOM!!!"))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(0))
                .assertHasFailedWith(Exception.class, "BOOM!!!");
    }

    @Test
    public void testOnFailureMapWithNonPassingPredicate() {
        Multi.createFrom().<Integer>failure(new RuntimeException("first"))
                .onFailure(IOException.class::isInstance)
                .mapTo(e -> new Exception("BOOM!!!"))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(0))
                .assertHasFailedWith(RuntimeException.class, "first");
    }

    @Test
    public void testOnFailureMapWithPredicateThrowingException() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer>failure(new RuntimeException("first"))
                .onFailure(f -> {
                    throw new IllegalArgumentException("bad");
                })
                .mapTo(e -> new Exception("BOOM"))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(0))
                .assertHasFailedWith(CompositeException.class, "first")
                .assertHasFailedWith(CompositeException.class, "bad");

        assertThat(subscriber.failures().get(0)).hasMessageNotContaining("BOOM");
    }

    @Test
    public void testOnFailureRecoverWithResultAndPredicate() {
        Multi.createFrom().<Integer>failure(new IOException())
                .onFailure(IOException.class::isInstance).recoverWithResult(42)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertCompletedSuccessfully()
                .assertReceived(42);
    }

    @Test
    public void testOnFailureRecoverWithResultAndPredicateNotPassing() {
        Multi.createFrom().<Integer>failure(new IOException("boom"))
                .onFailure(IllegalStateException.class::isInstance).recoverWithResult(42)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testOnFailureRecoverWithCompletionAndPredicate() {
        Multi.createFrom().<Integer>failure(new IOException())
                .onFailure(IOException.class::isInstance).recoverWithCompletion()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertCompletedSuccessfully()
                .assertHasNoResults();
    }
}
