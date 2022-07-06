package tutorials;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Duration;

public class ObserveTest {

    @Test
    public void test() {
        Uni<String> uni = Uni.createFrom().item("hello");
        Multi<String> multi = Multi.createFrom().items("a", "b", "c");
        // <invoke>
        Uni<String> u = uni.onItem()
            .invoke(i -> System.out.println("Received item: " + i));

        Multi<String> m = multi.onItem()
            .invoke(i -> System.out.println("Received item: " + i));
        // </invoke>

        // <call>
        multi
            .onItem().call(i ->
                Uni.createFrom().voidItem()
                    .onItem().delayIt().by(Duration.ofSeconds(1)
            )
        );
        // </call>

        MyResource resource = new MyResource();
        // <close>
        multi
            .onCompletion().call(() -> resource.close());
        // </close>
    }

    @Test
    public void all() {
        Multi<String> multi = Multi.createFrom().items("a", "b", "c");
        // <invoke-all>
        multi
            .onSubscription()
                .invoke(() -> System.out.println("⬇️ Subscribed"))
            .onItem()
                .invoke(i -> System.out.println("⬇️ Received item: " + i))
            .onFailure()
                .invoke(f -> System.out.println("⬇️ Failed with " + f))
            .onCompletion()
                .invoke(() -> System.out.println("⬇️ Completed"))
            .onCancellation()
                .invoke(() -> System.out.println("⬆️ Cancelled"))
            .onRequest()
                .invoke(l -> System.out.println("⬆️ Requested: " + l));
        // </invoke-all>

    }

    static class MyResource {
        Uni<Void> close() {
            return Uni.createFrom().voidItem();
        }
    }
}
