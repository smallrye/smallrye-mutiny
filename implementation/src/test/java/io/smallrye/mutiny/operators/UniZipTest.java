package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
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
    public void testWithTwoSimpleUnisCombine() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);

        UniAssertSubscriber<Integer> subscriber = Uni.combine().all().unis(uni, uni2)
                .collectFailures()
                .combinedWith(Integer::sum)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(3);
    }

    @Test
    public void testWithTwoOneFailure() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        UniAssertSubscriber<Tuple2<Integer, Integer>> subscriber = Uni.combine().all()
                .unis(uni, Uni.createFrom().<Integer> failure(new IOException("boom"))).asTuple()
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testWithNever() {
        assertThrows(TimeoutException.class, () -> {
            Uni<Tuple3<Integer, Integer, Object>> tuple = Uni.combine().all().unis(Uni.createFrom().item(1),
                    Uni.createFrom().item(2), Uni.createFrom().nothing()).asTuple();
            tuple.await().atMost(Duration.ofMillis(100));
        });
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
        subscriber.assertFailed()
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "boom 2");
    }

    @Test
    public void testWithCombinator() {
        Uni<Integer> uni1 = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        UniAssertSubscriber<Integer> subscriber = Uni.combine().all().unis(uni1, uni2, uni3)
                .combinedWith((i1, i2, i3) -> i1 + i2 + i3)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitItem().assertItem(6);
    }

    @Test
    public void testTerminationJoin() {
        Uni<Void> uni = Uni.combine().all().unis(Uni.createFrom().item(1),
                Uni.createFrom().item("hello")).asTuple().onItem().ignore().andContinueWithNull();

        uni.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
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

        subscriber.assertCompleted();

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

        subscriber.assertCompleted();

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

        subscriber.assertCompleted();

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

        subscriber.assertCompleted();

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
                .onSubscription().invoke(s -> subscriptions[0].set(true))
                .onCancellation().invoke(() -> cancellations[0].set(true));
        Uni<Integer> uni2 = Uni.createFrom().item(2)
                .onSubscription().invoke(s -> subscriptions[1].set(true))
                .onCancellation().invoke(() -> cancellations[1].set(true));
        Uni<Integer> uni3 = Uni.createFrom().item(3)
                .onSubscription().invoke(s -> subscriptions[2].set(true))
                .onCancellation().invoke(() -> cancellations[2].set(true));
        Uni<Integer> uni4 = Uni.createFrom().item(4)
                .onSubscription().invoke(s -> subscriptions[3].set(true))
                .onCancellation().invoke(() -> cancellations[3].set(true));
        Uni<Integer> uni5 = Uni.createFrom().item(5)
                .onSubscription().invoke(s -> subscriptions[4].set(true))
                .onCancellation().invoke(() -> cancellations[4].set(true));
        Uni<Integer> uni6 = Uni.createFrom().item(6)
                .onSubscription().invoke(s -> subscriptions[5].set(true))
                .onCancellation().invoke(() -> cancellations[5].set(true));
        Uni<Integer> uni7 = Uni.createFrom().<Integer> emitter(e -> {
            // Do not emit
        })
                .onSubscription().invoke(s -> subscriptions[6].set(true))
                .onCancellation().invoke(() -> cancellations[6].set(true));
        Uni<Integer> uni8 = Uni.createFrom().item(() -> 8)
                .onSubscription().invoke(s -> subscriptions[7].set(true))
                .onCancellation().invoke(() -> cancellations[7].set(true));
        Uni<Integer> uni9 = Uni.createFrom().item(() -> 9)
                .onSubscription().invoke(s -> subscriptions[8].set(true))
                .onCancellation().invoke(() -> cancellations[8].set(true));
        Uni<Integer> uni10 = Uni.createFrom().item(() -> 10)
                .onSubscription().invoke(s -> subscriptions[9].set(true))
                .onCancellation().invoke(() -> cancellations[9].set(true));

        Uni<Integer> all = Uni.combine().all().unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7, uni8, uni9, uni10)
                .combinedWith(l -> l.stream().mapToInt(o -> (Integer) o).sum());

        assertThat(subscriptions).allSatisfy(bool -> assertThat(bool).isFalse());
        assertThat(cancellations).allSatisfy(bool -> assertThat(bool).isFalse());
        UniAssertSubscriber<Integer> subscriber = all.subscribe().withSubscriber(new UniAssertSubscriber<>());

        subscriber.assertSubscribed()
                .assertNotTerminated();

        assertThat(subscriptions).allSatisfy(bool -> assertThat(bool).isTrue());
        assertThat(cancellations).allSatisfy(bool -> assertThat(bool).isFalse());
        subscriber.cancel();
        assertThat(subscriptions).allSatisfy(bool -> assertThat(bool).isTrue());
    }

    @Test
    public void testCombineWith3() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);

        UniAssertSubscriber<Integer> subscriber = Uni.combine().all().unis(uni, uni2, uni3)
                .collectFailures()
                .combinedWith((a, b, c) -> a + b + c)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(6);
    }

    @Test
    public void testCombineWith4() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        Uni<Integer> uni4 = Uni.createFrom().item(4);

        UniAssertSubscriber<Integer> subscriber = Uni.combine().all().unis(uni, uni2, uni3, uni4)
                .collectFailures()
                .combinedWith((a, b, c, d) -> a + b + c + d)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(10);
    }

    @Test
    public void testCombineWith5() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        Uni<Integer> uni4 = Uni.createFrom().item(4);
        Uni<Integer> uni5 = Uni.createFrom().item(5);

        UniAssertSubscriber<Integer> subscriber = Uni.combine().all().unis(uni, uni2, uni3, uni4, uni5)
                .collectFailures()
                .combinedWith((a, b, c, d, e) -> a + b + c + d + e)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(15);
    }

    @Test
    public void testCombineWith6() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        Uni<Integer> uni4 = Uni.createFrom().item(4);
        Uni<Integer> uni5 = Uni.createFrom().item(5);
        Uni<Integer> uni6 = Uni.createFrom().item(6);

        UniAssertSubscriber<Integer> subscriber = Uni.combine().all().unis(uni, uni2, uni3, uni4, uni5, uni6)
                .collectFailures()
                .combinedWith((a, b, c, d, e, f) -> a + b + c + d + e + f)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(21);
    }

    @Test
    public void testCombineWith7() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        Uni<Integer> uni4 = Uni.createFrom().item(4);
        Uni<Integer> uni5 = Uni.createFrom().item(5);
        Uni<Integer> uni6 = Uni.createFrom().item(6);
        Uni<Integer> uni7 = Uni.createFrom().item(7);

        UniAssertSubscriber<Integer> subscriber = Uni.combine().all().unis(uni, uni2, uni3, uni4, uni5, uni6, uni7)
                .collectFailures()
                .combinedWith((a, b, c, d, e, f, g) -> a + b + c + d + e + f + g)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(28);
    }

    @Test
    public void testCombineWith8() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        Uni<Integer> uni4 = Uni.createFrom().item(4);
        Uni<Integer> uni5 = Uni.createFrom().item(5);
        Uni<Integer> uni6 = Uni.createFrom().item(6);
        Uni<Integer> uni7 = Uni.createFrom().item(7);
        Uni<Integer> uni8 = Uni.createFrom().item(8);

        UniAssertSubscriber<Integer> subscriber = Uni.combine().all().unis(uni, uni2, uni3, uni4, uni5, uni6, uni7, uni8)
                .collectFailures()
                .combinedWith((a, b, c, d, e, f, g, h) -> a + b + c + d + e + f + g + h)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(36);
    }

    @Test
    public void testCombineWith9() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        Uni<Integer> uni4 = Uni.createFrom().item(4);
        Uni<Integer> uni5 = Uni.createFrom().item(5);
        Uni<Integer> uni6 = Uni.createFrom().item(6);
        Uni<Integer> uni7 = Uni.createFrom().item(7);
        Uni<Integer> uni8 = Uni.createFrom().item(8);
        Uni<Integer> uni9 = Uni.createFrom().item(9);

        UniAssertSubscriber<Integer> subscriber = Uni.combine().all().unis(uni, uni2, uni3, uni4, uni5, uni6, uni7, uni8, uni9)
                .collectFailures()
                .combinedWith((a, b, c, d, e, f, g, h, i) -> a + b + c + d + e + f + g + h + i)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(45);
    }

    @Test
    public void nullUniArray() {
        Uni<?>[] unis = null;
        assertThatThrownBy(() -> Uni.combine().all().unis(unis))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The Uni array is null");
    }

    @Test
    public void emptyArray() {
        assertThatThrownBy(() -> Uni.combine().all().unis())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The Uni set is empty");
    }

    @Test
    public void withNullUniArray() {
        Uni<?>[] unis = new Uni[] {
                Uni.createFrom().item(58),
                null
        };
        assertThatThrownBy(() -> Uni.combine().all().unis(unis))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The Uni at index 1 is null");
    }

    @Test
    public void nullUniList() {
        List<Uni<?>> unis = null;
        assertThatThrownBy(() -> Uni.combine().all().unis(unis))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The Uni array is null");
    }

    @Test
    public void emptyList() {
        assertThatThrownBy(() -> Uni.combine().all().unis(Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The Uni set is empty");
    }

    @Test
    public void withNullUniIterable() {
        List<Uni<Integer>> unis = Arrays.asList(
                Uni.createFrom().item(58),
                null);
        assertThatThrownBy(() -> Uni.combine().all().unis(unis))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The Uni at index 1 is null");
    }

    @Nested
    class ConcurrencyLimitTest {

        @Test
        void combineAllSmokeTest() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().item(2);
            Uni<Integer> c = Uni.createFrom().item(3);
            Uni<Integer> d = Uni.createFrom().item(4);

            UniAssertSubscriber<? extends List<?>> sub = Uni.combine().all().unis(a, b, c, d)
                    .usingConcurrencyOf(1)
                    .combinedWith(unis -> unis)
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            sub.assertCompleted();
            assertThat(sub.getItem()).asInstanceOf(LIST).containsExactly(1, 2, 3, 4);
        }

        @ParameterizedTest
        @CsvSource({
                "1, 100, 1, 400",
                "4, 100, 1, 400",
                "4, 100, 2, 200",
                "4, 100, 16, 100"
        })
        void combineAllSTestWithDelays(int poolSize, long delay, int concurrency, long minTime) {
            ScheduledExecutorService pool = Executors.newScheduledThreadPool(poolSize);

            Uni<Integer> a = Uni.createFrom().future(() -> pool.schedule(() -> 1, delay, TimeUnit.MILLISECONDS));
            Uni<Integer> b = Uni.createFrom().future(() -> pool.schedule(() -> 2, delay, TimeUnit.MILLISECONDS));
            Uni<Integer> c = Uni.createFrom().future(() -> pool.schedule(() -> 3, delay, TimeUnit.MILLISECONDS));
            Uni<Integer> d = Uni.createFrom().future(() -> pool.schedule(() -> 4, delay, TimeUnit.MILLISECONDS));

            long start = System.currentTimeMillis();

            UniAssertSubscriber<? extends List<?>> sub = Uni.combine().all().unis(a, b, c, d)
                    .usingConcurrencyOf(concurrency)
                    .combinedWith(unis -> unis)
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            sub.awaitItem();
            long stop = System.currentTimeMillis();

            sub.assertCompleted();
            assertThat(sub.getItem()).asInstanceOf(LIST).containsExactly(1, 2, 3, 4);
            assertThat(stop - start).isGreaterThanOrEqualTo(minTime);

            pool.shutdownNow();
        }

        @ParameterizedTest
        @CsvSource({
                "1, 100, 1, 300",
                "4, 100, 1, 300",
                "4, 100, 2, 100",
                "4, 100, 16, 0",
        })
        void combineAllSTestWithDelaysAndError(int poolSize, long delay, int concurrency, long minTime) {
            ScheduledExecutorService pool = Executors.newScheduledThreadPool(poolSize);

            Uni<Integer> a = Uni.createFrom().future(() -> pool.schedule(() -> 1, delay, TimeUnit.MILLISECONDS));
            Uni<Integer> b = Uni.createFrom().future(() -> pool.schedule(() -> 2, delay, TimeUnit.MILLISECONDS));
            Uni<Integer> c = Uni.createFrom().future(() -> pool.schedule(() -> 3, delay, TimeUnit.MILLISECONDS));
            Uni<Integer> d = Uni.createFrom().failure(new IOException("yolo"));

            long start = System.currentTimeMillis();

            UniAssertSubscriber<? extends List<?>> sub = Uni.combine().all().unis(a, b, c, d)
                    .usingConcurrencyOf(concurrency)
                    .combinedWith(unis -> unis)
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            sub.awaitFailure();
            long stop = System.currentTimeMillis();

            sub.assertFailedWith(IOException.class, "yolo");
            assertThat(stop - start).isGreaterThanOrEqualTo(minTime);

            pool.shutdownNow();
        }

        @Test
        void combineAllCancellation() {
            ScheduledExecutorService pool = Executors.newScheduledThreadPool(4);

            AtomicReference<UniSubscription> box = new AtomicReference<>();
            AtomicBoolean subscribedToA = new AtomicBoolean();
            AtomicBoolean subscribedToB = new AtomicBoolean();
            AtomicBoolean cancelled = new AtomicBoolean();

            Uni<Integer> a = Uni.createFrom().future(() -> pool.schedule(() -> 63, 500, TimeUnit.MILLISECONDS))
                    .onItem().invoke(() -> {
                        box.get().cancel();
                        cancelled.set(true);
                    })
                    .onCancellation().invoke(() -> cancelled.set(true))
                    .onSubscription().invoke(() -> subscribedToA.set(true));
            Uni<Integer> b = Uni.createFrom().future(() -> pool.schedule(() -> 69, 10, TimeUnit.MILLISECONDS))
                    .onSubscription().invoke(() -> subscribedToB.set(true));

            Uni.combine().all().unis(a, b)
                    .usingConcurrencyOf(1)
                    .combinedWith(list -> list)
                    .onSubscription().invoke(box::set)
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            await().untilTrue(cancelled);
            assertThat(subscribedToA).isTrue();
            assertThat(subscribedToB).isFalse();

            pool.shutdownNow();
        }
    }
}
