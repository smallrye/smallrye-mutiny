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
        AssertSubscriber<Integer> ts = AssertSubscriber.create();

        Uni.createFrom().result(1)
                .onNoResult().after(Duration.ofMillis(10)).recoverWithUni(Uni.createFrom().nothing())
                .subscribe().withSubscriber(ts);

        ts.await().assertCompletedSuccessfully().assertResult(1);
    }

    @Test
    public void testTimeout() {
        AssertSubscriber<Integer> ts = AssertSubscriber.create();

        Uni.createFrom().result(1)
                .onResult().delayIt().by(Duration.ofMillis(10))
                .onNoResult().after(Duration.ofMillis(1)).fail()
                .subscribe().withSubscriber(ts);

        ts.await().assertCompletedWithFailure();
        assertThat(ts.getFailure()).isInstanceOf(TimeoutException.class);

    }

    @Test
    public void testRecoverWithResult() {
        AssertSubscriber<Integer> ts = Uni.createFrom().<Integer>nothing()
                .onNoResult().after(Duration.ofMillis(10)).recoverWithResult(5)
                .subscribe().withSubscriber(AssertSubscriber.create());
        ts.await().assertResult(5);
    }

    @Test
    public void testRecoverWithResultSupplier() {
        AssertSubscriber<Integer> ts = Uni.createFrom().<Integer>nothing()
                .onNoResult().after(Duration.ofMillis(10)).recoverWithResult(() -> 23)
                .subscribe().withSubscriber(AssertSubscriber.create());
        ts.await().assertResult(23);
    }

    @Test
    public void testRecoverWithSwitchToUni() {
        AssertSubscriber<Integer> ts = Uni.createFrom().<Integer>nothing()
                .onNoResult().after(Duration.ofMillis(10)).recoverWithUni(() -> Uni.createFrom().result(15))
                .subscribe().withSubscriber(AssertSubscriber.create());
        ts.await().assertResult(15);
    }

    @Test
    public void testFailingWithAnotherException() {
        AssertSubscriber<Integer> ts = Uni.createFrom().<Integer>nothing()
                .onNoResult().after(Duration.ofMillis(10)).failWith(new IOException("boom"))
                .subscribe().withSubscriber(AssertSubscriber.create());
        ts.await().assertFailure(IOException.class, "boom");
    }

    @Test
    public void testDurationValidity() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Uni.createFrom().result(1).onNoResult().after(null))
                .withMessageContaining("timeout");

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Uni.createFrom().result(1).onNoResult().after(Duration.ofMillis(0)))
                .withMessageContaining("timeout");

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Uni.createFrom().result(1).onNoResult().after(Duration.ofMillis(-1)))
                .withMessageContaining("timeout");
    }


}
