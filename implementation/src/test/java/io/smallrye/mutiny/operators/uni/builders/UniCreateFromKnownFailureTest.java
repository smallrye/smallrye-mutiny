package io.smallrye.mutiny.operators.uni.builders;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class UniCreateFromKnownFailureTest {

    @Test
    public void testCreationWithFailure() {
        assertThatThrownBy(() -> Uni.createFrom().failure(new IOException("io")).await().indefinitely())
                .hasCauseInstanceOf(IOException.class).hasMessageContaining("io");
    }

    @Test
    public void testCreationWithNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().failure((Throwable) null));
    }

    @Test
    public void testCancellationAfterEmission() {
        UniAssertSubscriber<String> hello = Uni.createFrom().<String> failure(new IOException("boom"))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        hello.cancel();
        hello.assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCancellationBeforeEmission() {
        UniAssertSubscriber<String> subscriber = new UniAssertSubscriber<>(true);
        Uni.createFrom().<String> failure(new IOException("boom"))
                .subscribe().withSubscriber(subscriber);
        subscriber.assertNotTerminated();
    }

}
