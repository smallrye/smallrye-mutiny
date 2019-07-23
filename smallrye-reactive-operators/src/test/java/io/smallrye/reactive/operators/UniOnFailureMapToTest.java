package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class UniOnFailureMapToTest {

    @Test(expected = IllegalArgumentException.class)
    public void testThatMapperMustNotBeNull() {
        Uni.createFrom().result(1).onFailure().mapTo(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatSourceMustNotBeNull() {
        new UniMapOnFailure<>(null, t -> true, Function.identity());
    }

    private Uni<Integer> failure = Uni.createFrom().failure(new IOException("boom"));

    private class BoomException extends Exception {
        BoomException() {
            super("BoomException");
        }

        BoomException(int count) {
            super(Integer.toString(count));
        }
    }

    @Test
    public void testSimpleMapping() {
        AssertSubscriber<Integer> subscriber = failure
                .onFailure().mapTo(t -> new BoomException())
                .subscribe().withSubscriber(AssertSubscriber.create());
        subscriber.assertCompletedWithFailure()
                .assertFailure(BoomException.class, "BoomException");
    }

    @Test
    public void testWithTwoSubscribers() {
        AssertSubscriber<Integer> ts1 = AssertSubscriber.create();
        AssertSubscriber<Integer> ts2 = AssertSubscriber.create();


        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = failure.onFailure().mapTo(t -> new BoomException(count.incrementAndGet()));
        uni.subscribe().withSubscriber(ts1);
        uni.subscribe().withSubscriber(ts2);

        ts1.assertCompletedWithFailure()
                .assertFailure(BoomException.class, "1");
        ts2.assertCompletedWithFailure()
                .assertFailure(BoomException.class, "2");
    }

    @Test
    public void testWhenTheMapperThrowsAnException() {
        AssertSubscriber<Object> ts = AssertSubscriber.create();

        failure.onFailure().mapTo(t -> {
            throw new RuntimeException("failure");
        }).subscribe().withSubscriber(ts);

        ts.assertFailure(RuntimeException.class, "failure");
    }

    @Test
    public void testThatMapperCanNotReturnNull() {
        AssertSubscriber<Object> ts = AssertSubscriber.create();

        failure.onFailure().mapTo(t -> null).subscribe().withSubscriber(ts);

        ts.assertFailure(NullPointerException.class, "null");
    }

    @Test
    public void testThatMapperIsCalledOnTheRightExecutor() {
        AssertSubscriber<Integer> ts = new AssertSubscriber<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            AtomicReference<String> threadName = new AtomicReference<>();
            failure
                    .handleFailureOn(executor)
                    .onFailure().mapTo(fail -> {
                threadName.set(Thread.currentThread().getName());
                return new BoomException();
            })
                    .subscribe().withSubscriber(ts);

            ts.await().assertFailure(BoomException.class, "BoomException");
            assertThat(threadName).isNotNull().doesNotHaveValue("main");
            assertThat(ts.getOnFailureThreadName()).isEqualTo(threadName.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testThatMapperIsNotCallOnResult() {
        AssertSubscriber<Integer> ts = AssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().result(1)
                .onFailure().mapTo(f -> {
            called.set(true);
            return f;
        })
                .subscribe().withSubscriber(ts);
        ts.assertResult(1);
        assertThat(called).isFalse();
    }
}