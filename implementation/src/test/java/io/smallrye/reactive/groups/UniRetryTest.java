package io.smallrye.reactive.groups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.junit.Test;

import io.smallrye.reactive.Uni;

public class UniRetryTest {
    @Test
    public void testFailureWithPredicateException() {
        AtomicLong counter = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Uni.createFrom().failure(new Throwable("Cause failure"))
                .onFailure(new ThrowablePredicate()).retry().atMost(2)
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);

        await().until(() -> counter.intValue() == 0);
        assertThat(failure.get()).isNotNull();
    }

    @Test
    public void testFailureWithPredicateFailure() {
        AtomicLong counter = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Uni.createFrom().failure(new Throwable("Cause failure"))
                .onFailure((t) -> false).retry().atMost(2)
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);

        await().until(() -> counter.intValue() == 0);
        assertThat(failure.get()).isNotNull();
    }

    class ThrowablePredicate implements Predicate<Throwable> {
        @Override
        public boolean test(Throwable throwable) {
            throw new RuntimeException();
        }
    }

}
