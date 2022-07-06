package tutorials;

import guides.extension.SystemOutCaptureExtension;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("Convert2MethodRef")
@ExtendWith(SystemOutCaptureExtension.class)
public class RetryTest {

    @Test
    public void testRetryAtMost() {
        Uni<String> uni = Uni.createFrom().failure(new Exception("boom"));
        Multi<String> multi = Multi.createFrom().failure(new Exception("boom"));
        // <retry-at-most>
        Uni<String> u = uni
                .onFailure().retry().atMost(3);
        Multi<String> m = multi
                .onFailure().retry().atMost(3);
        // </retry-at-most>
        assertThatThrownBy(() -> u.await().indefinitely()).hasMessageContaining("boom");
        assertThatThrownBy(() -> m.collect().asList()
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testRetryWithBackoff() {
        Uni<String> uni = Uni.createFrom().failure(new Exception("boom"));
        // <retry-backoff>
        Uni<String> u = uni
                .onFailure().retry()
                .withBackOff(Duration.ofMillis(100), Duration.ofSeconds(1))
                .atMost(3);
        // </retry-backoff>
        assertThatThrownBy(() -> u.await().indefinitely())
                .getCause() // Expected exception is wrapped in a java.util.concurrent.CompletionException
                .hasMessageContaining("boom")
                .hasSuppressedException(new IllegalStateException("Retries exhausted: 3/3"));
    }

    @Test
    public void testRetryUntil() {
        Uni<String> uni = Uni.createFrom().failure(new Exception("boom"));
        // <retry-until>
        Uni<String> u = uni
                .onFailure().retry()
                .until(f -> shouldWeRetry(f));
        // </retry-until>
        assertThatThrownBy(() -> u.await().indefinitely()).hasMessageContaining("boom");
    }

    private boolean shouldWeRetry(Throwable ignored) {
        return false;
    }
}
