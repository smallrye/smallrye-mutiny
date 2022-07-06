package tutorials;

import guides.extension.SystemOut;
import guides.extension.SystemOutCaptureExtension;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SuppressWarnings({ "ConstantConditions", "Convert2MethodRef" })
@ExtendWith(SystemOutCaptureExtension.class)
public class CreatingUniTest {

    @Test
    void pipeline(SystemOut out) {
        // <pipeline>
        Uni.createFrom().item(1)
                .onItem().transform(i -> "hello-" + i)
                .onItem().delayIt().by(Duration.ofMillis(100))
                .subscribe().with(System.out::println);
        // </pipeline>
        await().untilAsserted(() ->
                assertThat(out.get()).contains("hello-1")
        );
    }

    @Test
    void subscription(SystemOut out) {
        Uni<Integer> uni = Uni.createFrom().item(1);
        // <subscription>
        Cancellable cancellable = uni
                .subscribe().with(
                        item -> System.out.println(item),
                        failure -> System.out.println("Failed with " + failure));
        // </subscription>
        assertThat(cancellable).isNotNull();
        assertThat(out.get()).contains("1").doesNotContain("Failed");
    }

    @Test
    public void creation() {
        {
            // <simple>
            Uni<Integer> uni = Uni.createFrom().item(1);
            // </simple>
            assertThat(uni.await().indefinitely()).isEqualTo(1);
        }

        {
            // <supplier>
            AtomicInteger counter = new AtomicInteger();
            Uni<Integer> uni = Uni.createFrom().item(() -> counter.getAndIncrement());
            // </supplier>
            assertThat(uni.await().indefinitely()).isEqualTo(0);
            assertThat(uni.await().indefinitely()).isEqualTo(1);
            assertThat(uni.await().indefinitely()).isEqualTo(2);
        }

        {
            // <failed>
            // Pass an exception directly:
            Uni<Integer> failed1 = Uni.createFrom().failure(new Exception("boom"));

            // Pass a supplier called for every subscriber:
            Uni<Integer> failed2 = Uni.createFrom().failure(() -> new Exception("boom"));
            // </failed>

            assertThatThrownBy(() -> failed1.await().indefinitely())
                    .hasMessageContaining("boom");
            assertThatThrownBy(() -> failed2.await().indefinitely())
                    .hasMessageContaining("boom");

        }

        {
            // <null>
            Uni<Void> uni = Uni.createFrom().nullItem();
            // </null>
            assertThat(uni.await().indefinitely()).isNull();
        }

        {
            String result = "hello";
            // <emitter>
            Uni<String> uni = Uni.createFrom().emitter(em -> {
                // When the result is available, emit it
                em.complete(result);
            });
            // </emitter>
            assertThat(uni.await().indefinitely()).isEqualTo("hello");
        }

        {
            CompletionStage<String> stage = CompletableFuture.completedFuture("hello");
            // <cs>
            Uni<String> uni = Uni.createFrom().completionStage(stage);
            // </cs>
            assertThat(uni.await().indefinitely()).isEqualTo("hello");
        }
    }

}
