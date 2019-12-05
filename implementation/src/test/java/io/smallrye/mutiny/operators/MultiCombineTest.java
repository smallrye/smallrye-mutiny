package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;
import io.smallrye.mutiny.tuples.*;

public class MultiCombineTest {

    @Test
    public void combineIterableOfStreamsFollowedByAFlatMap() {
        Multi<Integer> multi1 = Multi.createFrom().item(1);
        Multi<Integer> multi2 = Multi.createFrom().item(2);
        Multi<Integer> multi3 = Multi.createFrom().item(3);
        Multi<Integer> multi4 = Multi.createFrom().item(4);

        Multi<Integer> combined = Multi.createBy().combining().streams(Arrays.asList(multi1, multi2, multi3, multi4))
                .using(l -> l)
                .onItem().flatMap(l -> Multi.createFrom().iterable(l))
                .onItem().castTo(Integer.class);

        combined.subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testCombinationOfTwoStreamsAsTuple() {
        List<Tuple2<Integer, Integer>> list = Multi.createBy()
                .combining().streams(Multi.createFrom().range(1, 4), Multi.createFrom().range(2, 5)).asTuple()
                .collectItems().asList().await().indefinitely();
        assertThat(list).hasSize(3).containsExactly(Tuple2.of(1, 2), Tuple2.of(2, 3), Tuple2.of(3, 4));
    }

    @Test
    public void testCombinationOfAStreamWithItself() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3, 4);

