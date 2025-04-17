package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.operators.uni.UniOnFailureTransform;
import io.smallrye.mutiny.subscription.UniEmitter;

public class UniOnFailureTransformTest {

    private Uni<Integer> failure;

    @BeforeEach
    public void init() {
        failure = Uni.createFrom().failure(new IOException("boom"));
    }

    @Test
    public void testThatMapperMustNotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item(1).onFailure().transform(null));
    }

    @Test
    public void testThatSourceMustNotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new UniOnFailureTransform<>(null, t -> true, Function.identity(), Throwable.class));
    }

    @Test
    public void testSimpleMapping() {
        UniAssertSubscriber<Integer> subscriber = failure
                .onFailure().transform(t -> new BoomException())
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailed()
                .assertFailedWith(BoomException.class, "BoomException");
    }

    @Test
    public void testWithTwoSubscribers() {
        UniAssertSubscriber<Integer> s1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> s2 = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = failure.onFailure().transform(t -> new BoomException(count.incrementAndGet()));
        uni.subscribe().withSubscriber(s1);
        uni.subscribe().withSubscriber(s2);

        s1.assertFailed()
                .assertFailedWith(BoomException.class, "1");
        s2.assertFailed()
                .assertFailedWith(BoomException.class, "2");
    }

    @Test
    public void testWhenTheMapperThrowsAnException() {
        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();

        failure.onFailure().transform(t -> {
            throw new RuntimeException("failure");
        }).subscribe().withSubscriber(subscriber);

        subscriber.assertFailedWith(RuntimeException.class, "failure");
    }

    @Test
    public void testThatMapperCanNotReturnNull() {
        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();

        failure.onFailure().transform(t -> null).subscribe().withSubscriber(subscriber);

        subscriber.assertFailedWith(NullPointerException.class, "null");
    }

    @Test
    public void testThatMapperIsCalledOnTheRightExecutor() {
        UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>();
        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            AtomicReference<String> threadName = new AtomicReference<>();
            failure
                    .emitOn(executor)
                    .onFailure().transform(fail -> {
                        threadName.set(Thread.currentThread().getName());
                        return new BoomException();
                    })
                    .subscribe().withSubscriber(subscriber);

            subscriber.awaitFailure().assertFailedWith(BoomException.class, "BoomException");
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
        subscriber.assertFailed().assertFailedWith(IllegalStateException.class, "boom");
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
        subscriber.assertFailed()
                .assertFailedWith(CompositeException.class, "boomboom")
                .assertFailedWith(CompositeException.class, " boom");
        assertThat(called).isFalse();
    }

    @Test
    public void verifyThatTheMapperIsNotCalledAfterCancellationWithEmitter() {
        AtomicReference<UniEmitter<? super Integer>> emitter = new AtomicReference<>();
        AtomicBoolean called = new AtomicBoolean();
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom()
                .emitter((Consumer<UniEmitter<? super Integer>>) emitter::set)
                .onFailure().transform(failure -> {
                    called.set(true);
                    return new ArithmeticException(failure.getMessage());
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        assertThat(called).isFalse();
        subscriber.assertNotTerminated().assertSubscribed();
        subscriber.cancel();
        emitter.get().fail(new IOException("boom"));
        assertThat(called).isFalse();
    }

    @Test
    public void verifyThatTheMapperIsNotCalledAfterImmediateCancellationWithEmitter() {
        AtomicReference<UniEmitter<? super Integer>> emitter = new AtomicReference<>();
        AtomicBoolean called = new AtomicBoolean();
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom()
                .emitter((Consumer<UniEmitter<? super Integer>>) emitter::set)
                .onFailure().transform(failure -> {
                    called.set(true);
                    return new ArithmeticException(failure.getMessage());
                })
                .subscribe().withSubscriber(new UniAssertSubscriber<>(true));

        assertThat(called).isFalse();
        subscriber.assertNotTerminated().assertSubscribed();
        subscriber.cancel();
        emitter.get().fail(new IOException("boom"));
        assertThat(called).isFalse();
    }

    @Test
    public void testTypedCallOnFailureWithSpecificException() {
        SpecificException specificException = new SpecificException(500);
        Uni<Integer> failure = Uni.createFrom().failure(specificException);
        AtomicInteger called = new AtomicInteger(-1);

        UniAssertSubscriber<Integer> subscriber = failure
                .onFailure(SpecificException.class) // Specify the exception type
                .call(se -> {
                    called.set(se.getErrorCode());
                    return Uni.createFrom().voidItem();
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(SpecificException.class);
        assertThat(called).hasValue(500);
    }

    @Test
    public void testTypedInvokeOnFailureWithSpecificException() {
        SpecificException specificException = new SpecificException(500);
        Uni<Integer> failure = Uni.createFrom().failure(specificException);
        AtomicInteger called = new AtomicInteger(-1);

        UniAssertSubscriber<Integer> subscriber = failure
                .onFailure(SpecificException.class) // Specify the exception type
                .invoke(se -> called.set(se.getErrorCode())) // Perform a side-effect
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(SpecificException.class);
        assertThat(called).hasValue(500);
    }

    @Test
    public void testTypedTransformOnFailureWithSpecificException() {
        SpecificException specificException = new SpecificException(500);
        Uni<Integer> failure = Uni.createFrom().failure(specificException);
        AtomicInteger called = new AtomicInteger(-1);

        UniAssertSubscriber<Integer> subscriber = failure
                .onFailure(SpecificException.class) // Specify the exception type
                .transform(se -> {
                    called.set(se.getErrorCode());
                    return new IllegalStateException("Transformed: " + se.getErrorCode());
                }) // Transform the exception
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(IllegalStateException.class, "Transformed: 500");
        assertThat(called).hasValue(500);
    }

    @Test
    public void testTypedRecoverWithUniOnFailureWithSpecificException() {
        SpecificException specificException = new SpecificException(500);
        Uni<Integer> failure = Uni.createFrom().failure(specificException);
        AtomicInteger called = new AtomicInteger(-1);

        UniAssertSubscriber<Integer> subscriber = failure
                .onFailure(SpecificException.class) // Specify the exception type
                .recoverWithUni(se -> {
                    called.set(se.getErrorCode());
                    return Uni.createFrom().item(se.getErrorCode());
                }) // Recover with a Uni
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(500);
        assertThat(called).hasValue(500);
    }

    @Test
    public void testTypedCallOnFailureWithNonMatchingException() {
        SpecificException specificException = new SpecificException(500);
        Uni<Integer> failure = Uni.createFrom().failure(specificException);
        AtomicInteger called = new AtomicInteger(-1);

        UniAssertSubscriber<Integer> subscriber = failure
                .onFailure(IllegalStateException.class) // Specify a type that doesn't match
                .call(illegalStateException -> {
                    // This block should not be executed
                    called.set(1);
                    throw new AssertionError("Should not be called");
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(SpecificException.class);
        assertThat(called).hasValue(-1);
    }

    @Test
    public void testTypedInvokeOnFailureWithNonMatchingException() {
        SpecificException specificException = new SpecificException(500);
        Uni<Integer> failure = Uni.createFrom().failure(specificException);
        AtomicInteger called = new AtomicInteger(-1);

        UniAssertSubscriber<Integer> subscriber = failure
                .onFailure(IllegalStateException.class) // Specify a type that doesn't match
                .invoke(se -> called.set(1)) // This block should not be executed
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(SpecificException.class);
        assertThat(called).hasValue(-1);
    }

    @Test
    public void testTypedTransformOnFailureWithNonMatchingException() {
        SpecificException specificException = new SpecificException(500);
        Uni<Integer> failure = Uni.createFrom().failure(specificException);
        AtomicInteger called = new AtomicInteger(-1);

        UniAssertSubscriber<Integer> subscriber = failure
                .onFailure(IllegalStateException.class) // Specify a type that doesn't match
                .transform(se -> {
                    called.set(1); // This block should not be executed
                    return new IllegalStateException("Transformed");
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(SpecificException.class);
        assertThat(called).hasValue(-1);
    }

    @Test
    public void testTypedRecoverWithUniOnFailureWithNonMatchingException() {
        SpecificException specificException = new SpecificException(500);
        Uni<Integer> failure = Uni.createFrom().failure(specificException);
        AtomicInteger called = new AtomicInteger(-1);

        UniAssertSubscriber<Integer> subscriber = failure
                .onFailure(IllegalStateException.class) // Specify a type that doesn't match
                .recoverWithUni(se -> {
                    called.set(1); // This block should not be executed
                    return Uni.createFrom().item(1);
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(SpecificException.class);
        assertThat(called).hasValue(-1);
    }

    private static class BoomException extends Exception {
        BoomException() {
            super("BoomException");
        }

        BoomException(int count) {
            super(Integer.toString(count));
        }
    }

    private static class SpecificException extends Exception {

        private final int errorCode;

        SpecificException(int errorCode) {
            super("SpecificException");
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }
    }

}
