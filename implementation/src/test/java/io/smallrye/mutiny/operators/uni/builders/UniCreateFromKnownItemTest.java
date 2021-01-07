package io.smallrye.mutiny.operators.uni.builders;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class UniCreateFromKnownItemTest {

    @Test
    public void testCreationWithItem() {
        assertThat(Uni.createFrom().item("hello").await().indefinitely()).isEqualTo("hello");
    }

    @Test
    public void testCreationWithNull() {
        assertThat(Uni.createFrom().item((String) null).await().indefinitely()).isNull();
    }

    @Test
    public void testCancellationAfterEmission() {
        UniAssertSubscriber<String> hello = Uni.createFrom().item("hello")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        hello.cancel();
        hello.assertCompleted().assertItem("hello");
    }

    @Test
    public void testCancellationBeforeEmission() {
        UniAssertSubscriber<String> subscriber = new UniAssertSubscriber<>(true);
        Uni.createFrom().item("hello")
                .subscribe().withSubscriber(subscriber);
        subscriber.assertNotTerminated();
    }

}