        List<Tuple2<Integer, Integer>> list = Multi.createBy()
                .combining().streams(stream, stream).asTuple()
                .collectItems().asList().await().indefinitely();
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
                .collectItems().asList().await().indefinitely();
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
                .collectItems().asList().await().indefinitely();
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
                .collectItems().asList().await().indefinitely();
        assertThat(list).hasSize(3)
                .containsExactly(Tuple5.of(1, 2, 3, 4, 5),
                        Tuple5.of(2, 3, 4, 5, 6),
                        Tuple5.of(3, 4, 5, 6, 7));
    }

    @Test
    public void testCombinationWithBackPressure() {
        Multi<Integer> stream = Multi.createFrom().range(1, 5);
        MultiAssertSubscriber<Integer> subscriber = Multi.createBy().combining().streams(stream, stream)
                .using((a, b) -> a + b)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(0));

        subscriber.assertNotTerminated().assertHasNotReceivedAnyItem();

        subscriber.request(2)
                .assertReceived(2, 4);

        subscriber.request(3)
                .assertReceived(2, 4, 6, 8)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testCombinationOfAStreamsOfDifferentSize() {
        Multi<Integer> stream1 = Multi.createFrom().items(1, 2, 3, 4);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7);

        List<Tuple2<Integer, Integer>> list = Multi.createBy()
                .combining().streams(stream1, stream2).asTuple()
                .collectItems().asList().await().indefinitely();
        assertThat(list).hasSize(4).containsExactly(Tuple2.of(1, 1), Tuple2.of(2, 2),
                Tuple2.of(3, 3), Tuple2.of(4, 4));

        list = Multi.createBy()
                .combining().streams(stream2, stream1).asTuple()
                .collectItems().asList().await().indefinitely();
        assertThat(list).hasSize(4).containsExactly(Tuple2.of(1, 1), Tuple2.of(2, 2),
                Tuple2.of(3, 3), Tuple2.of(4, 4));
    }

    @Test
    public void testCombinationOfAStreamsOfDifferentSizeUsingLatest() {
        Multi<Integer> stream1 = Multi.createFrom().items(1, 2, 3, 4);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7);

        List<Tuple2<Integer, Integer>> list = Multi.createBy()
                .combining().streams(stream1, stream2).latestItems().asTuple()
                .collectItems().asList().await().indefinitely();
        assertThat(list).hasSize(7).containsExactly(Tuple2.of(4, 1), Tuple2.of(4, 2),
                Tuple2.of(4, 3), Tuple2.of(4, 4), Tuple2.of(4, 5), Tuple2.of(4, 6), Tuple2.of(4, 7));

        list = Multi.createBy()
                .combining().streams(stream2, stream1).latestItems().asTuple()
                .collectItems().asList().await().indefinitely();
        assertThat(list).hasSize(4).containsExactly(Tuple2.of(7, 1), Tuple2.of(7, 2),
                Tuple2.of(7, 3), Tuple2.of(7, 4));
    }

    @Test
    public void testCombinationWithEmpty() {
        Multi.createBy().combining()
                .streams(Multi.createFrom().<Integer> empty(), Multi.createFrom().range(1, 2_000_000))
                .using((a, b) -> a + b)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();

        Multi.createBy().combining()
                .streams(Multi.createFrom().range(1, 2_000_000), Multi.createFrom().<Integer> empty())
                .using((a, b) -> a + b)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCombiningASingleStreamUsingIterable() {
        Multi.createBy().combining().streams(Collections.singletonList(Multi.createFrom().item(1))).using(l -> l.get(0))
                .onItem().castTo(Integer.class)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertCompletedSuccessfully()
                .assertReceived(1);
    }

    @Test
    public void testCombiningASingleEmptyStreamUsingIterable() {
        Multi.createBy().combining().streams(Collections.singletonList(Multi.createFrom().empty())).using(l -> l.get(0))
                .onItem().castTo(Integer.class)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatIterableCannotBeNull() {
        Multi.createBy().combining().streams(null);
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
                .subscribe().withSubscriber(new MultiAssertSubscriber<>(2))
                .assertHasFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testThatCombinatorCanThrowAnException() {
        Multi<Integer> multi = Multi.createFrom().item(1);

        Multi.createBy().combining().streams(multi, multi).using((i, j) -> {
            throw new IllegalArgumentException("boom");
        })
                .subscribe().withSubscriber(new MultiAssertSubscriber<>(2))
                .assertHasFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testCombiningWithFailures() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3);
        Multi<Integer> multi2 = Multi.createFrom().emitter(e -> e.emit(1).fail(new IOException("boom")));

        Multi.createBy().combining().streams(multi, multi2).asTuple()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(3))
                .assertHasFailedWith(IOException.class, "boom")
                .assertReceived(Tuple2.of(1, 1));
    }

    @Test
    public void testCombiningWithFailuresAndCollectFailure() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3);
        Multi<Integer> multi2 = Multi.createFrom().emitter(e -> e.emit(1).fail(new IOException("boom")));
        Multi<Integer> multi3 = Multi.createFrom().emitter(e -> e.emit(1).emit(2).fail(new IOException("boom")));

        Multi.createBy().combining().streams(multi, multi2, multi, multi3).collectFailures().asTuple()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(3))
                .assertHasFailedWith(IOException.class, "boom")
                .assertReceived(Tuple4.of(1, 1, 1, 1));
    }

    @Test(expectedExceptions = CompletionException.class)
    public void testCombinationOfAFailingStream() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3, 4);
        Multi<Integer> fail = Multi.createFrom().failure(new IOException("boom"));

        Multi.createBy()
                .combining().streams(stream, fail).asTuple()
                .collectItems().asList().await().indefinitely();
    }

    @Test
    public void testCombiningWithIterableWithSameSize() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3, 4);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3, 4);

        Multi.createBy().combining().streams(Arrays.asList(stream, stream2))
                .using(l -> (Integer) l.get(0) + (Integer) l.get(1))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(4))
                .assertCompletedSuccessfully()
                .assertReceived(2, 4, 6, 8);
    }

    @Test
    public void testCombiningWithIterableWithFirstShorter() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3, 4);

        Multi.createBy().combining().streams(Arrays.asList(stream, stream2))
                .using(l -> (Integer) l.get(0) + (Integer) l.get(1))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(4))
                .assertCompletedSuccessfully()
                .assertReceived(2, 4, 6);
    }

    @Test
    public void testCombiningWithIterableWithSecondShorter() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3, 4);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3);

        Multi.createBy().combining().streams(Arrays.asList(stream, stream2))
                .using(l -> (Integer) l.get(0) + (Integer) l.get(1))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(4))
                .assertCompletedSuccessfully()
                .assertReceived(2, 4, 6);
    }

    @Test
    public void testCombiningWithIterableWithEmptyStream() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3, 4);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3);

        Multi.createBy().combining().streams(Arrays.asList(stream, stream2, Multi.createFrom().empty()))
                .using(l -> (Integer) l.get(0) + (Integer) l.get(1))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(4))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCombineLatestWithCombinatorReturningNull() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3, 4);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3);
        Multi.createBy().combining().streams(stream, stream2).latestItems().using((a, b) -> null)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(4))
                .assertHasFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testCombineLatestWithCombinatorThrowingAnException() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3, 4);
        Multi<Integer> stream2 = Multi.createFrom().items(1, 2, 3);
        Multi.createBy().combining().streams(stream, stream2).latestItems().using((a, b) -> {
            throw new IllegalStateException("boom");
        })
                .subscribe().withSubscriber(MultiAssertSubscriber.create(4))
                .assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testCombineLatestWithFailingStream() {
        Multi<Integer> stream = Multi.createFrom().failure(new IOException("boom"));
        Multi.createBy().combining().streams(stream, Multi.createFrom().nothing()).latestItems().using((a, b) -> null)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(4))
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCombineLatest() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3);

        List<Tuple2<Integer, Integer>> list = Multi.createBy().combining().streams(stream, stream).latestItems()
                .asTuple()
                .collectItems().asList().await().indefinitely();
        assertThat(list).containsExactly(Tuple2.of(3, 1), Tuple2.of(3, 2), Tuple2.of(3, 3));

        List<Integer> list2 = Multi.createBy().combining().streams(stream, stream).latestItems().using((a, b) -> a)
                .collectItems().asList().await().indefinitely();
        assertThat(list2).containsExactly(3, 3, 3);

        list2 = Multi.createBy().combining().streams(stream, stream).latestItems().using((a, b) -> b)
                .collectItems().asList().await().indefinitely();
        assertThat(list2).containsExactly(1, 2, 3);
    }

    @Test
    public void testCombineLatestWithSinglePublisher() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3);
        List<Integer> list = Multi.createBy().combining().streams(Collections.singletonList(stream))
                .using(l -> (Integer) l.get(0))
                .collectItems().asList()
                .await().indefinitely();
        assertThat(list).containsExactly(1, 2, 3);
    }

    @Test
    public void testCombineLatestWithEmpty() {
        Multi<Integer> stream = Multi.createFrom().items(1, 2, 3);
        Multi<Integer> empty = Multi.createFrom().empty();

        List<Tuple2<Integer, Integer>> list = Multi.createBy().combining().streams(stream, empty).latestItems()
                .asTuple()
                .collectItems().asList().await().indefinitely();
        assertThat(list).isEmpty();
    }

    @Test
    public void testCombineLatestWith3Streams() {
        Multi<Integer> one = Multi.createFrom().item(1);
        Multi<Integer> two = Multi.createFrom().item(2);
        Multi<Integer> three = Multi.createFrom().items(3, 4);

        List<Tuple3<Integer, Integer, Integer>> list = Multi.createBy().combining().streams(one, two, three)
                .latestItems().asTuple()
                .collectItems().asList().await().indefinitely();

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
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple4.of(1, 2, 4, 5));

        list = Multi.createBy().combining().streams(one, two, four, three).latestItems().asTuple()
                .collectItems().asList().await().indefinitely();

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
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple5.of(1, 2, 4, 5, 6));

        list = Multi.createBy().combining().streams(one, two, four, five, three).latestItems().asTuple()
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactly(Tuple5.of(1, 2, 5, 6, 3), Tuple5.of(1, 2, 5, 6, 4));

        list = Multi.createBy().combining().streams(one, two, four, Multi.createFrom().<Integer> empty(), three)
                .latestItems().asTuple()
                .collectItems().asList().await().indefinitely();
        assertThat(list).isEmpty();
    }

}
