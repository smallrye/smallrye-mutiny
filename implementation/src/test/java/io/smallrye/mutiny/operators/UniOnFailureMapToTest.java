package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;

public class UniOnFailureMapToTest {

    private Uni<Integer> failure;

    @BeforeMethod
    public void init() {
        failure = Uni.createFrom().failure(new IOException("boom"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatMapperMustNotBeNull() {
        Uni.createFrom().item(1).onFailure().mapTo(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatSourceMustNotBeNull() {
        new UniMapOnFailure<>(null, t -> true, Function.identity());
    }

    @Test
    public void testSimpleMapping() {
        UniAssertSubscriber<Integer> subscriber = failure
                .onFailure().mapTo(t -> new BoomException())
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompletedWithFailure()
                .assertFailure(BoomException.class, "BoomException");
    }

    @Test
    public void testWithTwoSubscribers() {
        UniAssertSubscriber<Integer> ts1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> ts2 = UniAssertSubscriber.create();

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
        UniAssertSubscriber<Object> ts = UniAssertSubscriber.create();

        failure.onFailure().mapTo(t -> {
            throw new RuntimeException("failure");
        }).subscribe().withSubscriber(ts);

        ts.assertFailure(RuntimeException.class, "failure");
    }

    @Test
    public void testThatMapperCanNotReturnNull() {
        UniAssertSubscriber<Object> ts = UniAssertSubscriber.create();

        failure.onFailure().mapTo(t -> null).subscribe().withSubscriber(ts);

        ts.assertFailure(NullPointerException.class, "null");
    }

    @Test
    public void testThatMapperIsCalledOnTheRightExecutor() {
        UniAssertSubscriber<Integer> ts = new UniAssertSubscriber<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            AtomicReference<String> threadName = new AtomicReference<>();
            failure
                    .emitOn(executor)
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
    public void testThatMapperIsNotCalledOnResult() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().item(1)
                .onFailure().mapTo(f -> {
                    called.set(true);
                    return f;
                })
                .subscribe().withSubscriber(ts);
        ts.assertItem(1);
        assertThat(called).isFalse();
    }

    @Test
    public void testThatMapperIsNotCalledOnNonMatchingPredicate() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure(IOException.class).mapTo(f -> {
                    called.set(true);
                    return new IllegalArgumentException("Karamba");
                })
                .subscribe().withSubscriber(ts);
        ts.assertCompletedWithFailure().assertFailure(IllegalStateException.class, "boom");
        assertThat(called).isFalse();
    }

    @Test
    public void testThatMapperIsNotCalledWhenPredicateThrowsAnException() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure(t -> {
                    throw new IllegalArgumentException("boomboom");
                }).mapTo(f -> {
                    called.set(true);
                    return new RuntimeException("Karamba");
                })
                .subscribe().withSubscriber(ts);
        ts.assertCompletedWithFailure()
                .assertFailure(CompositeException.class, "boomboom")
                .assertFailure(CompositeException.class, " boom");
        assertThat(called).isFalse();
    }

    private class BoomException extends Exception {
        BoomException() {
            super("BoomException");
        }

        BoomException(int count) {
            super(Integer.toString(count));
        }
    }

}
