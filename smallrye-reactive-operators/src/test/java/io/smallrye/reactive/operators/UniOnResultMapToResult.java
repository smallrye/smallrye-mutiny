package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class UniOnResultMapToResult {


    @Test(expected = IllegalArgumentException.class)
    public void testThatMapperMustNotBeNull() {
        Uni.createFrom().result(1).map(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatSourceMustNotBeNull() {
        new UniMapOnResult<>(null, Function.identity());
    }

    private Uni<Integer> one = Uni.createFrom().result(1);

    @Test
    public void testSimpleMapping() {
        AssertSubscriber<Integer> ts = AssertSubscriber.create();


        one.map(v -> v + 1).subscribe().withSubscriber(ts);

        ts.assertCompletedSuccessfully()
                .assertResult(2);
    }

    @Test
    public void testWithTwoSubscribers() {
        AssertSubscriber<Integer> ts1 = AssertSubscriber.create();
        AssertSubscriber<Integer> ts2 = AssertSubscriber.create();


        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = one.map(v -> v + count.incrementAndGet());
        uni.subscribe().withSubscriber(ts1);
        uni.subscribe().withSubscriber(ts2);

        ts1.assertCompletedSuccessfully()
                .assertResult(2);
        ts2.assertCompletedSuccessfully()
                .assertResult(3);
    }

    @Test
    public void testWhenTheMapperThrowsAnException() {
        AssertSubscriber<Object> ts = AssertSubscriber.create();

        one.map(v -> {
            throw new RuntimeException("failure");
        }).subscribe().withSubscriber(ts);

        ts.assertFailure(RuntimeException.class, "failure");
    }

    @Test
    public void testThatMapperCanReturnNull() {
        AssertSubscriber<Object> ts = AssertSubscriber.create();

        one.map(v -> null).subscribe().withSubscriber(ts);

        ts.assertCompletedSuccessfully().assertResult(null);
    }

    @Test
    public void testThatMapperIsCalledWithNull() {
        AssertSubscriber<String> ts = AssertSubscriber.create();
        Uni.createFrom().result((String) null).map(x -> "foo").subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertResult("foo");
    }

    @Test
    public void testThatMapperIsCalledOnTheRightExecutor() {
        AssertSubscriber<Integer> ts = new AssertSubscriber<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            AtomicReference<String> threadName = new AtomicReference<>();
            Uni.createFrom().result(1)
                    .handleResultOn(executor)
                    .map(i -> {
                        threadName.set(Thread.currentThread().getName());
                        return i + 1;
                    })
                    .subscribe().withSubscriber(ts);

            ts.await().assertCompletedSuccessfully().assertResult(2);
            assertThat(threadName).isNotNull().doesNotHaveValue("main");
            assertThat(ts.getOnResultThreadName()).isEqualTo(threadName.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testThatMapperIsNotCalledIfPreviousStageFailed() {
        AssertSubscriber<Integer> ts = AssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().<Integer>failure(new Exception("boom"))
                .map(x -> {
                    called.set(true);
                    return x + 1;
                }).subscribe().withSubscriber(ts);

        ts.assertFailure(Exception.class, "boom");
        assertThat(called).isFalse();
    }
}