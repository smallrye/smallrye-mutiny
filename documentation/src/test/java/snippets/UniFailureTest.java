package snippets;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UniFailureTest {

    @Test
    public void test() {
        Uni<String> uni = Uni.createFrom().failure(new IOException("boom"));
        // tag::code[]

        // Transform a failure in another type of failure
        CompletableFuture<String> res0 = uni.onFailure().apply(failure -> new MyBusinessException("oh no!"))
                .subscribeAsCompletionStage();

        // Recover with an item
        String res1 = uni
                .onFailure().recoverWithItem("hello")
                .await().indefinitely();

        // Filter the type of failure
        String res2 = uni
                .onFailure(IllegalArgumentException.class).recoverWithItem("bonjour")
                .onFailure(IOException.class).recoverWithItem("hello")
                .await().indefinitely();

        // Recover recover with an uni
        String res3 = uni
                .onFailure().recoverWithUni(() -> Uni.createFrom().item("fallback"))
                .await().indefinitely();

        // Retry at most twice
        CompletableFuture<String> res4 = uni
                .onFailure().retry().atMost(2)
                .subscribeAsCompletionStage();

        // end::code[]

        assertThatThrownBy(res0::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(MyBusinessException.class);
        assertThat(res1).isEqualTo("hello");
        assertThat(res2).isEqualTo("hello");
        assertThat(res3).isEqualTo("fallback");
        assertThatThrownBy(res4::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void retry() {
        AtomicInteger attempt = new AtomicInteger();
        Uni<String> uni = Uni.createFrom().<String>emitter(e -> {
            int i = attempt.getAndIncrement();
            if (i < 3) {
                e.fail(new MyBusinessException("boom"));
            } else {
                e.complete("OK-" + i);
            }
        });
        // tag::code-retry[]
        Uni<String> uniWithRetry = uni.onFailure().retry().atMost(4);
        Uni<String> uniWithRetryAndBackoff = uni.onFailure().retry()
                .withBackOff(Duration.ofMillis(10), Duration.ofMinutes(1))
                .atMost(5);
        // end::code-retry[]

        assertThat(uniWithRetry.await().indefinitely()).isEqualTo("OK-" + 3);
        assertThat(uniWithRetryAndBackoff.await().indefinitely()).isEqualTo("OK-" + 4);
    }

    private class MyBusinessException extends Exception {

        MyBusinessException(String s) {
            super(s);
        }
    }
}
