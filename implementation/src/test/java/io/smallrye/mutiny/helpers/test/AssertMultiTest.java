package io.smallrye.mutiny.helpers.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
class AssertMultiTest {

    @Test
    void expectNextSingleValue() {
        AssertMulti.create(Multi.createFrom().items(1, 2, 3))
                .expectNext(1)
                .expectNext(2)
                .expectNext(3)
                .expectComplete()
                .verify();
    }

    @Test
    void expectNextVarargs() {
        AssertMulti.create(Multi.createFrom().items(1, 2, 3))
                .expectNext(1, 2, 3)
                .expectComplete()
                .verify();
    }

    @Test
    void expectNextWrongValue() {
        assertThatThrownBy(() -> AssertMulti.create(Multi.createFrom().items(1, 2, 3))
                .expectNext(1, 99, 3)
                .expectComplete()
                .verify())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Step 0")
                .hasMessageContaining("99");
    }

    @Test
    void expectNextMatches() {
        AssertMulti.create(Multi.createFrom().items(1, 2, 3))
                .expectNextMatches(i -> i == 1, "equals 1")
                .expectNextMatches(i -> i > 1, "greater than 1")
                .expectNextMatches(i -> i == 3, "equals 3")
                .expectComplete()
                .verify();
    }

    @Test
    void expectNextMatchesFails() {
        assertThatThrownBy(() -> AssertMulti.create(Multi.createFrom().items(1, 2, 3))
                .expectNextMatches(i -> i > 10, "greater than 10")
                .expectComplete()
                .verify())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Step 0")
                .hasMessageContaining("expectNextMatches: greater than 10")
                .hasMessageContaining("did not match predicate");
    }

    @Test
    void expectNextCount() {
        AssertMulti.create(Multi.createFrom().items(1, 2, 3, 4, 5))
                .expectNextCount(3)
                .expectNext(4, 5)
                .expectComplete()
                .verify();
    }

    @Test
    void consumeNext() {
        AssertMulti.create(Multi.createFrom().items(1, 2, 3))
                .consumeNext(item -> assertThat(item).isEqualTo(1))
                .consumeNext(item -> assertThat(item).isEqualTo(2))
                .expectNext(3)
                .expectComplete()
                .verify();
    }

    @Test
    void consumeNextFails() {
        assertThatThrownBy(() -> AssertMulti.create(Multi.createFrom().items(1, 2, 3))
                .consumeNext(item -> {
                    throw new AssertionError("bad item");
                })
                .expectComplete()
                .verify())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Step 0")
                .hasMessageContaining("bad item");
    }

    @Test
    void consumeNextItems() {
        AssertMulti.create(Multi.createFrom().items(1, 2, 3, 4))
                .consumeNextItems(3, items -> assertThat(items).containsExactly(1, 2, 3))
                .expectNext(4)
                .expectComplete()
                .verify();
    }

    @Test
    void thenRequest() {
        AssertMulti.create(Multi.createFrom().items(1, 2, 3))
                .thenRequest(10)
                .expectNext(1, 2, 3)
                .expectComplete()
                .verify();
    }

    @Test
    void thenCancel() {
        AssertMulti.create(Multi.createFrom().items(1, 2, 3))
                .expectNext(1)
                .thenCancel()
                .verify();
    }

    @Test
    void expectComplete() {
        AssertMulti.create(Multi.createFrom().empty())
                .expectComplete()
                .verify();
    }

    @Test
    void expectFailure() {
        AssertMulti.create(Multi.createFrom().failure(new IOException("boom")))
                .expectFailure()
                .verify();
    }

    @Test
    void expectFailureWithType() {
        AssertMulti.create(Multi.createFrom().failure(new IOException("boom")))
                .expectFailure(IOException.class)
                .verify();
    }

    @Test
    void expectFailureWithTypeAndMessage() {
        AssertMulti.create(Multi.createFrom().failure(new IOException("boom")))
                .expectFailure(IOException.class, "boom")
                .verify();
    }

    @Test
    void expectFailureWithValidator() {
        AssertMulti.create(Multi.createFrom().failure(new IOException("boom")))
                .expectFailure(t -> assertThat(t).isInstanceOf(IOException.class).hasMessage("boom"))
                .verify();
    }

    @Test
    void expectFailureWrongType() {
        assertThatThrownBy(() -> AssertMulti.create(Multi.createFrom().failure(new IOException("boom")))
                .expectFailure(IllegalStateException.class)
                .verify())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Step 0")
                .hasMessageContaining("expectFailure");
    }

    @Test
    void wrongTerminalExpectsCompleteGetsFailure() {
        assertThatThrownBy(() -> AssertMulti.create(Multi.createFrom().failure(new IOException("boom")))
                .expectComplete()
                .verify())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure");
    }

    @Test
    void wrongTerminalExpectsFailureGetsComplete() {
        assertThatThrownBy(() -> AssertMulti.create(Multi.createFrom().items(1))
                .expectNext(1)
                .expectFailure()
                .verify())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("completion");
    }

    @Test
    void stepIndexInErrorMessage() {
        assertThatThrownBy(() -> AssertMulti.create(Multi.createFrom().items(1, 2, 3))
                .expectNext(1)
                .expectNext(2)
                .expectNextMatches(i -> i > 100, "greater than 100")
                .expectComplete()
                .verify())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Step 2");
    }

    @Test
    void emptyStream() {
        AssertMulti.create(Multi.createFrom().empty())
                .expectComplete()
                .verify();
    }

    @Test
    void singleItemStream() {
        AssertMulti.create(Multi.createFrom().item(42))
                .expectNext(42)
                .expectComplete()
                .verify();
    }

    @Test
    void withContext() {
        Context ctx = Context.of("key", "value");
        AssertMulti.create(
                Multi.createFrom().context(c -> Multi.createFrom().item(c.<String> get("key"))),
                ctx)
                .expectNext("value")
                .expectComplete()
                .verify();
    }

    @Test
    void noStepsThrows() {
        assertThatThrownBy(() -> AssertMulti.create(Multi.createFrom().empty())
                .verify())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No steps");
    }

    @Test
    void lastStepNotTerminalThrows() {
        assertThatThrownBy(() -> AssertMulti.create(Multi.createFrom().items(1, 2))
                .expectNext(1)
                .verify())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("terminal");
    }

    @Test
    void verifyWithTimeout() {
        AssertMulti.create(Multi.createFrom().items(1, 2, 3))
                .expectNext(1, 2, 3)
                .expectComplete()
                .verify(Duration.ofSeconds(5));
    }
}
