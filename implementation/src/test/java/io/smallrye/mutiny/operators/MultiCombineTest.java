package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.spies.MultiOnCancellationSpy;
import io.smallrye.mutiny.helpers.spies.Spy;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.tuples.*;

public class MultiCombineTest {

    @Test
    public void testCombiningNothing() {
        AssertSubscriber<?> subscriber = Multi.createBy().combining().streams(Collections.emptyList())
                .using(l -> l)
                .onItem().disjoint()
                .subscribe().withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCombiningOne() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().combining().streams(
                Collections.singleton(Multi.createFrom().items(1, 2, 3)))
                .using(l -> l)
                .onItem().disjoint()
                .onItem().castTo(Integer.class)
                .subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertItems(1, 2, 3);
    }

    @Test
    public void testCombiningOneButEmpty() {
        AssertSubscriber<?> subscriber = Multi.createBy().combining().streams(Collections.singleton(Multi.createFrom().empty()))
                .using(l -> l)
                .onItem().disjoint()
                .subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testMergeOneButNever() {
        MultiOnCancellationSpy<Object> spy = Spy.onCancellation(Multi.createFrom().nothing());
        AssertSubscriber<?> subscriber = Multi.createBy().merging().streams(spy)
                .subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertNotTerminated()
                .cancel();

        assertThat(spy.isCancelled()).isTrue();
    }

    @Test
    public void combineIterableOfStreamsFollowedByAFlatMap() {
        Multi<Integer> multi1 = Multi.createFrom().item(1);
        Multi<Integer> multi2 = Multi.createFrom().item(2);
        Multi<Integer> multi3 = Multi.createFrom().item(3);
        Multi<Integer> multi4 = Multi.createFrom().item(4);

        Multi<Integer> combined = Multi.createBy()
                .combining().streams(Arrays.asList(multi1, multi2, multi3, multi4)).using(l -> l)
                .flatMap(l -> Multi.createFrom().iterable(l))
                .onItem().castTo(Integer.class);

        combined.subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testCombinationOfTwoStreamsAsTuple() {
        List<Tuple2<Integer, Integer>> list = Multi.createBy()
                .combining().streams(Multi.createFrom().range(1, 4), Multi.createFrom().range(2, 5)).asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).hasSize(3).containsExactly(Tuple2.of(1, 2), Tuple2.of(2, 3), Tuple2.of(3, 4));
    }

    @Test
    public void testCombinationOfAStreamWithItself() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3, 4);

