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
        Uni.createFrom().item(1).onFailure().transform(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatSourceMustNotBeNull() {
        new UniOnFailureMap<>(null, t -> true, Function.identity());
    }

    @Test
    public void testSimpleMapping() {
        UniAssertSubscriber<Integer> subscriber = failure
                .onFailure().transform(t -> new BoomException())
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompletedWithFailure()
                .assertFailure(BoomException.class, "BoomException");
    }

    @Test
    public void testWithTwoSubscribers() {
        UniAssertSubscriber<Integer> s1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> s2 = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = failure.onFailure().transform(t -> new BoomException(count.incrementAndGet()));
        uni.subscribe().withSubscriber(s1);
        uni.subscribe().withSubscriber(s2);

        s1.assertCompletedWithFailure()
                .assertFailure(BoomException.class, "1");
        s2.assertCompletedWithFailure()
                .assertFailure(BoomException.class, "2");
    }

    @Test
    public void testWhenTheMapperThrowsAnException() {
        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();

        failure.onFailure().transform(t -> {
            throw new RuntimeException("failure");
        }).subscribe().withSubscriber(subscriber);

        subscriber.assertFailure(RuntimeException.class, "failure");
    }

    @Test
    public void testThatMapperCanNotReturnNull() {
        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();

        failure.onFailure().transform(t -> null).subscribe().withSubscriber(subscriber);

        subscriber.assertFailure(NullPointerException.class, "null");
    }

    @Test
    public void testThatMapperIsCalledOnTheRightExecutor() {
        UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            AtomicReference<String> threadName = new AtomicReference<>();
            failure
                    .emitOn(executor)
                    .onFailure().transform(fail -> {
                        threadName.set(Thread.currentThread().getName());
                        return new BoomException();
                    })
                    .subscribe().withSubscriber(subscriber);

            subscriber.await().assertFailure(BoomException.class, "BoomException");
            assertThat(threadName).isNotNull().doesNotHaveValue("main");
            assertThat(subscriber.getOnFailureThreadName()).isEqualTo(threadName.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testThatMapperIsNotCalledOnItem() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().item(1)
                .onFailure().transform(f -> {
                    called.set(true);
                    return f;
                })
                .subscribe().withSubscriber(subscriber);
        subscriber.assertItem(1);
        assertThat(called).isFalse();
    }

    @Test
    public void testThatMapperIsNotCalledOnNonMatchingPredicate() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure(IOException.class).transform(f -> {
                    called.set(true);
                    return new IllegalArgumentException("Karamba");
                })
                .subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedWithFailure().assertFailure(IllegalStateException.class, "boom");
        assertThat(called).isFalse();
    }

    @Test
    public void testThatMapperIsNotCalledWhenPredicateThrowsAnException() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onFailure(t -> {
                    throw new IllegalArgumentException("boomboom");
                }).transform(f -> {
                    called.set(true);
                    return new RuntimeException("Karamba");
                })
                .subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedWithFailure()
                .assertFailure(CompositeException.class, "boomboom")
                .assertFailure(CompositeException.class, " boom");
        assertThat(called).isFalse();
    }

    private static class BoomException extends Exception {
        BoomException() {
            super("BoomException");
        }

        BoomException(int count) {
            super(Integer.toString(count));
        }
    }

}
