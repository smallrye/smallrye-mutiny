package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.helpers.spies.MultiOnCancellationSpy;
import io.smallrye.mutiny.helpers.spies.Spy;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.MultiEmitter;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class MultiDistinctTest {

    @Test
    public void testDistinct() {
        Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .select().distinct()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testDistinctWithComparator() {
        Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .select().distinct(Integer::compareTo)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testDistinctWithNullComparator() {
        Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .select().distinct((Comparator<Integer>) null)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testDistinctWithComparatorReturningAlways0() {
        Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .select().distinct((a, b) -> 0)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1);
    }

    @Test
    public void testDistinctWithComparatorReturningAlways1() {
        //noinspection ComparatorMethodParameterNotUsed
        Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .select().distinct((a, b) -> 1)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 2, 4, 2, 4);
    }

    @Test
    public void testDistinctByKeyWithIdentityExtractor() {

        KeyTester kt1 = new KeyTester(1, "foo");
        KeyTester kt2 = new KeyTester(2, "bar");
        KeyTester kt3 = new KeyTester(3, "baz");
        KeyTester kt4 = new KeyTester(4, "foo-foo");
        KeyTester kt5 = new KeyTester(2, "foo-bar");
        KeyTester kt6 = new KeyTester(4, "foo-baz");
        KeyTester kt7 = new KeyTester(2, "bar-bar");
        KeyTester kt8 = new KeyTester(4, "bar-baz");

        Multi.createFrom().items(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8)
                .select().distinct(Function.identity())
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8);
    }

    @Test
    public void testDistinctByKeyWithExtractor() {

        KeyTester kt1 = new KeyTester(1, "foo");
        KeyTester kt2 = new KeyTester(2, "bar");
        KeyTester kt3 = new KeyTester(3, "baz");
        KeyTester kt4 = new KeyTester(4, "foo-foo");
        KeyTester kt5 = new KeyTester(2, "foo-bar");
        KeyTester kt6 = new KeyTester(4, "foo-baz");
        KeyTester kt7 = new KeyTester(2, "bar-bar");
        KeyTester kt8 = new KeyTester(4, "bar-baz");

        Multi.createFrom().items(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8)
                .select().distinct(kt -> kt.id)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(kt1, kt2, kt3, kt4);
    }

    @Test
    public void testDistinctWithNullExtractor() {

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                        .select().distinct((Function<Integer, Integer>) null)
                        .subscribe()
                        .withSubscriber(AssertSubscriber.create(10)))
                .withMessageContaining("`keyExtractor` must not be `null`");
    }

    @Test
    public void testDistinctByKeyReturningSameKey() {

        KeyTester kt1 = new KeyTester(1, "foo");
        KeyTester kt2 = new KeyTester(2, "bar");
        KeyTester kt3 = new KeyTester(3, "baz");
        KeyTester kt4 = new KeyTester(4, "foo-foo");
        KeyTester kt5 = new KeyTester(2, "foo-bar");
        KeyTester kt6 = new KeyTester(4, "foo-baz");
        KeyTester kt7 = new KeyTester(2, "bar-bar");
        KeyTester kt8 = new KeyTester(4, "bar-baz");

        Multi.createFrom().items(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8)
                .select().distinct(a -> 0)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(kt1);
    }

    @Test
    public void testDistinctByKeyReturningDifferentKey() {

        AtomicInteger counter = new AtomicInteger();

        KeyTester kt1 = new KeyTester(1, "foo");
        KeyTester kt2 = new KeyTester(2, "bar");
        KeyTester kt3 = new KeyTester(3, "baz");
        KeyTester kt4 = new KeyTester(4, "foo-foo");
        KeyTester kt5 = new KeyTester(2, "foo-bar");
        KeyTester kt6 = new KeyTester(4, "foo-baz");
        KeyTester kt7 = new KeyTester(2, "bar-bar");
        KeyTester kt8 = new KeyTester(4, "bar-baz");

        Multi.createFrom().items(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8)
                .select().distinct(a -> counter.getAndIncrement())
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(kt1, kt2, kt3, kt4, kt5, kt6, kt7, kt8);
    }

    @Test
    public void testDistinctWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .select().distinct()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDistinctWithComparatorWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .select().distinct(Integer::compareTo)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDistinctByKeyWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .select().distinct(Function.identity())
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testThatNullSubscriberAreRejectedDistinct() {
        assertThrows(NullPointerException.class, () -> Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .select().distinct()
                .subscribe(null));
    }

    @Test
    public void testThatNullSubscriberAreRejectedDistinctByKey() {
        assertThrows(NullPointerException.class, () -> Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .select().distinct(Function.identity())
                .subscribe(null));
    }

    @Test
    public void testThatNullSubscriberAreRejectedSkipRepetitions() {
        assertThrows(NullPointerException.class, () -> Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .skip().repetitions()
                .subscribe(null));
    }

    @Test
    public void testDistinctOnAStreamWithoutDuplicates() {
        Multi.createFrom().range(1, 5)
                .select().distinct()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testDistinctByKeyOnAStreamWithoutDuplicates() {
        Multi.createFrom().range(1, 5)
                .select().distinct(Function.identity())
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testSkipRepetitionsWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .skip().repetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testSkipRepetitions() {
        Multi.createFrom().items(1, 2, 3, 4, 4, 2, 2, 4, 1, 1, 2, 4)
                .skip().repetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 2, 4, 1, 2, 4);
    }

    @Test
    public void testSkipRepetitionsWithComparator() {
        Multi.createFrom().items(1, 2, 3, 4, 4, 2, 2, 4, 1, 1, 2, 4)
                .skip().repetitions(Integer::compareTo)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 2, 4, 1, 2, 4);
    }

    @Test
    public void testSkipRepetitionsWithComparatorAlwaysReturning0() {
        Multi.createFrom().items(1, 2, 3, 4, 4, 2, 2, 4, 1, 1, 2, 4)
                .skip().repetitions((a, b) -> 0)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1);
    }

    @Test
    public void testSkipRepetitionsWithComparatorAlwaysReturning1() {
        //noinspection ComparatorMethodParameterNotUsed
        Multi.createFrom().items(1, 2, 3, 4, 4, 2, 2, 4, 1, 1, 2, 4)
                .skip().repetitions((a, b) -> 1)
                .subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 4, 2, 2, 4, 1, 1, 2, 4);
    }

    @Test
    public void testDroppedRepetitions() {
        Multi.createFrom().items(1, 2, 3, 4, 4, 2, 2, 4, 1, 1, 2, 4)
                .skip().repetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 2, 4, 1, 2, 4);
    }

    @Test
    public void testSkipRepetitionsWithCancellation() {
        AtomicLong count = new AtomicLong();
        MultiOnCancellationSpy<Long> multi = Spy
                .onCancellation(Multi.createFrom().ticks().every(Duration.ofMillis(1)));
        AssertSubscriber<Long> subscriber = multi
                .onItem().transform(l -> {
                    if (count.getAndIncrement() % 2 == 0) {
                        return l;
                    } else {
                        return l - 1;
                    }
                })
                .skip().repetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        multi.assertNotCancelled();
        subscriber.awaitNextItems(10).cancel();
        multi.assertCancelled();
    }

    @Test
    public void testSkipRepetitionsWithImmediateCancellation() {
        AtomicLong count = new AtomicLong();
        AtomicBoolean cancelled = new AtomicBoolean();
        Multi.createFrom().ticks().every(Duration.ofMillis(1))
                .onCancellation().invoke(() -> cancelled.set(true))
                .onItem().transform(l -> {
                    if (count.getAndIncrement() % 2 == 0) {
                        return l;
                    } else {
                        return l - 1;
                    }
                })
                .skip().repetitions()
                .subscribe().withSubscriber(new AssertSubscriber<>(Long.MAX_VALUE, true));

        assertThat(cancelled).isTrue();
        assertThat(count).hasValue(0);
    }

    @Test
    public void testSkipRepetitionsOnAStreamWithoutDuplicates() {
        Multi.createFrom().range(1, 5)
                .skip().repetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testNoEmissionAfterCancellation() {
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().emitter(
                (Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .select().distinct()
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
    public void testDistinctExceptionInHashCode() {
        AtomicReference<MultiEmitter<? super BadlyComparableStuffOnHashCode>> emitter = new AtomicReference<>();
        AssertSubscriber<BadlyComparableStuffOnHashCode> subscriber = Multi.createFrom().emitter(
                (Consumer<MultiEmitter<? super BadlyComparableStuffOnHashCode>>) emitter::set)
                .select().distinct()
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        BadlyComparableStuffOnHashCode item1 = new BadlyComparableStuffOnHashCode();
        BadlyComparableStuffOnHashCode item2 = new BadlyComparableStuffOnHashCode();
        emitter.get().emit(item1).emit(item2).complete();
        subscriber.assertFailedWith(TestException.class, "boom");
    }

    @Test
    public void testDistinctExceptionInComparator() {
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().emitter(
                (Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .select().distinct((a, b) -> {
                    throw new TestException("boom");
                })
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        emitter.get().emit(1).emit(2).complete();
        subscriber.assertFailedWith(TestException.class, "boom");
    }

    @Test
    public void testDistinctByKeyExceptionInExtractor() {
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().emitter(
                (Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .select().distinct(a -> {
                    throw new TestException("boom");
                })
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        emitter.get().emit(1).emit(2).complete();
        subscriber.assertFailedWith(TestException.class, "boom");
    }

    @Test
    public void testSkipRepetitionsExceptionInEquals() {
        AtomicReference<MultiEmitter<? super BadlyComparableStuffOnEquals>> emitter = new AtomicReference<>();
        AssertSubscriber<BadlyComparableStuffOnEquals> subscriber = Multi.createFrom().emitter(
                (Consumer<MultiEmitter<? super BadlyComparableStuffOnEquals>>) emitter::set)
                .skip().repetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        BadlyComparableStuffOnEquals item1 = new BadlyComparableStuffOnEquals();
        BadlyComparableStuffOnEquals item2 = new BadlyComparableStuffOnEquals();
        emitter.get().emit(item1).emit(item2).complete();
        subscriber
                .awaitFailure(t -> assertThat(t)
                        .isInstanceOf(TestException.class)
                        .hasMessageContaining("boom"));
    }

    @Test
    public void testSkipRepetitionsExceptionInComparator() {
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().emitter(
                (Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .skip().repetitions((a, b) -> {
                    throw new TestException("boom");
                })
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        emitter.get().emit(1).emit(2).complete();
        subscriber
                .awaitFailure(t -> assertThat(t)
                        .isInstanceOf(TestException.class)
                        .hasMessageContaining("boom"));
    }

    @Test
    public void testOnItemAfterCancellation() {
        AtomicReference<Flow.Subscriber<? super Integer>> ref = new AtomicReference<>();
        AbstractMulti<Integer> upstream = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(mock(Flow.Subscription.class));
                ref.set(subscriber);
            }
        };

        upstream
                .select().distinct()
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .run(() -> ref.get().onNext(1))
                .assertItems(1)
                .request(1)
                .run(() -> ref.get().onNext(1))
                .run(() -> ref.get().onNext(3))
                .assertItems(1, 3)
                .cancel()
                .run(() -> ref.get().onNext(4))
                .assertItems(1, 3);

        upstream
                .select().distinct(Function.identity())
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .run(() -> ref.get().onNext(1))
                .assertItems(1)
                .request(1)
                .run(() -> ref.get().onNext(1))
                .run(() -> ref.get().onNext(3))
                .assertItems(1, 3)
                .cancel()
                .run(() -> ref.get().onNext(4))
                .assertItems(1, 3);

        upstream
                .skip().repetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .run(() -> ref.get().onNext(1))
                .assertItems(1)
                .request(1)
                .run(() -> ref.get().onNext(1))
                .run(() -> ref.get().onNext(3))
                .assertItems(1, 3)
                .cancel()
                .run(() -> ref.get().onNext(4))
                .assertItems(1, 3);
    }

    private static class BadlyComparableStuffOnHashCode {

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        @Override
        public boolean equals(Object obj) {
            return Objects.equals(obj, this);
        }

        @Override
        public int hashCode() {
            throw new TestException("boom");
        }
    }

    private static class BadlyComparableStuffOnEquals {

        @Override
        public boolean equals(Object obj) {
            throw new TestException("boom");
        }
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
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            KeyTester keyTester = (KeyTester) o;
            return id == keyTester.id && Objects.equals(text, keyTester.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, text);
        }
    }

}
