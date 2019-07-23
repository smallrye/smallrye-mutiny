package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class UniOnFailureRecoveryTest {

    private Uni<Integer> failed = Uni.createFrom().failure(IOException::new);


    @Test
    public void testRecoverWithDirectValue() {
        Integer result = failed.onFailure().recoverWithResult(23).await().indefinitely();
        Integer result2 = Uni.createFrom().result(1).onFailure().recoverWithResult(23).await().indefinitely();
        assertThat(result).isEqualTo(23);
        assertThat(result2).isEqualTo(1);
    }

    @Test
    public void testRecoverWithNullValue() {
        Integer result = failed.onFailure().recoverWithResult((Integer) null).await().indefinitely();
        assertThat(result).isEqualTo(null);
    }

    @Test
    public void testRecoverWithSupplierOfValue() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> recovered = failed.onFailure().recoverWithResult(() -> 23 + count.getAndIncrement());
        Integer result = recovered.await().indefinitely();
        Integer result2 = recovered.await().indefinitely();
        assertThat(result).isEqualTo(23);
        assertThat(result2).isEqualTo(24);
    }

    @Test
    public void testWhenSupplierThrowsAnException() {
        Uni<Integer> recovered = failed.onFailure().recoverWithResult(() -> {
            throw new IllegalStateException("boom");
        });

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> recovered.await().indefinitely())
                .withMessage("boom");

    }

    @Test
    public void testRecoverWithFunctionProducingOfValue() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> recovered = failed.onFailure().recoverWithResult(fail -> 23 + count.getAndIncrement());
        Integer result = recovered.await().indefinitely();
        Integer result2 = recovered.await().indefinitely();
        assertThat(result).isEqualTo(23);
        assertThat(result2).isEqualTo(24);
    }

    @Test
    public void testWithPredicateOnClass() {
        Integer result = failed.onFailure(IOException.class).recoverWithResult(23).await().indefinitely();
        assertThat(result).isEqualTo(23);
        assertThatExceptionOfType(CompletionException.class)
                .isThrownBy(() -> failed.onFailure(IllegalStateException.class).recoverWithResult(23).await().indefinitely())
                .withCauseExactlyInstanceOf(IOException.class);
    }

    @Test
    public void testWithPredicate() {
        Integer result = failed.onFailure(f -> f instanceof IOException).recoverWithResult(23).await().indefinitely();
        assertThat(result).isEqualTo(23);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> failed.onFailure(f -> {
                    throw new IllegalArgumentException("BOOM!");
                }).recoverWithResult(23).await().indefinitely())
                .withMessageEndingWith("BOOM!");
    }

    @Test
    public void testRecoverWithUni() {
        Integer result = failed.onFailure().recoverWithUni(Uni.createFrom().result(25)).await().indefinitely();
        Integer result2 = Uni.createFrom().result(1).onFailure().recoverWithUni(Uni.createFrom().result(25)).await().indefinitely();
        assertThat(result).isEqualTo(25);
        assertThat(result2).isEqualTo(1);
    }

    @Test
    public void testRecoverWithUniNull() {
        Integer result = failed.onFailure().recoverWithUni(Uni.createFrom().result(1).map(i -> null)).await().indefinitely();
        assertThat(result).isEqualTo(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRecoverWithUniFail() {
        failed.onFailure().recoverWithUni(Uni.createFrom().failure(IllegalArgumentException::new)).await().indefinitely();
    }

    @Test
    public void testRecoverWithSupplierOfUni() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = failed.onFailure().recoverWithUni(() -> Uni.createFrom().result(() -> 25 + count.incrementAndGet()));
        Integer result = uni.await().indefinitely();
        Integer result2 = uni.await().indefinitely();
        assertThat(result).isEqualTo(26);
        assertThat(result2).isEqualTo(27);
    }

    @Test
    public void testWhenSupplierOfUniThrowsAnException() {
        Uni<Integer> recovered = failed.onFailure().recoverWithUni(() -> {
            throw new IllegalStateException("boom");
        });

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> recovered.await().indefinitely())
                .withMessage("boom");

    }

    @Test
    public void testRecoverWithFunctionProducingOfUni() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> recovered = failed.onFailure().recoverWithUni(fail -> Uni.createFrom().result(() -> 23 + count.getAndIncrement()));
        Integer result = recovered.await().indefinitely();
        Integer result2 = recovered.await().indefinitely();
        assertThat(result).isEqualTo(23);
        assertThat(result2).isEqualTo(24);
    }

    @Test
    public void testRecoveringWithUniWithPredicateOnClass() {
        Integer result = failed.onFailure(IOException.class).recoverWithUni(Uni.createFrom().result(23)).await().indefinitely();
        assertThat(result).isEqualTo(23);
        assertThatExceptionOfType(CompletionException.class)
                .isThrownBy(() -> failed
                        .onFailure(IllegalStateException.class).recoverWithUni(Uni.createFrom().result(23)).await().indefinitely())
                .withCauseExactlyInstanceOf(IOException.class);
    }

    @Test
    public void testRecoveringWithUniWithPredicate() {
        Integer result = failed
                .onFailure(f -> f instanceof IOException).recoverWithUni(Uni.createFrom().result(23)).await().indefinitely();
        assertThat(result).isEqualTo(23);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> failed.onFailure(f -> {
                    throw new IllegalArgumentException("BOOM!");
                }).recoverWithResult(23).await().indefinitely())
                .withMessageEndingWith("BOOM!");
    }


    @Test
    public void testNotCalledOnResult() {
        AssertSubscriber<Integer> ts = AssertSubscriber.create();
        Uni.createFrom().result(1)
                .onFailure().recoverWithUni(v -> Uni.createFrom().result(2))
                .subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertResult(1);
    }


    @Test
    public void testCalledOnFailure() {
        AssertSubscriber<Integer> ts = AssertSubscriber.create();

        Uni.createFrom().<Integer>failure(new RuntimeException("boom"))
                .onFailure().recoverWithUni(fail -> Uni.createFrom().result(2))
                .subscribe().withSubscriber(ts);

        ts.assertCompletedSuccessfully().assertResult(2);
    }

    @Test
    public void testCalledOnFailureWithDirectResult() {
        AssertSubscriber<Integer> ts = AssertSubscriber.create();

        Uni.createFrom().<Integer>failure(new RuntimeException("boom"))
                .onFailure().recoverWithResult(fail -> 2)
                .subscribe().withSubscriber(ts);

        ts.assertCompletedSuccessfully().assertResult(2);
    }

    @Test
    public void testWithMappingOfFailure() {
        AssertSubscriber<Integer> ts = AssertSubscriber.create();
        Uni.createFrom().<Integer>failure(new Exception())
                .onFailure().mapTo(f -> new RuntimeException("boom"))
                .subscribe().withSubscriber(ts);
        ts.assertCompletedWithFailure()
                .assertFailure(RuntimeException.class, "boom");
    }


    @Test
    public void testWithMappingOfFailureAndPredicates() {
        AssertSubscriber<Integer> ts = AssertSubscriber.create();
        Uni.createFrom().<Integer>failure(new IOException())
                .onFailure().mapTo(t -> new IndexOutOfBoundsException())
                .onFailure(IOException.class).recoverWithUni(Uni.createFrom().result(1))
                .onFailure(IndexOutOfBoundsException.class).recoverWithUni(Uni.createFrom().result(2))
                .subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertResult(2);
    }


}
