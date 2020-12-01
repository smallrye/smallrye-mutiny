package guides;

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
        // tag::retry-at-most[]
        Uni<String> u = uni
                .onFailure().retry().atMost(3);
        Multi<String> m = multi
                .onFailure().retry().atMost(3);
        // end::retry-at-most[]
        assertThatThrownBy(() -> u.await().indefinitely()).hasMessageContaining("boom");
        assertThatThrownBy(() -> m.collectItems().asList()
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testRetryWithBackoff() {
        Uni<String> uni = Uni.createFrom().failure(new Exception("boom"));
        // tag::retry-backoff[]
        Uni<String> u = uni
                .onFailure().retry()
                .withBackOff(Duration.ofMillis(100), Duration.ofSeconds(1))
                .atMost(3);
        // end::retry-backoff[]
        assertThatThrownBy(() -> u.await().indefinitely()).hasMessageContaining("3/3");
    }

    @Test
    public void testRetryUntil() {
        Uni<String> uni = Uni.createFrom().failure(new Exception("boom"));
        // tag::retry-until[]
        Uni<String> u = uni
                .onFailure().retry()
                .until(f -> shouldWeRetry(f));
        // end::retry-until[]
        assertThatThrownBy(() -> u.await().indefinitely()).hasMessageContaining("boom");
    }

    private boolean shouldWeRetry(Throwable ignored) {
        return false;
    }
}
