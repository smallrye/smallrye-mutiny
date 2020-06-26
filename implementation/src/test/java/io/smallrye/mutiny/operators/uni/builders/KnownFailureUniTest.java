package io.smallrye.mutiny.operators.uni.builders;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.UniAssertSubscriber;

public class KnownFailureUniTest {

    @Test
    public void testCreationWithFailure() {
        assertThatThrownBy(() -> Uni.createFrom().failure(new IOException("io")).await().indefinitely())
                .hasCauseInstanceOf(IOException.class).hasMessageContaining("io");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreationWithNull() {
        Uni.createFrom().failure((Throwable) null);
    }

    @Test
    public void testCancellationAfterEmission() {
        UniAssertSubscriber<String> hello = Uni.createFrom().<String> failure(new IOException("boom"))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        hello.cancel();
        hello.assertFailure(IOException.class, "boom");
    }

    @Test
    public void testCancellationBeforeEmission() {
        UniAssertSubscriber<String> subscriber = new UniAssertSubscriber<>(true);
        Uni.createFrom().<String> failure(new IOException("boom"))
                .subscribe().withSubscriber(subscriber);
        subscriber.assertNotCompleted();
    }

}
