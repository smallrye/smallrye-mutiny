package io.smallrye.mutiny;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.RepeatedTest;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class BugReproducersTest {

    @RepeatedTest(100)
    public void reproducer_689() {
        // Adapted from https://github.com/smallrye/smallrye-mutiny/issues/689
        AtomicLong src = new AtomicLong();

        AssertSubscriber<Long> sub = Multi.createBy().repeating()
                .supplier(src::incrementAndGet)
                .until(l -> l.equals(10_000L))
                .flatMap(l -> Multi.createFrom().item(l * 2))
                .emitOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        sub.awaitCompletion();
        assertThat(sub.getItems()).hasSize(9_999);
    }
}
