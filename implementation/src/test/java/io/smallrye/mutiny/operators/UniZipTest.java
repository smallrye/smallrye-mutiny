package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.*;

public class UniZipTest {

    @Test
    public void testWithTwoSimpleUnis() {

        Uni<Integer> uni = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);

        UniAssertSubscriber<Tuple2<Integer, Integer>> subscriber = Uni.combine().all().unis(uni, uni2).asTuple()
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        assertThat(subscriber.getItem().asList()).containsExactly(1, 2);
    }

    @Test
    public void testWithTwoOneFailure() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        UniAssertSubscriber<Tuple2<Integer, Integer>> subscriber = Uni.combine().all()
                .unis(uni, Uni.createFrom().<Integer> failure(new IOException("boom"))).asTuple()
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailure(IOException.class, "boom");
    }

    @Test(expectedExceptions = TimeoutException.class)
    public void testWithNever() {
        Uni<Tuple3<Integer, Integer, Object>> tuple = Uni.combine().all().unis(Uni.createFrom().item(1),
                Uni.createFrom().item(2), Uni.createFrom().nothing()).asTuple();
        tuple.await().atMost(Duration.ofMillis(1000));
    }

    @Test
    public void testWithTwoFailures() {
        UniAssertSubscriber<Tuple3<Integer, Integer, Integer>> subscriber = Uni.combine().all().unis(
                Uni.createFrom().item(1),
                Uni.createFrom().<Integer> failure(new IOException("boom")),
                Uni.createFrom().<Integer> failure(new IOException("boom 2")))
                .collectFailures()
                .asTuple()
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompletedWithFailure()
                .assertFailure(CompositeException.class, "boom")
                .assertFailure(CompositeException.class, "boom 2");
    }

    @Test
    public void testWithCombinator() {
        Uni<Integer> uni1 = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        UniAssertSubscriber<Integer> subscriber = Uni.combine().all().unis(uni1, uni2, uni3)
                .combinedWith((i1, i2, i3) -> i1 + i2 + i3)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.await().assertItem(6);
    }

    @Test
    public void testTerminationJoin() {
        Uni<Void> uni = Uni.combine().all().unis(Uni.createFrom().item(1),
                Uni.createFrom().item("hello")).asTuple().onItem().ignore().andContinueWithNull();

        uni.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompletedSuccessfully()
                .assertItem(null);
    }

    @Test
    public void testWithFiveUnis() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        Uni<Integer> uni4 = Uni.createFrom().item(4);

        UniAssertSubscriber<Tuple5<Integer, Integer, Integer, Integer, Integer>> subscriber = Uni.combine().all()
                .unis(uni, uni, uni2, uni3, uni4).asTuple().subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        assertThat(subscriber.getItem().asList()).containsExactly(1, 1, 2, 3, 4);
    }

    @Test
    public void testWithSixUnis() {
        Uni<Integer> uni1 = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        Uni<Integer> uni4 = Uni.createFrom().item(4);
        Uni<Integer> uni5 = Uni.createFrom().item(5);
        Uni<Integer> uni6 = Uni.createFrom().item(6);

        UniAssertSubscriber<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> subscriber = Uni.combine()
                .all()
                .unis(uni1, uni2, uni3, uni4, uni5, uni6).asTuple()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompletedSuccessfully();

        assertThat(subscriber.getItem().asList()).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    public void testWithFourUnis() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);

        UniAssertSubscriber<Tuple4<Integer, Integer, Integer, Integer>> subscriber = Uni.combine().all()
                .unis(uni, uni, uni2, uni3).asTuple().subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        assertThat(subscriber.getItem().asList()).containsExactly(1, 1, 2, 3);
    }

    @Test
    public void testWithSevenUnis() {
        Uni<Integer> uni1 = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        Uni<Integer> uni4 = Uni.createFrom().item(4);
        Uni<Integer> uni5 = Uni.createFrom().item(5);
        Uni<Integer> uni6 = Uni.createFrom().item(6);
        Uni<Integer> uni7 = Uni.createFrom().item(7);

        UniAssertSubscriber<Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>> subscriber = Uni
                .combine()
                .all()
                .unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7).asTuple()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompletedSuccessfully();

        assertThat(subscriber.getItem().asList()).containsExactly(1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    public void testWithEightUnis() {
        Uni<Integer> uni1 = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        Uni<Integer> uni4 = Uni.createFrom().item(4);
        Uni<Integer> uni5 = Uni.createFrom().item(5);
        Uni<Integer> uni6 = Uni.createFrom().item(6);
        Uni<Integer> uni7 = Uni.createFrom().item(7);
        Uni<Integer> uni8 = Uni.createFrom().item(8);

        UniAssertSubscriber<Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> subscriber = Uni
                .combine().all()
                .unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7, uni8).asTuple()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompletedSuccessfully();

        assertThat(subscriber.getItem().asList()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    public void testWithNineUnis() {
        Uni<Integer> uni1 = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        Uni<Integer> uni4 = Uni.createFrom().item(4);
        Uni<Integer> uni5 = Uni.createFrom().item(5);
        Uni<Integer> uni6 = Uni.createFrom().item(6);
        Uni<Integer> uni7 = Uni.createFrom().item(7);
        Uni<Integer> uni8 = Uni.createFrom().item(8);
        Uni<Integer> uni9 = Uni.createFrom().item(9);

        UniAssertSubscriber<Tuple9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> subscriber = Uni
                .combine().all()
                .unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7, uni8, uni9).asTuple()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompletedSuccessfully();

        assertThat(subscriber.getItem().asList()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testUniCombine() {
        Uni<String> uni1 = Uni.createFrom().item("hello");
        Uni<String> uni2 = Uni.createFrom().item("world");
        Uni<String> uni3 = Uni.createFrom().item("!");

        String r = Uni.combine().all().unis(uni1, uni2, uni3)
                .combinedWith((s1, s2, s3) -> s1 + " " + s2 + " " + s3).await().indefinitely();
        assertThat(r).isEqualTo("hello world !");

        List<Uni<String>> list = Arrays.asList(uni1, uni2, uni3);
        r = Uni.combine().all().unis(list).combinedWith(l -> l.get(0) + " " + l.get(1) + " " + l.get(2)).await()
                .indefinitely();
        assertThat(r).isEqualTo("hello world !");
    }

    @Test
    public void testDiscardingItems() {
        Uni<String> uni1 = Uni.createFrom().item("hello");
        Uni<String> uni2 = Uni.createFrom().item("world");
        Uni<String> uni3 = Uni.createFrom().item("!");
        Uni<Integer> uni4 = Uni.createFrom().item(2);

        Void res = Uni.combine().all().unis(uni1, uni2, uni3, uni4).discardItems()
                .await().indefinitely();

        assertThat(res).isNull();

        Uni<Integer> failed = Uni.createFrom().failure(new IOException("boom"));

        assertThatThrownBy(
                () -> Uni.combine().all().unis(uni1, uni2, uni3, failed, uni4).discardItems().await().indefinitely())
                        .isInstanceOf(CompletionException.class)
                        .hasCauseInstanceOf(IOException.class)
                        .hasMessageContaining("boom");

        Uni<Integer> failed2 = Uni.createFrom().failure(new IllegalStateException("d'oh"));

        assertThatThrownBy(
                () -> Uni.combine().all().unis(uni1, uni2, uni3, failed, uni4, failed2).collectFailures().discardItems()
                        .await().indefinitely())
                                .isInstanceOf(CompositeException.class)
                                .hasMessageContaining("boom").hasMessageContaining("d'oh");
    }

    @Test
    public void testWithArraysAndImmediateItems() {
        // We need 10 unis to avoid being handled as tuples
        Uni<Integer> uni1 = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        Uni<Integer> uni4 = Uni.createFrom().item(4);
        Uni<Integer> uni5 = Uni.createFrom().item(5);
        Uni<Integer> uni6 = Uni.createFrom().item(6);
        Uni<Integer> uni7 = Uni.createFrom().item(7);
        Uni<Integer> uni8 = Uni.createFrom().item(8);
        Uni<Integer> uni9 = Uni.createFrom().item(9);
        Uni<Integer> uni10 = Uni.createFrom().item(10);

        int sum = Uni.combine().all().unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7, uni8, uni9, uni10)
                .combinedWith(l -> l.stream().mapToInt(o -> (Integer) o).sum())
                .await().indefinitely();

        assertThat(sum).isEqualTo(1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10);

    }

    @Test
    public void testWithArraysAndOneFailure() {
        // We need 10 unis to avoid being handled as tuples
        Uni<Integer> uni1 = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        Uni<Integer> uni4 = Uni.createFrom().item(4);
        Uni<Integer> uni5 = Uni.createFrom().item(5);
        Uni<Integer> uni6 = Uni.createFrom().failure(new ArithmeticException("boom"));
        Uni<Integer> uni7 = Uni.createFrom().item(7);
        Uni<Integer> uni8 = Uni.createFrom().item(8);
        Uni<Integer> uni9 = Uni.createFrom().item(9);
        Uni<Integer> uni10 = Uni.createFrom().item(10);

        assertThatThrownBy(() -> Uni.combine().all().unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7, uni8, uni9, uni10)
                .combinedWith(l -> l.stream().mapToInt(o -> (Integer) o).sum())
                .await().indefinitely()).isInstanceOf(ArithmeticException.class).hasMessageContaining("boom");

    }

    @Test
    public void testWithArraysAndMultipleFailures() {
        // We need 10 unis to avoid being handled as tuples
        Uni<Integer> uni1 = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        Uni<Integer> uni4 = Uni.createFrom().failure(new UncheckedIOException(new IOException("io")));
        Uni<Integer> uni5 = Uni.createFrom().item(5);
        Uni<Integer> uni6 = Uni.createFrom().failure(new ArithmeticException("boom"));
        Uni<Integer> uni7 = Uni.createFrom().item(7);
        Uni<Integer> uni8 = Uni.createFrom().item(8);
        Uni<Integer> uni9 = Uni.createFrom().item(9);
        Uni<Integer> uni10 = Uni.createFrom().failure(new IllegalStateException("state"));

        assertThatThrownBy(() -> Uni.combine().all().unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7, uni8, uni9, uni10)
                .combinedWith(l -> l.stream().mapToInt(o -> (Integer) o).sum())
                .await().indefinitely()).isInstanceOf(UncheckedIOException.class).hasMessageContaining("io");

    }

    @Test
    public void testWithArraysAndMultipleFailuresAndFailureCollection() {
        // We need 10 unis to avoid being handled as tuples
        Uni<Integer> uni1 = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        Uni<Integer> uni4 = Uni.createFrom().failure(new UncheckedIOException(new IOException("io")));
        Uni<Integer> uni5 = Uni.createFrom().item(5);
        Uni<Integer> uni6 = Uni.createFrom().failure(new ArithmeticException("boom"));
        Uni<Integer> uni7 = Uni.createFrom().item(7);
        Uni<Integer> uni8 = Uni.createFrom().item(8);
        Uni<Integer> uni9 = Uni.createFrom().item(9);
        Uni<Integer> uni10 = Uni.createFrom().failure(new IllegalStateException("state"));

        assertThatThrownBy(() -> Uni.combine().all().unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7, uni8, uni9, uni10)
                .collectFailures()
                .combinedWith(l -> l.stream().mapToInt(o -> (Integer) o).sum())
                .await().indefinitely()).isInstanceOfSatisfying(CompositeException.class, t -> {
                    assertThat(t.getCauses()).hasSize(3);
                    assertThat(t.getSuppressed()).hasSize(2);
                    assertThat(t.getCause()).isInstanceOf(UncheckedIOException.class);
                    assertThat(t.getSuppressed()[0]).isInstanceOf(ArithmeticException.class);
                    assertThat(t.getSuppressed()[1]).isInstanceOf(IllegalStateException.class);
                });

    }

    @Test
    public void testWithArraysWithOnlyFailuresAndFailureCollection() {
        // We need 10 unis to avoid being handled as tuples
        Uni<Integer> uni1 = Uni.createFrom().failure(new IOException("1"));
        Uni<Integer> uni2 = Uni.createFrom().failure(new IOException("2"));
        Uni<Integer> uni3 = Uni.createFrom().failure(new IOException("3"));
        Uni<Integer> uni4 = Uni.createFrom().failure(new IOException("4"));
        Uni<Integer> uni5 = Uni.createFrom().failure(new IOException("5"));
        Uni<Integer> uni6 = Uni.createFrom().failure(new IOException("6"));
        Uni<Integer> uni7 = Uni.createFrom().failure(new IOException("7"));
        Uni<Integer> uni8 = Uni.createFrom().failure(new IOException("8"));
        Uni<Integer> uni9 = Uni.createFrom().failure(new IOException("9"));
        Uni<Integer> uni10 = Uni.createFrom().failure(new IOException("10"));

        assertThatThrownBy(() -> Uni.combine().all().unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7, uni8, uni9, uni10)
                .collectFailures()
                .combinedWith(l -> l.stream().mapToInt(o -> (Integer) o).sum())
                .await().indefinitely()).isInstanceOfSatisfying(CompositeException.class, t -> {
                    assertThat(t.getCauses()).hasSize(10);
                    assertThat(t.getSuppressed()).hasSize(9);
                });

    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testWithArraysWithNoResultAndCancellation() {
        int count = 10;
        AtomicBoolean[] subscriptions = new AtomicBoolean[count];
        AtomicBoolean[] cancellations = new AtomicBoolean[count];
        for (int i = 0; i < count; i++) {
            subscriptions[i] = new AtomicBoolean();
            cancellations[i] = new AtomicBoolean();
        }

        // We need 10 unis to avoid being handled as tuples
        Uni<Integer> uni1 = Uni.createFrom().item(1)
                .onSubscribe().invoke(s -> subscriptions[0].set(true))
                .onCancellation().invoke(() -> cancellations[0].set(true));
        Uni<Integer> uni2 = Uni.createFrom().item(2)
                .onSubscribe().invoke(s -> subscriptions[1].set(true))
                .onCancellation().invoke(() -> cancellations[1].set(true));
        Uni<Integer> uni3 = Uni.createFrom().item(3)
                .onSubscribe().invoke(s -> subscriptions[2].set(true))
                .onCancellation().invoke(() -> cancellations[2].set(true));
        Uni<Integer> uni4 = Uni.createFrom().item(4)
                .onSubscribe().invoke(s -> subscriptions[3].set(true))
                .onCancellation().invoke(() -> cancellations[3].set(true));
        Uni<Integer> uni5 = Uni.createFrom().item(5)
                .onSubscribe().invoke(s -> subscriptions[4].set(true))
                .onCancellation().invoke(() -> cancellations[4].set(true));
        Uni<Integer> uni6 = Uni.createFrom().item(6)
                .onSubscribe().invoke(s -> subscriptions[5].set(true))
                .onCancellation().invoke(() -> cancellations[5].set(true));
        Uni<Integer> uni7 = Uni.createFrom().<Integer> emitter(e -> {
            // Do not emit
        })
                .onSubscribe().invoke(s -> subscriptions[6].set(true))
                .onCancellation().invoke(() -> cancellations[6].set(true));
        Uni<Integer> uni8 = Uni.createFrom().item(() -> 8)
                .onSubscribe().invoke(s -> subscriptions[7].set(true))
                .onCancellation().invoke(() -> cancellations[7].set(true));
        Uni<Integer> uni9 = Uni.createFrom().item(() -> 9)
                .onSubscribe().invoke(s -> subscriptions[8].set(true))
                .onCancellation().invoke(() -> cancellations[8].set(true));
        Uni<Integer> uni10 = Uni.createFrom().item(() -> 10)
                .onSubscribe().invoke(s -> subscriptions[9].set(true))
                .onCancellation().invoke(() -> cancellations[9].set(true));

        Uni<Integer> all = Uni.combine().all().unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7, uni8, uni9, uni10)
                .combinedWith(l -> l.stream().mapToInt(o -> (Integer) o).sum());

        assertThat(subscriptions).allSatisfy(bool -> assertThat(bool).isFalse());
        assertThat(cancellations).allSatisfy(bool -> assertThat(bool).isFalse());
        UniAssertSubscriber<Integer> subscriber = all.subscribe().withSubscriber(new UniAssertSubscriber<>());

        subscriber.assertSubscribed()
                .assertNotCompleted()
                .assertNoFailure();

        assertThat(subscriptions).allSatisfy(bool -> assertThat(bool).isTrue());
        assertThat(cancellations).allSatisfy(bool -> assertThat(bool).isFalse());
        subscriber.cancel();
        assertThat(subscriptions).allSatisfy(bool -> assertThat(bool).isTrue());
    }

}
