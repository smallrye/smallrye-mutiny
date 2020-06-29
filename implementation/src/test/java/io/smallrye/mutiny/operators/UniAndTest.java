package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;

import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.*;

public class UniAndTest {

    @Test
    public void testWithTwoSimpleUnis() {

        Uni<Integer> uni = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);

        UniAssertSubscriber<Tuple2<Integer, Integer>> subscriber = Uni.combine().all().unis(uni, uni2).asTuple().subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        assertThat(subscriber.getItem().asList()).containsExactly(1, 2);
    }

    @Test
    public void testWithTwoOneFailure() {
        UniAssertSubscriber<Tuple2<Integer, Integer>> subscriber = Uni.combine().all().unis(
                Uni.createFrom().item(1),
                Uni.createFrom().<Integer> failure(new IOException("boom"))).asTuple()
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailure(IOException.class, "boom");
    }

    @Test(expectedExceptions = TimeoutException.class)
    public void testWithNever() {
        Uni<Tuple3<Integer, Integer, Object>> tuple = Uni.combine().all().unis(
                Uni.createFrom().item(1),
                Uni.createFrom().item(2),
                Uni.createFrom().nothing()).asTuple();
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
    public void testWithCombinatorAndDeprecatedApis() {
        Uni<Integer> uni1 = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);
        UniAssertSubscriber<Integer> subscriber = uni1.and().unis(uni2, uni3)
                .combinedWith((i1, i2, i3) -> i1 + i2 + i3)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.await().assertItem(6);
    }

    @Test
    public void testTerminationJoin() {
        Uni<Void> uni = Uni.combine().all().unis(Uni.createFrom().item(1), Uni.createFrom().item("hello")).asTuple()
                .onItem().ignore().andContinueWithNull();

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

        UniAssertSubscriber<Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> subscriber = Uni.combine().all()
                .unis(uni1, uni2, uni3, uni4, uni5, uni6).asTuple().subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        assertThat(subscriber.getItem().asList()).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    public void testWithFourUnis() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);

        UniAssertSubscriber<Tuple4<Integer, Integer, Integer, Integer>> subscriber = Uni.combine().all()
                .unis(uni, uni, uni2, uni3).asTuple()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        assertThat(subscriber.getItem().asList()).containsExactly(1, 1, 2, 3);
    }

    @Test
    public void testWithFourUnisAndDeprecatedApis() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        Uni<Integer> uni2 = Uni.createFrom().item(2);
        Uni<Integer> uni3 = Uni.createFrom().item(3);

        UniAssertSubscriber<Tuple4<Integer, Integer, Integer, Integer>> subscriber = uni.and()
                .unis(uni, uni2, uni3).asTuple()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

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

        UniAssertSubscriber<Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>> subscriber = Uni.combine()
                .all()
                .unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7).asTuple().subscribe()
                .withSubscriber(UniAssertSubscriber.create());

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
                .unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7, uni8).asTuple().subscribe()
                .withSubscriber(UniAssertSubscriber.create());

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
                .unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7, uni8, uni9).asTuple().subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        assertThat(subscriber.getItem().asList()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

}
