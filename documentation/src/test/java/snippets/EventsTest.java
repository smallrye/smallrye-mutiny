package snippets;

import org.junit.Test;

import io.smallrye.mutiny.Multi;

public class EventsTest {

    @Test
    public void test() {
        // tag::code[]
        Multi<String> source = Multi.createFrom().items("a", "b", "c");

        source
                .onItem().invoke(item -> System.out.println("Received item " + item))
                .onFailure().invoke(failure -> System.out.println("Failed with " + failure.getMessage()))
                .onCompletion().invoke(() -> System.out.println("Completed"))
                .on().subscribed(subscription -> System.out.println("We are subscribed!"))

                .on().cancellation(() -> System.out.println("Downstream has cancelled the interaction"))
                .on().request(n -> System.out.println("Downstream requested " + n + " items"))
                .subscribe().with(item -> {
                });
        // end::code[]

    }
}
