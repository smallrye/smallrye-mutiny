package snippets;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;

public class EventsTest {

    private void log(String s) {
        System.out.println(s);
    }

    @Test
    public void test() {
        // tag::code[]
        Multi<String> source = Multi.createFrom().items("a", "b", "c");
        source
          .onItem() // Called for every item
            .invoke(item -> log("Received item " + item))
          .onFailure() // Called on failure
            .invoke(failure -> log("Failed with " + failure))
          .onCompletion() // Called when the stream completes
            .invoke(() -> log("Completed"))
          .onSubscribe() // Called the the upstream is ready
            .invoke(subscription -> log("We are subscribed!"))
          .onCancellation() // Called when the downstream cancels
            .invoke(() -> log("Cancelled :-("))
          .onRequest() // Call on downstream requests
            .invoke(n -> log("Downstream requested " + n + " items"))
          .subscribe()
            .with(item -> log("Subscriber received " + item));
        // end::code[]

        // tag::shortcut[]
        Multi<String> multi = Multi.createFrom().items("a", "b", "c");
        multi.invoke(item -> System.out.println("Received item " + item));
        // end::shortcut[]

        // tag::call-uni[]
        multi.call(item -> executeAnAsyncAction(item));
        // end::call-uni[]

    }

    private Uni<?> executeAnAsyncAction(String item) {
        return Uni.createFrom().nullItem();
    }
}