        List<Tuple2<Integer, Integer>> list = Multi.createBy()
                .combining().streams(stream, stream).asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).hasSize(4).containsExactly(Tuple2.of(1, 1), Tuple2.of(2, 2),
                Tuple2.of(3, 3), Tuple2.of(4, 4));
    }

    @Test
    public void testCombinationOfThreeStreamsAsTuple() {
        Multi<Integer> s1 = Multi.createFrom().range(1, 4);
        Multi<Integer> s2 = Multi.createFrom().range(2, 5);
        Multi<Integer> s3 = Multi.createFrom().range(3, 6);
        List<Tuple3<Integer, Integer, Integer>> list = Multi.createBy()
                .combining().streams(s1, s2, s3).asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).hasSize(3).containsExactly(Tuple3.of(1, 2, 3), Tuple3.of(2, 3, 4), Tuple3.of(3, 4, 5));
    }

    @Test
    public void testCombinationOfFourStreamsAsTuple() {
        Multi<Integer> s1 = Multi.createFrom().range(1, 4);
        Multi<Integer> s2 = Multi.createFrom().range(2, 5);
        Multi<Integer> s3 = Multi.createFrom().range(3, 6);
        Multi<Integer> s4 = Multi.createFrom().range(4, 7);
        List<Tuple4<Integer, Integer, Integer, Integer>> list = Multi.createBy()
                .combining().streams(s1, s2, s3, s4).asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).hasSize(3)
                .containsExactly(Tuple4.of(1, 2, 3, 4), Tuple4.of(2, 3, 4, 5),
                        Tuple4.of(3, 4, 5, 6));
    }

    @Test
    public void testCombinationOfFiveStreamsAsTuple() {
        Multi<Integer> s1 = Multi.createFrom().range(1, 4);
        Multi<Integer> s2 = Multi.createFrom().range(2, 5);
        Multi<Integer> s3 = Multi.createFrom().range(3, 6);
        Multi<Integer> s4 = Multi.createFrom().range(4, 7);
        Multi<Integer> s5 = Multi.createFrom().range(5, 8);
        List<Tuple5<Integer, Integer, Integer, Integer, Integer>> list = Multi.createBy()
                .combining().streams(s1, s2, s3, s4, s5).asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).hasSize(3)
                .containsExactly(Tuple5.of(1, 2, 3, 4, 5),
                        Tuple5.of(2, 3, 4, 5, 6),
                        Tuple5.of(3, 4, 5, 6, 7));
    }

    @Test
    public void testCombinationOfSixStreamsAsTuple() {
        Multi<Integer> s1 = Multi.createFrom().range(1, 4);
        Multi<Integer> s2 = Multi.createFrom().range(2, 5);
        Multi<Integer> s3 = Multi.createFrom().range(3, 6);
        Multi<Integer> s4 = Multi.createFrom().range(4, 7);
        Multi<Integer> s5 = Multi.createFrom().range(5, 8);
        Multi<Integer> s6 = Multi.createFrom().range(6, 9);
        List<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> list = Multi.createBy()
                .combining().streams(s1, s2, s3, s4, s5, s6).asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).hasSize(3)
                .containsExactly(
                        Tuple6.of(1, 2, 3, 4, 5, 6),
                        Tuple6.of(2, 3, 4, 5, 6, 7),
                        Tuple6.of(3, 4, 5, 6, 7, 8));
    }

    @Test
    public void testCombinationOfSevenStreamsAsTuple() {
        Multi<Integer> s1 = Multi.createFrom().range(1, 4);
        Multi<Integer> s2 = Multi.createFrom().range(2, 5);
        Multi<Integer> s3 = Multi.createFrom().range(3, 6);
        Multi<Integer> s4 = Multi.createFrom().range(4, 7);
        Multi<Integer> s5 = Multi.createFrom().range(5, 8);
        Multi<Integer> s6 = Multi.createFrom().range(6, 9);
        Multi<Integer> s7 = Multi.createFrom().range(7, 10);
        List<Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>> list = Multi.createBy()
                .combining().streams(s1, s2, s3, s4, s5, s6, s7).asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).hasSize(3)
                .containsExactly(
                        Tuple7.of(1, 2, 3, 4, 5, 6, 7),
                        Tuple7.of(2, 3, 4, 5, 6, 7, 8),
                        Tuple7.of(3, 4, 5, 6, 7, 8, 9));
    }

    @Test
    public void testCombinationOfEightStreamsAsTuple() {
        Multi<Integer> s1 = Multi.createFrom().range(1, 4);
        Multi<Integer> s2 = Multi.createFrom().range(2, 5);
        Multi<Integer> s3 = Multi.createFrom().range(3, 6);
        Multi<Integer> s4 = Multi.createFrom().range(4, 7);
        Multi<Integer> s5 = Multi.createFrom().range(5, 8);
        Multi<Integer> s6 = Multi.createFrom().range(6, 9);
        Multi<Integer> s7 = Multi.createFrom().range(7, 10);
        Multi<Integer> s8 = Multi.createFrom().range(8, 11);
        List<Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> list = Multi.createBy()
                .combining().streams(s1, s2, s3, s4, s5, s6, s7, s8).asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).hasSize(3)
                .containsExactly(
                        Tuple8.of(1, 2, 3, 4, 5, 6, 7, 8),
                        Tuple8.of(2, 3, 4, 5, 6, 7, 8, 9),
                        Tuple8.of(3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void testCombinationOfNineStreamsAsTuple() {
        Multi<Integer> s1 = Multi.createFrom().range(1, 4);
        Multi<Integer> s2 = Multi.createFrom().range(2, 5);
        Multi<Integer> s3 = Multi.createFrom().range(3, 6);
        Multi<Integer> s4 = Multi.createFrom().range(4, 7);
        Multi<Integer> s5 = Multi.createFrom().range(5, 8);
        Multi<Integer> s6 = Multi.createFrom().range(6, 9);
        Multi<Integer> s7 = Multi.createFrom().range(7, 10);
        Multi<Integer> s8 = Multi.createFrom().range(8, 11);
        Multi<Integer> s9 = Multi.createFrom().range(9, 12);
        List<Tuple9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> list = Multi
                .createBy()
                .combining().streams(s1, s2, s3, s4, s5, s6, s7, s8, s9).asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).hasSize(3)
                .containsExactly(
                        Tuple9.of(1, 2, 3, 4, 5, 6, 7, 8, 9),
                        Tuple9.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
                        Tuple9.of(3, 4, 5, 6, 7, 8, 9, 10, 11));
    }

    @Test
    public void testCombinationWithBackPressure() {
        Multi<Integer> stream = Multi.createFrom().range(1, 5);
        AssertSubscriber<Integer> subscriber = Multi.createBy().combining().streams(stream, stream)
                .using(Integer::sum)
                .subscribe().withSubscriber(AssertSubscriber.create(0));

        subscriber.assertNotTerminated().assertHasNotReceivedAnyItem();

        subscriber.request(2)
                .assertItems(2, 4);

        subscriber.request(3)
                .assertItems(2, 4, 6, 8)
                .assertCompleted();
    }

    @Test
    public void testCombinationOfAStreamsOfDifferentSize() {
        Multi<Integer> stream1 = Multi.createFrom().items(1, 2, 3, 4);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7);

        List<Tuple2<Integer, Integer>> list = Multi.createBy()
                .combining().streams(stream1, stream2).asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).hasSize(4).containsExactly(Tuple2.of(1, 1), Tuple2.of(2, 2),
                Tuple2.of(3, 3), Tuple2.of(4, 4));

        list = Multi.createBy()
                .combining().streams(stream2, stream1).asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).hasSize(4).containsExactly(Tuple2.of(1, 1), Tuple2.of(2, 2),
                Tuple2.of(3, 3), Tuple2.of(4, 4));
    }

    @Test
    public void testCombinationOfAStreamsOfDifferentSizeUsingLatest() {
        Multi<Integer> stream1 = Multi.createFrom().items(1, 2, 3, 4);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7);

        List<Tuple2<Integer, Integer>> list = Multi.createBy()
                .combining().streams(stream1, stream2).latestItems().asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).hasSize(7).containsExactly(Tuple2.of(4, 1), Tuple2.of(4, 2),
                Tuple2.of(4, 3), Tuple2.of(4, 4), Tuple2.of(4, 5), Tuple2.of(4, 6), Tuple2.of(4, 7));

        list = Multi.createBy()
                .combining().streams(stream2, stream1).latestItems().asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).hasSize(4).containsExactly(Tuple2.of(7, 1), Tuple2.of(7, 2),
                Tuple2.of(7, 3), Tuple2.of(7, 4));
    }

    @Test
    public void testCombinationWithEmpty() {
        Multi.createBy().combining()
                .streams(Multi.createFrom().<Integer> empty(), Multi.createFrom().range(1, 2_000_000))
                .using(Integer::sum)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();

        Multi.createBy().combining()
                .streams(Multi.createFrom().range(1, 2_000_000), Multi.createFrom().<Integer> empty())
                .using(Integer::sum)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCombiningASingleStreamUsingIterable() {
        Multi.createBy().combining().streams(Collections.singletonList(Multi.createFrom().item(1))).using(l -> l.get(0))
                .onItem().castTo(Integer.class)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompleted()
                .assertItems(1);
    }

    @Test
    public void testCombiningASingleEmptyStreamUsingIterable() {
        Multi.createBy().combining().streams(Collections.singletonList(Multi.createFrom().empty())).using(l -> l.get(0))
                .onItem().castTo(Integer.class)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testThatIterableCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createBy().combining().streams(null));
    }

    @Test
    public void testThatStreamsCannotBeNull() {
        Multi<Integer> multi = Multi.createFrom().item(1);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createBy().combining().streams(null, multi));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createBy().combining().streams(multi, null));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createBy().combining().streams(null, multi, multi));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createBy().combining().streams(multi, null, multi));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createBy().combining().streams(multi, multi, multi, null));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createBy().combining().streams(multi, multi, multi, null, multi));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createBy().combining().streams(multi, multi, multi, multi, null));
    }

    @Test
    public void testThatCombinatorCannotBeNull() {
        Multi<Integer> multi = Multi.createFrom().item(1);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createBy().combining().streams(multi, multi).using((BiFunction<Integer, Integer, ?>) null));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createBy().combining().streams(multi, multi).using((Function<List<?>, ?>) null));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createBy().combining().streams(multi, multi, multi)
                        .using((Functions.Function3<Integer, Integer, Integer, ?>) null));
    }

    @Test
    public void testThatCombinatorCannotProduceNull() {
        Multi<Integer> multi = Multi.createFrom().item(1);

        Multi.createBy().combining().streams(multi, multi).using((i, j) -> null)
                .subscribe().withSubscriber(new AssertSubscriber<>(2))
                .assertFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testThatCombinatorCanThrowAnException() {
        Multi<Integer> multi = Multi.createFrom().item(1);

        Multi.createBy().combining().streams(multi, multi).using((i, j) -> {
            throw new IllegalArgumentException("boom");
        })
                .subscribe().withSubscriber(new AssertSubscriber<>(2))
                .assertFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testCombiningWithFailures() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3);
        Multi<Integer> multi2 = Multi.createFrom().emitter(e -> e.emit(1).fail(new IOException("boom")));

        Multi.createBy().combining().streams(multi, multi2).asTuple()
                .subscribe().withSubscriber(AssertSubscriber.create(3))
                .assertFailedWith(IOException.class, "boom")
                .assertItems(Tuple2.of(1, 1));
    }

    @Test
    public void testCombiningWithFailuresAndCollectFailure() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3);
        Multi<Integer> multi2 = Multi.createFrom().emitter(e -> e.emit(1).fail(new IOException("boom")));
        Multi<Integer> multi3 = Multi.createFrom().emitter(e -> e.emit(1).emit(2).fail(new IOException("boom")));

        Multi.createBy().combining().streams(multi, multi2, multi, multi3).collectFailures().asTuple()
                .subscribe().withSubscriber(AssertSubscriber.create(3))
                .assertFailedWith(IOException.class, "boom")
                .assertItems(Tuple4.of(1, 1, 1, 1));
    }

    @Test
    public void testCombiningWithFailuresAtSameTimeAndCollectFailure() {
        AtomicReference<MultiEmitter<? super Integer>> emitter1 = new AtomicReference<>();
        AtomicReference<MultiEmitter<? super Integer>> emitter2 = new AtomicReference<>();
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3);
        Multi<Integer> multi2 = Multi.createFrom().emitter(e -> {
            emitter1.set(e);
            e.emit(1);
        });
        Multi<Integer> multi3 = Multi.createFrom().emitter(e -> {
            emitter2.set(e);
            e.emit(1);
        });

        Multi.createBy().combining().streams(multi, multi2, multi, multi3).collectFailures().asTuple()
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertItems(Tuple4.of(1, 1, 1, 1))
                .request(2)
                .run(() -> {
                    emitter1.get().emit(2).fail(new IOException("boomA"));
                    emitter2.get().emit(2).fail(new IOException("boomB"));
                })
                .assertItems(Tuple4.of(1, 1, 1, 1), Tuple4.of(2, 2, 2, 2))
                .assertFailedWith(IOException.class, "boomA");
    }

    @Test
    public void testCombinationOfAFailingStream() {
        assertThrows(CompletionException.class, () -> {

            Multi<Integer> stream = Multi.createFrom().items(1, 2, 3, 4);
            Multi<Integer> fail = Multi.createFrom().failure(new IOException("boom"));

            Multi.createBy()
                    .combining().streams(stream, fail).asTuple()
                    .collect().asList().await().indefinitely();
        });
    }

    @Test
    public void testCombiningWithIterableWithSameSize() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3, 4);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3, 4);

        Multi.createBy().combining().streams(Arrays.asList(stream, stream2))
                .using(l -> (Integer) l.get(0) + (Integer) l.get(1))
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .assertCompleted()
                .assertItems(2, 4, 6, 8);
    }

    @Test
    public void testCombiningWithIterableWithFirstShorter() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3, 4);

        Multi.createBy().combining().streams(Arrays.asList(stream, stream2))
                .using(l -> (Integer) l.get(0) + (Integer) l.get(1))
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .assertCompleted()
                .assertItems(2, 4, 6);
    }

    @Test
    public void testCombiningWithIterableWithSecondShorter() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3, 4);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3);

        Multi.createBy().combining().streams(Arrays.asList(stream, stream2))
                .using(l -> (Integer) l.get(0) + (Integer) l.get(1))
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .assertCompleted()
                .assertItems(2, 4, 6);
    }

    @Test
    public void testMultiCombineNoStreams() {
        Multi.createBy().combining().streams(Collections.emptyList()).using(l -> l)
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testMultiCombineWithCancellation() {
        AtomicBoolean cancelled1 = new AtomicBoolean();
        AtomicBoolean cancelled2 = new AtomicBoolean();
        AtomicBoolean cancelled3 = new AtomicBoolean();
        Multi<Integer> stream1 = Multi.createFrom().emitter(e -> {
            e.onTermination(() -> cancelled1.set(true));
            e.emit(1);
        });
        Multi<Integer> stream2 = Multi.createFrom().emitter(e -> {
            e.onTermination(() -> cancelled2.set(true));
            e.emit(2);
        });
        Multi<Integer> stream3 = Multi.createFrom().emitter(e -> {
            e.onTermination(() -> cancelled3.set(true));
        });

        Multi.createBy().combining().streams(stream1, stream2, stream3).asTuple()
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .assertSubscribed()
                .assertNotTerminated()
                .cancel()
                .assertHasNotReceivedAnyItem();

        assertThat(cancelled1).isTrue();
        assertThat(cancelled2).isTrue();
        assertThat(cancelled3).isTrue();
    }

    @Test
    public void testMultiCombineWithImmediateCancellation() {
        AtomicBoolean cancelled1 = new AtomicBoolean();
        AtomicBoolean cancelled2 = new AtomicBoolean();
        AtomicBoolean cancelled3 = new AtomicBoolean();
        AtomicBoolean subscribe1 = new AtomicBoolean();
        AtomicBoolean subscribe2 = new AtomicBoolean();
        AtomicBoolean subscribe3 = new AtomicBoolean();
        Multi<Integer> stream1 = Multi.createFrom().emitter(e -> {
            subscribe1.set(true);
            e.onTermination(() -> cancelled1.set(true));
            e.emit(1);
        });
        Multi<Integer> stream2 = Multi.createFrom().emitter(e -> {
            subscribe2.set(true);
            e.onTermination(() -> cancelled2.set(true));
            e.emit(2);
        });
        Multi<Integer> stream3 = Multi.createFrom().emitter(e -> {
            subscribe3.set(true);
            e.onTermination(() -> cancelled3.set(true));
        });

        Multi.createBy().combining().streams(stream1, stream2, stream3).asTuple()
                .subscribe().withSubscriber(new AssertSubscriber<>(3, true))
                .assertSubscribed()
                .assertNotTerminated()
                .cancel()
                .assertHasNotReceivedAnyItem();

        assertThat(subscribe1).isFalse();
        assertThat(subscribe2).isFalse();
        assertThat(subscribe3).isFalse();
        assertThat(cancelled1).isFalse();
        assertThat(cancelled2).isFalse();
        assertThat(cancelled3).isFalse();
    }

    @Test
    public void testCombiningWithIterableWithEmptyStream() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3, 4);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3);

        Multi.createBy().combining().streams(Arrays.asList(stream, stream2, Multi.createFrom().empty()))
                .using(l -> (Integer) l.get(0) + (Integer) l.get(1))
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCombineLatestWithCombinatorReturningNull() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3, 4);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3);
        Multi.createBy().combining().streams(stream, stream2).latestItems().using((a, b) -> null)
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .assertFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testCombineLatestWithCombinatorThrowingAnException() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3, 4);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3);
        Multi.createBy().combining().streams(stream, stream2).latestItems().using((a, b) -> {
            throw new IllegalStateException("boom");
        })
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testCombineLatestWithFailingStream() {
        Multi<Integer> stream = Multi.createFrom().failure(new IOException("boom"));
        Multi.createBy().combining().streams(stream, Multi.createFrom().nothing()).latestItems().using((a, b) -> null)
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCombineLatest() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3);

        List<Tuple2<Integer, Integer>> list = Multi.createBy().combining().streams(stream, stream).latestItems()
                .asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).containsExactly(Tuple2.of(3, 1), Tuple2.of(3, 2), Tuple2.of(3, 3));

        List<Integer> list2 = Multi.createBy().combining().streams(stream, stream).latestItems().using((a, b) -> a)
                .collect().asList().await().indefinitely();
        assertThat(list2).containsExactly(3, 3, 3);

        list2 = Multi.createBy().combining().streams(stream, stream).latestItems().using((a, b) -> b)
                .collect().asList().await().indefinitely();
        assertThat(list2).containsExactly(1, 2, 3);
    }

    @Test
    public void testCombineLatestWithSinglePublisher() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3);
        List<Integer> list = Multi.createBy().combining().streams(Collections.singletonList(stream))
                .using(l -> (Integer) l.get(0))
                .collect().asList()
                .await().indefinitely();
        assertThat(list).containsExactly(1, 2, 3);
    }

    @Test
    public void testCombineLatestWithEmpty() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3);
        Multi<Integer> empty = Multi.createFrom().empty();

        List<Tuple2<Integer, Integer>> list = Multi.createBy().combining().streams(stream, empty).latestItems()
                .asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).isEmpty();
    }

    @Test
    public void testCombineLatestWith3Streams() {
        Multi<Integer> one = Multi.createFrom().item(1);
        Multi<Integer> two = Multi.createFrom().item(2);
        Multi<Integer> three = Multi.createFrom().items(3, 4);

        List<Tuple3<Integer, Integer, Integer>> list = Multi.createBy().combining().streams(one, two, three)
                .latestItems().asTuple()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple3.of(1, 2, 3), Tuple3.of(1, 2, 4));
    }

    @Test
    public void testCombineLatestWithFourStreams() {
        Multi<Integer> one = Multi.createFrom().item(1);
        Multi<Integer> two = Multi.createFrom().item(2);
        Multi<Integer> three = Multi.createFrom().items(3, 4);
        Multi<Integer> four = Multi.createFrom().items(5);

        List<Tuple4<Integer, Integer, Integer, Integer>> list = Multi.createBy().combining()
                .streams(one, two, three, four).latestItems().asTuple()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple4.of(1, 2, 4, 5));

        list = Multi.createBy().combining().streams(one, two, four, three).latestItems().asTuple()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple4.of(1, 2, 5, 3), Tuple4.of(1, 2, 5, 4));
    }

    @Test
    public void testCombineLatestWithFiveStreams() {
        Multi<Integer> one = Multi.createFrom().item(1);
        Multi<Integer> two = Multi.createFrom().item(2);
        Multi<Integer> three = Multi.createFrom().items(3, 4);
        Multi<Integer> four = Multi.createFrom().items(5);
        Multi<Integer> five = Multi.createFrom().items(6);

        List<Tuple5<Integer, Integer, Integer, Integer, Integer>> list = Multi.createBy().combining()
                .streams(one, two, three, four, five).latestItems().asTuple()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple5.of(1, 2, 4, 5, 6));

        list = Multi.createBy().combining().streams(one, two, four, five, three).latestItems().asTuple()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple5.of(1, 2, 5, 6, 3), Tuple5.of(1, 2, 5, 6, 4));

        list = Multi.createBy().combining().streams(one, two, four, Multi.createFrom().<Integer> empty(), three)
                .latestItems().asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).isEmpty();
    }

    @Test
    public void testCombineLatestWithSixStreams() {
        Multi<Integer> one = Multi.createFrom().item(1);
        Multi<Integer> two = Multi.createFrom().item(2);
        Multi<Integer> three = Multi.createFrom().items(3, 4);
        Multi<Integer> four = Multi.createFrom().items(5);
        Multi<Integer> five = Multi.createFrom().items(6);
        Multi<Integer> six = Multi.createFrom().items(7);

        List<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> list = Multi.createBy().combining()
                .streams(one, two, three, four, five, six).latestItems().asTuple()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple6.of(1, 2, 4, 5, 6, 7));

        list = Multi.createBy().combining().streams(one, two, four, five, six, three).latestItems().asTuple()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple6.of(1, 2, 5, 6, 7, 3), Tuple6.of(1, 2, 5, 6, 7, 4));

        list = Multi.createBy().combining().streams(one, two, four, Multi.createFrom().<Integer> empty(), six, three)
                .latestItems().asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).isEmpty();
    }

    @Test
    public void testCombineLatestWithSevenStreams() {
        Multi<Integer> one = Multi.createFrom().item(1);
        Multi<Integer> two = Multi.createFrom().item(2);
        Multi<Integer> three = Multi.createFrom().items(3, 4);
        Multi<Integer> four = Multi.createFrom().items(5);
        Multi<Integer> five = Multi.createFrom().items(6);
        Multi<Integer> six = Multi.createFrom().items(7);
        Multi<Integer> seven = Multi.createFrom().items(8);

        List<Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>> list = Multi.createBy().combining()
                .streams(one, two, three, four, five, six, seven).latestItems().asTuple()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple7.of(1, 2, 4, 5, 6, 7, 8));

        list = Multi.createBy().combining().streams(one, two, four, five, six, seven, three).latestItems().asTuple()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple7.of(1, 2, 5, 6, 7, 8, 3),
                Tuple7.of(1, 2, 5, 6, 7, 8, 4));

        list = Multi.createBy().combining()
                .streams(one, two, four, Multi.createFrom().<Integer> empty(), six, three, seven)
                .latestItems().asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).isEmpty();
    }

    @Test
    public void testCombineLatestWithEightStreams() {
        Multi<Integer> one = Multi.createFrom().item(1);
        Multi<Integer> two = Multi.createFrom().item(2);
        Multi<Integer> three = Multi.createFrom().items(3, 4);
        Multi<Integer> four = Multi.createFrom().items(5);
        Multi<Integer> five = Multi.createFrom().items(6);
        Multi<Integer> six = Multi.createFrom().items(7);
        Multi<Integer> seven = Multi.createFrom().items(8);
        Multi<Integer> eight = Multi.createFrom().items(9);

        List<Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> list = Multi.createBy()
                .combining()
                .streams(one, two, three, four, five, six, seven, eight).latestItems().asTuple()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple8.of(1, 2, 4, 5, 6, 7, 8, 9));

        list = Multi.createBy().combining().streams(one, two, four, five, six, seven, eight, three).latestItems()
                .asTuple()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple8.of(1, 2, 5, 6, 7, 8, 9, 3),
                Tuple8.of(1, 2, 5, 6, 7, 8, 9, 4));

        list = Multi.createBy().combining()
                .streams(one, two, four, Multi.createFrom().<Integer> empty(), six, three, seven, eight)
                .latestItems().asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).isEmpty();
    }

    @Test
    public void testCombineLatestWithNineStreams() {
        Multi<Integer> one = Multi.createFrom().item(1);
        Multi<Integer> two = Multi.createFrom().item(2);
        Multi<Integer> three = Multi.createFrom().items(3, 4);
        Multi<Integer> four = Multi.createFrom().items(5);
        Multi<Integer> five = Multi.createFrom().items(6);
        Multi<Integer> six = Multi.createFrom().items(7);
        Multi<Integer> seven = Multi.createFrom().items(8);
        Multi<Integer> eight = Multi.createFrom().items(9);
        Multi<Integer> nine = Multi.createFrom().items(10);

        List<Tuple9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> list = Multi
                .createBy()
                .combining()
                .streams(one, two, three, four, five, six, seven, eight, nine).latestItems().asTuple()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple9.of(1, 2, 4, 5, 6, 7, 8, 9, 10));

        list = Multi.createBy().combining().streams(one, two, four, five, six, seven, eight, nine, three).latestItems()
                .asTuple()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple9.of(1, 2, 5, 6, 7, 8, 9, 10, 3),
                Tuple9.of(1, 2, 5, 6, 7, 8, 9, 10, 4));

        list = Multi.createBy().combining()
                .streams(one, two, four, Multi.createFrom().<Integer> empty(), six, three, seven, eight, nine)
                .latestItems().asTuple()
                .collect().asList().await().indefinitely();
        assertThat(list).isEmpty();
    }

    @Test
    public void testCombineLatestWithNoStreams() {
        AssertSubscriber<? extends List<?>> subscriber = Multi.createBy().combining()
                .streams(Collections.emptyList()).latestItems().using(l -> l)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertSubscribed().assertCompleted().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCombineLatestWithOneStreams() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3);
        AssertSubscriber<Integer> subscriber = Multi.createBy().combining()
                .streams(Collections.singletonList(multi)).latestItems().using(l -> l)
                .onItem().<Integer> disjoint()
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertSubscribed().assertCompleted().assertItems(1, 2, 3);
    }

    @Test
    public void testCombineLatestWithCancellation() {
        AtomicBoolean cancelled1 = new AtomicBoolean();
        AtomicBoolean cancelled2 = new AtomicBoolean();
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onCancellation().invoke(() -> cancelled1.set(true));
        Multi<Integer> none = Multi.createFrom().<Integer> nothing()
                .onCancellation().invoke(() -> cancelled2.set(true));

        AssertSubscriber<Integer> subscriber = Multi.createBy().combining()
                .streams(multi, none).latestItems().using(l -> l)
                .onItem().<Integer> disjoint()
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertSubscribed();
        subscriber.cancel();
        subscriber.assertHasNotReceivedAnyItem().assertNotTerminated();
        assertThat(cancelled1).isFalse(); // Already completed.
        assertThat(cancelled2).isTrue();
    }

    @Test
    public void testCombineLatestWithFailingStreamAndFailureCollection() {
        Multi<Integer> stream = Multi.createFrom().failure(new IOException("boomA"));
        Multi<Integer> stream2 = Multi.createFrom().failure(new IOException("boomB"));

        Multi.createBy().combining().streams(stream, Multi.createFrom().nothing(), stream2)
                .latestItems().collectFailures().using((a, b, c) -> null)
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .assertFailedWith(IOException.class, "boomA");
    }

    @Test
    public void testCombineLatestWithFailureCollectionAndNoFailure() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3);
        Multi<Integer> stream2 = Multi.createFrom().items(4, 5, 6);

        Multi.createBy().combining().streams(stream, stream2)
                .latestItems().collectFailures().asTuple()
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .assertCompleted()
                .assertItems(Tuple2.of(3, 4), Tuple2.of(3, 5), Tuple2.of(3, 6));
    }

}
