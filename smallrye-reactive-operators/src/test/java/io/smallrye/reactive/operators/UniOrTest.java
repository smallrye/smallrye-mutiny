package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

public class UniOrTest {

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

    @After
    public void shutdown() {
        executor.shutdown();
    }


    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testWithNullAsIterable() {
        Uni.combine().any().of((Iterable) null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testWithNullAsArray() {
        Uni.combine().any().of((Uni[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithItemInIterable() {
        List<Uni<String>> unis = new ArrayList<>();
        unis.add(Uni.createFrom().result("foo"));
        unis.add(null);
        unis.add(Uni.createFrom().result("bar"));
        Uni.combine().any().of(unis);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithItemInArray() {
        Uni.combine().any().of(Uni.createFrom().result("foo"), null, Uni.createFrom().result("bar"));
    }

    @Test
    public void testWithNoCandidate() {
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        Uni.combine().any().<Void>of().subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedSuccessfully().assertResult(null);
    }

    @Test
    public void testWithSingleItemCompletingSuccessfully() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        Uni.combine().any().of(Uni.createFrom().result("foo")).subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedSuccessfully().assertResult("foo");
    }

    @Test
    public void testWithSingleItemCompletingWithAFailure() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        Uni.combine().any().of(Uni.createFrom().<String>failure(new IOException("boom"))).subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedWithFailure().assertFailure(IOException.class, "boom");
    }

    @Test
    public void testWithTwoUnisCompletingImmediately() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        Uni.combine().any().of(Uni.createFrom().result("foo"), Uni.createFrom().result("bar")).subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedSuccessfully().assertResult("foo");
    }

    @Test
    public void testWithTwoUnisCompletingWithAFailure() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        Uni.combine().any().of(Uni.createFrom().failure(new IOException("boom")), Uni.createFrom().result("foo")).subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedWithFailure().assertFailure(IOException.class, "boom");
    }

    @Test
    public void testWithADelayedUni() {
        UniAssertSubscriber<String> subscriber1 = UniAssertSubscriber.create();
        Uni.combine().any().of(Uni.createFrom().result("foo")
                .onResult().delayIt().onExecutor(executor).by(Duration.ofMillis(10)), Uni.createFrom().result("bar"))
                .subscribe().withSubscriber(subscriber1);
        subscriber1.assertCompletedSuccessfully().assertResult("bar");

        UniAssertSubscriber<String> subscriber2 = UniAssertSubscriber.create();
        Uni.combine().any().of(Uni.createFrom().result("foo").onResult().delayIt().onExecutor(executor).by(Duration.ofMillis(10)),
                Uni.createFrom().result("bar").onResult().delayIt().onExecutor(executor).by(Duration.ofMillis(100)))
                .subscribe().withSubscriber(subscriber2);
        subscriber2.await().assertCompletedSuccessfully().assertResult("foo");
    }

    @Test(timeout = 1000)
    public void testBlockingWithDelay() {
        Uni<Integer> uni1 = Uni.createFrom().nullValue()
                .onResult().delayIt().onExecutor(executor).by(Duration.ofMillis(500))
                .map(x -> 1);
        Uni<Integer> uni2 = Uni.createFrom().nullValue()
                .onResult().delayIt().onExecutor(executor).by(Duration.ofMillis(50))
                .map(x -> 2);
        assertThat(Uni.combine().any().of(uni1, uni2).await().indefinitely()).isEqualTo(2);
    }

    @Test(timeout = 1000)
    public void testCompletingAgainstEmpty() {
        Uni<Integer> uni1 = Uni.createFrom().nullValue().map(x -> 1);
        Uni<Integer> uni2 = Uni.createFrom().nullValue().onResult().delayIt().onExecutor(executor).by(Duration.ofMillis(50)).map(x -> 2);
        assertThat(Uni.combine().any().of(uni1, uni2).await().indefinitely()).isEqualTo(1);
    }

    @Test(timeout = 1000)
    public void testCompletingAgainstNever() {
        Uni<Integer> uni1 = Uni.createFrom().nothing().map(x -> 1);
        Uni<Integer> uni2 = Uni.createFrom().nullValue().onResult().delayIt().onExecutor(executor).by(Duration.ofMillis(50)).map(x -> 2);
        assertThat(Uni.combine().any().of(uni1, uni2).await().asOptional().indefinitely()).contains(2);
    }

    @Test
    public void testWithThreeImmediateChallengers() {
        Uni<Integer> any = Uni.combine().any().of(Uni.createFrom().result(1), Uni.createFrom().result(2), Uni.createFrom().result(3));

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        any.subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedSuccessfully().assertResult(1);
    }

}