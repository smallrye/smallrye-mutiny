package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class UniCreateFromDeferredSupplierTest {

    @Test
    public void testWithMultipleSubscriptions() {
        AtomicInteger counter = new AtomicInteger();
        Uni<Integer> s =  Uni.createFrom().deferred(() -> Uni.createFrom().item(counter.incrementAndGet()));

        for (int i = 1; i < 100; i++) {
            assertThat(s.await().indefinitely()).isEqualTo(i);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithNull() {
         Uni.createFrom().deferred(null);
    }

    @Test
    public void testWithASupplierProducingNull() {
        Uni<Integer> s =  Uni.createFrom().deferred(() -> null);
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        s.subscribe().withSubscriber(subscriber);
        subscriber.assertFailure(NullPointerException.class, "");
    }

    @Test
    public void testWithASupplierThrowingAnException() {
        Uni<Integer> s =  Uni.createFrom().deferred(() -> {
            throw new IllegalStateException("boom");
        });
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        s.subscribe().withSubscriber(subscriber);
        subscriber.assertFailure(IllegalStateException.class, "boom");
    }
}
