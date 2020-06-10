package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;

public class UniOnFailureRecoveryTest {

    private Uni<Integer> failed;

    @BeforeMethod
    public void init() {
        failed = Uni.createFrom().failure(IOException::new);
    }

    @Test
    public void testRecoverWithDirectValue() {
        Integer value = failed.onFailure().recoverWithItem(23).await().indefinitely();
        Integer value2 = Uni.createFrom().item(1).onFailure().recoverWithItem(23).await().indefinitely();
        assertThat(value).isEqualTo(23);
        assertThat(value2).isEqualTo(1);
    }

    @Test
    public void testRecoverWithNullValue() {
        Integer value = failed.onFailure().recoverWithItem((Integer) null).await().indefinitely();
        assertThat(value).isEqualTo(null);
    }

    @Test
    public void testRecoverWithSupplierOfValue() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> recovered = failed.onFailure().recoverWithItem(() -> 23 + count.getAndIncrement());
        Integer value = recovered.await().indefinitely();
        Integer value2 = recovered.await().indefinitely();
        assertThat(value).isEqualTo(23);
        assertThat(value2).isEqualTo(24);
    }

    @Test
    public void testWhenSupplierThrowsAnException() {
        Uni<Integer> recovered = failed.onFailure().recoverWithItem(() -> {
            throw new IllegalStateException("boom");
        });

        assertThatExceptionOfType(CompositeException.class)
                .isThrownBy(() -> recovered.await().indefinitely())
                .withMessageContaining("boom");

    }

    @Test
    public void testRecoverWithFunctionProducingOfValue() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> recovered = failed.onFailure().recoverWithItem(fail -> 23 + count.getAndIncrement());
        Integer value = recovered.await().indefinitely();
        Integer value2 = recovered.await().indefinitely();
        assertThat(value).isEqualTo(23);
        assertThat(value2).isEqualTo(24);
    }

    @Test
    public void testWithPredicateOnClass() {
        Integer value = failed.onFailure(IOException.class).recoverWithItem(23).await().indefinitely();
        assertThat(value).isEqualTo(23);
        assertThatExceptionOfType(CompletionException.class)
                .isThrownBy(() -> failed.onFailure(IllegalStateException.class).recoverWithItem(23).await()
                        .indefinitely())
                .withCauseExactlyInstanceOf(IOException.class);
    }

    @Test
    public void testWithPredicate() {
        Integer value = failed.onFailure(f -> f instanceof IOException).recoverWithItem(23).await().indefinitely();
        assertThat(value).isEqualTo(23);

        assertThatExceptionOfType(CompositeException.class)
                .isThrownBy(() -> failed.onFailure(f -> {
                    throw new IllegalArgumentException("BOOM!");
                }).recoverWithItem(23).await().indefinitely())
                .withMessageContaining("BOOM!");
    }

    @Test
    public void testRecoverWithUni() {
        Integer value = failed.onFailure().recoverWithUni(Uni.createFrom().item(25)).await().indefinitely();
        Integer value2 = Uni.createFrom().item(1).onFailure().recoverWithUni(Uni.createFrom().item(25)).await()
                .indefinitely();
        assertThat(value).isEqualTo(25);
        assertThat(value2).isEqualTo(1);
    }

    @Test
    public void testRecoverWithUniNull() {
        Integer value = failed.onFailure().recoverWithUni(Uni.createFrom().item(1).map(i -> null)).await()
                .indefinitely();
        assertThat(value).isEqualTo(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRecoverWithUniFail() {
        failed.onFailure().recoverWithUni(Uni.createFrom().failure(IllegalArgumentException::new)).await()
                .indefinitely();
    }

    @Test
    public void testRecoverWithSupplierOfUni() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = failed.onFailure()
                .recoverWithUni(() -> Uni.createFrom().item(() -> 25 + count.incrementAndGet()));
        Integer value = uni.await().indefinitely();
        Integer value2 = uni.await().indefinitely();
        assertThat(value).isEqualTo(26);
        assertThat(value2).isEqualTo(27);
    }

    @Test
    public void testWhenSupplierOfUniThrowsAnException() {
        Uni<Integer> recovered = failed.onFailure().recoverWithUni(() -> {
            throw new IllegalStateException("boom");
        });

        assertThatExceptionOfType(CompositeException.class)
                .isThrownBy(() -> recovered.await().indefinitely())
                .withMessageContaining("boom");

    }

    @Test
    public void testRecoverWithFunctionProducingOfUni() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> recovered = failed.onFailure()
                .recoverWithUni(fail -> Uni.createFrom().item(() -> 23 + count.getAndIncrement()));
        Integer value = recovered.await().indefinitely();
        Integer value2 = recovered.await().indefinitely();
        assertThat(value).isEqualTo(23);
        assertThat(value2).isEqualTo(24);
    }

    @Test
    public void testRecoveringWithUniWithPredicateOnClass() {
        Integer value = failed.onFailure(IOException.class).recoverWithUni(Uni.createFrom().item(23)).await()
                .indefinitely();
        assertThat(value).isEqualTo(23);
        assertThatExceptionOfType(CompletionException.class)
                .isThrownBy(() -> failed
                        .onFailure(IllegalStateException.class).recoverWithUni(Uni.createFrom().item(23)).await()
                        .indefinitely())
                .withCauseExactlyInstanceOf(IOException.class);
    }

    @Test
    public void testRecoveringWithUniWithPredicate() {
        Integer value = failed
                .onFailure(f -> f instanceof IOException).recoverWithUni(Uni.createFrom().item(23)).await()
                .indefinitely();
        assertThat(value).isEqualTo(23);
        assertThatExceptionOfType(CompositeException.class)
                .isThrownBy(() -> failed.onFailure(f -> {
                    throw new IllegalArgumentException("BOOM!");
                }).recoverWithItem(23).await().indefinitely())
                .withMessageContaining("BOOM!");
    }

    @Test
    public void testNotCalledOnItem() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        Uni.createFrom().item(1)
                .onFailure().recoverWithUni(v -> Uni.createFrom().item(2))
                .subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(1);
    }

    @Test
    public void testCalledOnFailure() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        Uni.createFrom().<Integer> failure(new RuntimeException("boom"))
                .onFailure().recoverWithUni(fail -> Uni.createFrom().item(2))
                .subscribe().withSubscriber(ts);

        ts.assertCompletedSuccessfully().assertItem(2);
    }

    @Test
    public void testCalledOnFailureWithDirectResult() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        Uni.createFrom().<Integer> failure(new RuntimeException("boom"))
                .onFailure().recoverWithItem(fail -> 2)
                .subscribe().withSubscriber(ts);

        ts.assertCompletedSuccessfully().assertItem(2);
    }

    @Test
    public void testWithMappingOfFailure() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        Uni.createFrom().<Integer> failure(new Exception())
                .onFailure().apply(f -> new RuntimeException("boom"))
                .subscribe().withSubscriber(ts);
        ts.assertCompletedWithFailure()
                .assertFailure(RuntimeException.class, "boom");
    }

    @Test
    public void testWithMappingOfFailureAndPredicates() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        Uni.createFrom().<Integer> failure(new IOException())
                .onFailure().apply(t -> new IndexOutOfBoundsException())
                .onFailure(IOException.class).recoverWithUni(Uni.createFrom().item(1))
                .onFailure(IndexOutOfBoundsException.class).recoverWithUni(Uni.createFrom().item(2))
                .subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(2);
    }

}
