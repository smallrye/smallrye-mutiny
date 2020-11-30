package guides;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("ConstantConditions")
public class CreatingUniTest {

    @Test
    void pipeline() {
        // tag::pipeline[]
        Uni.createFrom().item(1)
                .onItem().transform(i -> "hello-" + i)
                .onItem().delayIt().by(Duration.ofMillis(100))
                .subscribe().with(System.out::println);
        // end::pipeline[]
    }

    @Test
    void subscription() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        // tag::subscription[]
        Cancellable cancellable = uni
                .subscribe().with(
                        item -> System.out.println(item),
                        failure -> System.out.println("Failed with " + failure));
        // end::subscription[]
        assertThat(cancellable).isNotNull();
    }

    @Test
    public void creation() {
        {
            // tag::simple[]
            Uni<Integer> uni = Uni.createFrom().item(1);
            // end::simple[]
            assertThat(uni.await().indefinitely()).isEqualTo(1);
        }

        {
            // tag::supplier[]
            AtomicInteger counter = new AtomicInteger();
            Uni<Integer> uni = Uni.createFrom().item(() -> counter.getAndIncrement());
            // end::supplier[]
            assertThat(uni.await().indefinitely()).isEqualTo(0);
            assertThat(uni.await().indefinitely()).isEqualTo(1);
            assertThat(uni.await().indefinitely()).isEqualTo(2);
        }

        {
            // tag::failed[]
            // Pass an exception directly:
            Uni<Integer> failed1 = Uni.createFrom().failure(new Exception("boom"));

            // Pass a supplier called for every subscriber:
            Uni<Integer> failed2 = Uni.createFrom().failure(() -> new Exception("boom"));
            // end::failed[]

            assertThatThrownBy(() -> failed1.await().indefinitely())
                    .hasMessageContaining("boom");
            assertThatThrownBy(() -> failed2.await().indefinitely())
                    .hasMessageContaining("boom");

        }

        {
            // tag::null[]
            Uni<Void> uni  = Uni.createFrom().nullItem();
            // end::null[]
            assertThat(uni.await().indefinitely()).isNull();
        }

        {
            String result = "hello";
            // tag::emitter[]
            Uni<String> uni = Uni.createFrom().emitter(em -> {
                // When the result is available, emit it
                em.complete(result);
            });
            // end::emitter[]
            assertThat(uni.await().indefinitely()).isEqualTo("hello");
        }

        {
            CompletionStage<String> stage = CompletableFuture.completedFuture("hello");
            // tag::cs[]
            Uni<String> uni = Uni.createFrom().completionStage(stage);
            // end::cs[]
            assertThat(uni.await().indefinitely()).isEqualTo("hello");
        }
    }

}
