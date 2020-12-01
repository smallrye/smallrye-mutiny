package guides;

import guides.extension.SystemOut;
import guides.extension.SystemOutCaptureExtension;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.BlockingIterable;
import io.smallrye.mutiny.subscription.Cancellable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SuppressWarnings({ "Convert2MethodRef" })
@ExtendWith(SystemOutCaptureExtension.class)
public class CreatingMultiTest {

    @Test
    void pipeline(SystemOut out) {
        // tag::pipeline[]
        Multi.createFrom().items(1, 2, 3, 4, 5)
                .onItem().transform(i -> i * 2)
                .transform().byTakingFirstItems(3)
                .onFailure().recoverWithItem(0)
                .subscribe().with(System.out::println);
        // end::pipeline[]
        assertThat(out.get()).contains("2", "4", "6");
    }

    @Test
    void subscription(SystemOut out) {
        Multi<Integer> multi  = Multi.createFrom().item(1);
        // tag::subscription[]
        Cancellable cancellable = multi
                .subscribe().with(
                        item -> System.out.println(item),
                        failure -> System.out.println("Failed with " + failure),
                        () -> System.out.println("Completed"));
        // end::subscription[]
        assertThat(cancellable).isNotNull();
        assertThat(out.get()).contains("1", "Completed").doesNotContain("Failed");
    }

    @Test
    public void creation() {
        {
            // tag::simple[]
            Multi<Integer> multiFromItems = Multi.createFrom().items(1, 2, 3, 4);
            Multi<Integer> multiFromIterable = Multi.createFrom().iterable(Arrays.asList(1, 2, 3, 4, 5));
            // end::simple[]
            assertThat(multiFromItems.collectItems().asList().await().indefinitely()).containsExactly(1, 2, 3, 4);
            assertThat(multiFromIterable.collectItems().asList().await().indefinitely()).containsExactly(1, 2, 3, 4, 5);
        }

        {
            // tag::supplier[]
            AtomicInteger counter = new AtomicInteger();
            Multi<Integer> multi = Multi.createFrom().items(() ->
                    IntStream.range(counter.getAndIncrement(), counter.get() * 2).boxed());
            // end::supplier[]
            assertThat(multi.collectItems().asList().await().indefinitely()).containsExactly(0, 1);
            assertThat(multi.collectItems().asList().await().indefinitely()).containsExactly(1, 2, 3);
            assertThat(multi.collectItems().asList().await().indefinitely()).containsExactly(2, 3, 4, 5);
        }

        {
            // tag::failed[]
            // Pass an exception directly:
            Multi<Integer> failed1 = Multi.createFrom().failure(new Exception("boom"));

            // Pass a supplier called for every subscriber:
            Multi<Integer> failed2 = Multi.createFrom().failure(() -> new Exception("boom"));
            // end::failed[]

            assertThatThrownBy(() -> failed1.toUni().await().indefinitely())
                    .hasMessageContaining("boom");
            assertThatThrownBy(() -> failed2.toUni().await().indefinitely())
                    .hasMessageContaining("boom");

        }

        {
            // tag::empty[]
            Multi<String> multi = Multi.createFrom().empty();
            // end::empty[]
            assertThat(multi.toUni().await().indefinitely()).isNull();
        }

        {
            // tag::emitter[]
            Multi<Integer> multi = Multi.createFrom().emitter(em -> {
                em.emit(1);
                em.emit(2);
                em.emit(3);
                em.complete();
            });
            // end::emitter[]
            assertThat(multi.collectItems().asList().await().indefinitely()).containsExactly(1, 2, 3);
        }

        {
            // tag::ticks[]
            Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofMillis(100));
            // end::ticks[]
            BlockingIterable<Long> longs = ticks
                    .transform().byTakingFirstItems(3)
                    .subscribe().asIterable();
            await().until(() -> longs.stream().count() == 3);
        }
    }

}
