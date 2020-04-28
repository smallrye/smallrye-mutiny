package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;

import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.tuples.Tuple3;
import io.smallrye.mutiny.tuples.Tuple4;
import io.smallrye.mutiny.tuples.Tuple5;

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

        assertThatThrownBy(() -> Uni.combine().all().unis(uni1, uni2, uni3, failed, uni4).discardItems().await().indefinitely())
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

}
