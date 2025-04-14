package io.smallrye.mutiny.operators;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.MultiEmitter;
import junit5.support.InfrastructureResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class MultiDistinctUntilChangedTest {

    @Test
    public void distinctUntilChangedShouldEmitAllWhenNoConsecutiveDuplicates() {
        Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 2, 4, 2, 4);

        Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .onItem().distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 2, 4, 2, 4);
    }

    @Test
    public void distinctUntilChangedShouldFilterConsecutiveDuplicates() {
        Multi.createFrom().items(1, 2, 3, 4, 4, 4, 2, 4, 2, 4)
                .distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 2, 4, 2, 4);

        Multi.createFrom().items(1, 2, 3, 4, 4, 4, 2, 4, 2, 4)
                .onItem().distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 2, 4, 2, 4);
    }

    @Test
    public void distinctUntilChangedShouldFilterConsecutiveDuplicatesV2() {
        Multi.createFrom().items(1, 2, 3, 3, 3, 3, 3, 4, 2, 4, 2, 4)
                .distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 2, 4, 2, 4);

        Multi.createFrom().items(1, 2, 3, 3, 3, 3, 3, 4, 2, 4, 2, 4)
                .onItem().distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 2, 4, 2, 4);
    }

    @Test
    public void distinctUntilChangedShouldFailIfPredicateNull() {
        distinctUntilChangedShouldFailIfPredicateNull(() -> Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4).distinctUntilChanged(null));
        distinctUntilChangedShouldFailIfPredicateNull(() -> Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4).onItem().distinctUntilChanged(null));
    }

    private void distinctUntilChangedShouldFailIfPredicateNull(Supplier<Multi<? extends Integer>> multi) {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        Multi.createFrom().deferred(multi).subscribe(subscriber);

        subscriber.awaitFailure()
                .assertFailedWith(IllegalArgumentException.class, "`mapper` must not be `null`");
    }

    @Test
    public void distinctUntilChangedShouldFilterWhenPredicateIsGreaterThan() {
        // Predicate: (a, b) -> a >= b  (emit only if current > previous)
        Multi.createFrom().items(1, 1, 2, 3, 4, 2, 4, 2, 4).
                distinctUntilChanged((a, b) -> a >= b)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);

        Multi.createFrom().items(1, 1, 2, 3, 4, 2, 4, 2, 4)
                .onItem().distinctUntilChanged((a, b) -> a >= b)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void distinctUntilChangedShouldFilterWhenPredicateIsGreaterOrEqualsThan() {
        // Predicate: (a, b) -> a > b (emit only if current >= previous)
        Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .distinctUntilChanged((a, b) -> a > b)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 4, 4);

        Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .onItem().distinctUntilChanged((a, b) -> a > b)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 4, 4);
    }

    @Test
    public void distinctUntilChangedShouldFilterWhenPredicateIsLessThan() {
        // Predicate: (a, b) -> a <= b (emit only if current < previous)
        Multi.createFrom().items(5, 2, 3, 4, 2, 4, 2, 4)
                .distinctUntilChanged((a, b) -> a <= b)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(5, 2);

        Multi.createFrom().items(5, 2, 3, 4, 2, 4, 2, 4)
                .onItem().distinctUntilChanged((a, b) -> a <= b)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(5, 2);
    }

    @Test
    public void distinctUntilChangedShouldFilterWhenPredicateIsLessOrEqualsThan() {
        // Predicate: (a, b) -> a < b (emit only if current < previous)
        Multi.createFrom().items(5, 2, 3, 4, 2, 4, 2, 4, 1)
                .distinctUntilChanged((a, b) -> a < b)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(5, 2, 2, 2, 1);

        Multi.createFrom().items(5, 2, 3, 4, 2, 4, 2, 4, 1)
                .onItem().distinctUntilChanged((a, b) -> a < b)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(5, 2, 2, 2, 1);
    }

    @Test
    public void distinctUntilChangedShouldHandleCustomEqualityWithNoEquality() {
        // Tests distinctUntilChanged with a custom predicate (KeyTester::equals)
        // where all KeyTester objects are considered different.
        KeyTester kt1 = new KeyTester(1, "foo");
        KeyTester kt2 = new KeyTester(2, "bar");
        KeyTester kt3 = new KeyTester(3, "baz");
        KeyTester kt4 = new KeyTester(4, "foo-foo");
        KeyTester kt5 = new KeyTester(2, "foo-bar");
        KeyTester kt6 = new KeyTester(4, "foo-baz");
        KeyTester kt7 = new KeyTester(2, "bar-bar");
        KeyTester kt8 = new KeyTester(4, "bar-baz");

        Multi.createFrom().items(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8)
                .distinctUntilChanged(KeyTester::equals)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8);


        Multi.createFrom().items(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8)
                .onItem().distinctUntilChanged(KeyTester::equals)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8);
    }

    @Test
    public void distinctUntilChangedShouldHandleCustomEqualityWithOneEquality() {
        // Tests distinctUntilChanged with KeyTester::equals, where some objects are equal.
        KeyTester kt1 = new KeyTester(1, "foo");
        KeyTester kt2 = new KeyTester(2, "bar");
        KeyTester kt3 = new KeyTester(3, "baz");
        KeyTester kt4 = new KeyTester(4, "foo-foo");
        KeyTester kt5 = new KeyTester(2, "foo-bar");
        KeyTester kt6 = new KeyTester(4, "foo-foo");
        KeyTester kt7 = new KeyTester(4, "foo-foo");
        KeyTester kt8 = new KeyTester(2, "bar-bar");
        KeyTester kt9 = new KeyTester(4, "bar-baz");

        Multi.createFrom().items(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8, kt9)
                .distinctUntilChanged(KeyTester::equals)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(kt1, kt2, kt3, kt4, kt5, kt7, kt8, kt9);


        Multi.createFrom().items(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8, kt9)
                .onItem().distinctUntilChanged(KeyTester::equals)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(kt1, kt2, kt3, kt4, kt5, kt7, kt8, kt9);
    }

    @Test
    public void distinctUntilChangedShouldHandleAllEqualItems() {
        // Tests distinctUntilChanged when all items are equal according to KeyTester::equals.
        KeyTester kt1 = new KeyTester(1, "foo");
        KeyTester kt2 = new KeyTester(1, "foo");
        KeyTester kt3 = new KeyTester(1, "foo");
        KeyTester kt4 = new KeyTester(1, "foo");
        KeyTester kt5 = new KeyTester(1, "foo");
        KeyTester kt6 = new KeyTester(1, "foo");
        KeyTester kt7 = new KeyTester(1, "foo");
        KeyTester kt8 = new KeyTester(1, "foo");
        KeyTester kt9 = new KeyTester(1, "foo");


        Multi.createFrom().items(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8, kt9)
                .distinctUntilChanged(KeyTester::equals)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(kt1);

        Multi.createFrom().items(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8, kt9)
                .onItem().distinctUntilChanged(KeyTester::equals)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(kt1);
    }

    @Test
    public void distinctUntilChangedShouldHandleAllEqualItemsWithNoPredicate() {
        // Tests distinctUntilChanged when all items are equal (according to Object.equals)
        KeyTester kt1 = new KeyTester(1, "foo");
        KeyTester kt2 = new KeyTester(1, "foo");
        KeyTester kt3 = new KeyTester(1, "foo");
        KeyTester kt4 = new KeyTester(1, "foo");
        KeyTester kt5 = new KeyTester(1, "foo");
        KeyTester kt6 = new KeyTester(1, "foo");
        KeyTester kt7 = new KeyTester(1, "foo");
        KeyTester kt8 = new KeyTester(1, "foo");
        KeyTester kt9 = new KeyTester(1, "foo");


        Multi.createFrom().items(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8, kt9)
                .onItem().distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(kt1);

        Multi.createFrom().items(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8, kt9)
                .distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(kt1);
    }

    @Test
    public void distinctUntilChangedShouldHandleAllEqualItemsWithInvertedPredicate() {
        // Tests distinctUntilChanged with a predicate that always returns false (inverted equality)
        KeyTester kt1 = new KeyTester(1, "foo");
        KeyTester kt2 = new KeyTester(1, "foo");
        KeyTester kt3 = new KeyTester(1, "foo");
        KeyTester kt4 = new KeyTester(1, "foo");
        KeyTester kt5 = new KeyTester(1, "foo");
        KeyTester kt6 = new KeyTester(1, "foo");
        KeyTester kt7 = new KeyTester(1, "foo");
        KeyTester kt8 = new KeyTester(1, "foo");
        KeyTester kt9 = new KeyTester(1, "foo");


        Multi.createFrom().items(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8, kt9)
                .distinctUntilChanged((a, b) -> !a.equals(b))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8, kt9);

        Multi.createFrom().items(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8, kt9)
                .onItem().distinctUntilChanged((a, b) -> !a.equals(b))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8, kt9);
    }


    @Test
    public void distinctUntilChangedShouldPropagateUpstreamFailure() {
        Multi.createFrom().<Integer>failure(new IOException("boom"))
                .distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom");


        Multi.createFrom().<Integer>failure(new IOException("boom"))
                .onItem().distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void distinctUntilChangedShouldPropagateUpstreamFailureWithPredicate() {
        Multi.createFrom().<Integer>failure(new IOException("boom"))
                .distinctUntilChanged(Objects::equals)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom");

        Multi.createFrom().<Integer>failure(new IOException("boom"))
                .onItem().distinctUntilChanged(Objects::equals)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom");
    }


    @Test
    public void distinctUntilChangedShouldRejectNullSubscriber() {
        assertThrows(NullPointerException.class, () -> Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .onItem().distinctUntilChanged()
                .subscribe(null));

        assertThrows(NullPointerException.class, () -> Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .distinctUntilChanged()
                .subscribe(null));
    }

    @Test
    public void distinctUntilChangedShouldRejectNullSubscriberWithPredicate() {
        assertThrows(NullPointerException.class, () -> Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .distinctUntilChanged(Objects::equals)
                .subscribe(null));

        assertThrows(NullPointerException.class, () -> Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .onItem().distinctUntilChanged(Objects::equals)
                .subscribe(null));
    }

    @Test
    public void distinctUntilChangedShouldWorkWithRange() {
        Multi.createFrom().range(1, 5)
                .distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);


        Multi.createFrom().range(1, 5)
                .onItem().distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void distinctUntilChangedShouldWorkWithRangeAndPredicate() {
        Multi.createFrom().range(1, 5)
                .distinctUntilChanged((a, b) -> a > b)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);


        Multi.createFrom().range(1, 5)
                .onItem().distinctUntilChanged((a, b) -> a > b)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void distinctUntilChangedShouldWorkWithRangeAndPredicateV2() {
        Multi.createFrom().range(1, 5)
                .distinctUntilChanged((a, b) -> a >= b)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);


        Multi.createFrom().range(1, 5)
                .onItem().distinctUntilChanged((a, b) -> a >= b)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void distinctUntilChangedShouldNotEmitAfterCancellation() {
        distinctUntilChangedShouldNotEmitAfterCancellation(emitter -> Multi.createFrom().emitter(
                        (Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .distinctUntilChanged());

        distinctUntilChangedShouldNotEmitAfterCancellation(emitter -> Multi.createFrom().emitter(
                        (Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onItem().distinctUntilChanged());
    }

    private void distinctUntilChangedShouldNotEmitAfterCancellation(Function<AtomicReference<MultiEmitter<? super Integer>>, Multi<Integer>> multi) {
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();

        AssertSubscriber<Integer> subscriber = multi.apply(emitter)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        emitter.get().emit(1).emit(2).emit(1);
        subscriber.assertItems(1, 2, 1);

        subscriber.cancel();
        emitter.get().emit(1).emit(3).emit(4);
        subscriber.assertItems(1, 2, 1);
    }


    @Test
    public void distinctUntilChangedShouldNotEmitAfterCancellationWithPredicate() {
        distinctUntilChangedShouldNotEmitAfterCancellationWithPredicate(emitter -> Multi.createFrom().emitter(
                        (Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .distinctUntilChanged((a, b) -> a > b));

        distinctUntilChangedShouldNotEmitAfterCancellationWithPredicate(emitter -> Multi.createFrom().emitter(
                        (Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onItem().distinctUntilChanged((a, b) -> a > b));
    }

    private void distinctUntilChangedShouldNotEmitAfterCancellationWithPredicate(Function<AtomicReference<MultiEmitter<? super Integer>>, Multi<Integer>> multi) {
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();

        AssertSubscriber<Integer> subscriber = multi.apply(emitter)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        emitter.get().emit(1).emit(2).emit(1);
        subscriber.assertItems(1, 2);

        subscriber.cancel();
        emitter.get().emit(1).emit(3).emit(4);
        subscriber.assertItems(1, 2);
    }

    @Test
    public void distinctUntilChangedShouldEmitAllIfNotCancelled() {

        distinctUntilChangedShouldEmitAllIfNotCancelled(emitter -> Multi.createFrom().emitter(
                        (Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onItem().distinctUntilChanged());

        distinctUntilChangedShouldEmitAllIfNotCancelled(emitter -> Multi.createFrom().emitter(
                        (Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .distinctUntilChanged());
    }

    private void distinctUntilChangedShouldEmitAllIfNotCancelled(Function<AtomicReference<MultiEmitter<? super Integer>>, Multi<Integer>> multi) {
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();

        AssertSubscriber<Integer> subscriber = multi.apply(emitter)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        emitter.get().emit(1).emit(2).emit(1);
        subscriber.assertItems(1, 2, 1);

        emitter.get().emit(1).emit(3).emit(4);
        subscriber.assertItems(1, 2, 1, 3, 4);
    }

    @Test
    public void distinctUntilChangedShouldEmitCorrectlyIfNotCancelledWithPredicate() {

        distinctUntilChangedShouldEmitCorrectlyIfNotCancelledWithPredicate(emitter -> Multi.createFrom().emitter(
                        (Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .distinctUntilChanged((a, b) -> a > b));

        distinctUntilChangedShouldEmitCorrectlyIfNotCancelledWithPredicate(emitter -> Multi.createFrom().emitter(
                        (Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onItem().distinctUntilChanged((a, b) -> a > b));
    }

    private void distinctUntilChangedShouldEmitCorrectlyIfNotCancelledWithPredicate(Function<AtomicReference<MultiEmitter<? super Integer>>, Multi<Integer>> multi) {
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();

        AssertSubscriber<Integer> subscriber = multi.apply(emitter)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        emitter.get().emit(1).emit(2).emit(1);
        subscriber.assertItems(1, 2);

        emitter.get().emit(1).emit(3).emit(4);
        subscriber.assertItems(1, 2, 3, 4);
    }


    @Test
    public void distinctUntilChangedShouldHandleExceptionInPredicate() {
        distinctUntilChangedShouldHandleExceptionInPredicate(emitter -> Multi.createFrom().emitter(
                        (Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .distinctUntilChanged((a, b) -> {
                    throw new TestException("boom");
                }));

        distinctUntilChangedShouldHandleExceptionInPredicate(emitter -> Multi.createFrom().emitter(
                        (Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onItem().distinctUntilChanged((a, b) -> {
                    throw new TestException("boom");
                }));
    }

    private void distinctUntilChangedShouldHandleExceptionInPredicate(Function<AtomicReference<MultiEmitter<? super Integer>>, Multi<Integer>> multi) {
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = multi.apply(emitter)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        emitter.get().emit(1).emit(2).complete();
        subscriber.assertFailedWith(TestException.class, "boom");
    }


    @Test
    public void distinctUntilChangedShouldHandleOnItemAfterCancellation() {
        AtomicReference<Flow.Subscriber<? super Integer>> ref = new AtomicReference<>();
        AbstractMulti<Integer> upstream = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(mock(Flow.Subscription.class));
                ref.set(subscriber);
            }
        };

        upstream
                .onItem().distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .run(() -> ref.get().onNext(1))
                .assertItems(1)
                .request(1)
                .run(() -> ref.get().onNext(1))
                .run(() -> ref.get().onNext(3))
                .run(() -> ref.get().onNext(3))
                .run(() -> ref.get().onNext(3))
                .run(() -> ref.get().onNext(5))
                .assertItems(1, 3, 5)
                .cancel()
                .run(() -> ref.get().onNext(4))
                .assertItems(1, 3, 5);

        upstream
                .onItem().distinctUntilChanged((a, b) -> a >= b)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .run(() -> ref.get().onNext(1))
                .assertItems(1)
                .request(1)
                .run(() -> ref.get().onNext(1))
                .run(() -> ref.get().onNext(3))
                .run(() -> ref.get().onNext(1))
                .run(() -> ref.get().onNext(2))
                .run(() -> ref.get().onNext(5))
                .assertItems(1, 3, 5)
                .cancel()
                .run(() -> ref.get().onNext(4))
                .assertItems(1, 3, 5);

        upstream
                .distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .run(() -> ref.get().onNext(1))
                .assertItems(1)
                .request(1)
                .run(() -> ref.get().onNext(1))
                .run(() -> ref.get().onNext(3))
                .run(() -> ref.get().onNext(3))
                .run(() -> ref.get().onNext(3))
                .run(() -> ref.get().onNext(5))
                .assertItems(1, 3, 5)
                .cancel()
                .run(() -> ref.get().onNext(4))
                .assertItems(1, 3, 5);

        upstream
                .distinctUntilChanged((a, b) -> a >= b)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .run(() -> ref.get().onNext(1))
                .assertItems(1)
                .request(1)
                .run(() -> ref.get().onNext(1))
                .run(() -> ref.get().onNext(3))
                .run(() -> ref.get().onNext(1))
                .run(() -> ref.get().onNext(2))
                .run(() -> ref.get().onNext(5))
                .assertItems(1, 3, 5)
                .cancel()
                .run(() -> ref.get().onNext(4))
                .assertItems(1, 3, 5);
    }

    @Test
    public void distinctUntilChangedShouldHandleEmptyStream() {
        Multi.createFrom().<Integer>empty()
                .distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems();

        Multi.createFrom().<Integer>empty()
                .onItem().distinctUntilChanged()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems();
    }

    private static class KeyTester {

        private final int id;
        private final String text;

        private KeyTester(int id, String text) {
            this.id = id;
            this.text = text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            KeyTester keyTester = (KeyTester) o;
            return id == keyTester.id && Objects.equals(text, keyTester.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, text);
        }
    }

    private static class TestException extends RuntimeException {
        public TestException(String message) {
            super(message);
        }
    }

}
