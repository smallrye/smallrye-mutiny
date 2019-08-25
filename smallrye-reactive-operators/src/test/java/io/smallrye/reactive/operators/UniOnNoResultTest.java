package io.smallrye.reactive.operators;

import io.smallrye.reactive.TimeoutException;
import io.smallrye.reactive.Uni;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class UniOnNoResultTest {

    @Test
    public void testResultWhenTimeoutIsNotReached() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        Uni.createFrom().item(1)
                .onNoItem().after(Duration.ofMillis(10)).recoverWithUni(Uni.createFrom().nothing())
                .subscribe().withSubscriber(ts);

        ts.await().assertCompletedSuccessfully().assertItem(1);
    }

    @Test
    public void testTimeout() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        Uni.createFrom().item(1)
                .onItem().delayIt().by(Duration.ofMillis(10))
                .onNoItem().after(Duration.ofMillis(1)).fail()
                .subscribe().withSubscriber(ts);

        ts.await().assertCompletedWithFailure();
        assertThat(ts.getFailure()).isInstanceOf(TimeoutException.class);

    }

    @Test
    public void testRecoverWithResult() {
        UniAssertSubscriber<Integer> ts = Uni.createFrom().<Integer>nothing()
                .onNoItem().after(Duration.ofMillis(10)).recoverWithResult(5)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        ts.await().assertItem(5);
    }

    @Test
    public void testRecoverWithResultSupplier() {
        UniAssertSubscriber<Integer> ts = Uni.createFrom().<Integer>nothing()
                .onNoItem().after(Duration.ofMillis(10)).recoverWithResult(() -> 23)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        ts.await().assertItem(23);
    }

    @Test
    public void testRecoverWithSwitchToUni() {
        UniAssertSubscriber<Integer> ts = Uni.createFrom().<Integer>nothing()
                .onNoItem().after(Duration.ofMillis(10)).recoverWithUni(() -> Uni.createFrom().item(15))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        ts.await().assertItem(15);
    }

    @Test
    public void testFailingWithAnotherException() {
        UniAssertSubscriber<Integer> ts = Uni.createFrom().<Integer>nothing()
                .onNoItem().after(Duration.ofMillis(10)).failWith(new IOException("boom"))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        ts.await().assertFailure(IOException.class, "boom");
    }

    @Test
    public void testDurationValidity() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Uni.createFrom().item(1).onNoItem().after(null))
                .withMessageContaining("timeout");

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Uni.createFrom().item(1).onNoItem().after(Duration.ofMillis(0)))
                .withMessageContaining("timeout");

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Uni.createFrom().item(1).onNoItem().after(Duration.ofMillis(-1)))
                .withMessageContaining("timeout");
    }

}
