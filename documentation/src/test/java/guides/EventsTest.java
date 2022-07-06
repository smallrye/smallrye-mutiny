package guides;

import guides.extension.SystemOut;
import guides.extension.SystemOutCaptureExtension;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.awaitility.Awaitility.await;

@ExtendWith(SystemOutCaptureExtension.class)
public class EventsTest {

    private void log(String s) {
        System.out.println(s);
    }

    @SuppressWarnings("Convert2MethodRef")
    @Test
    public void test(SystemOut out) {
        // <code>
        Multi<String> source = Multi.createFrom().items("a", "b", "c");
        source
          .onItem() // Called for every item
            .invoke(item -> log("Received item " + item))
          .onFailure() // Called on failure
            .invoke(failure -> log("Failed with " + failure))
          .onCompletion() // Called when the stream completes
            .invoke(() -> log("Completed"))
          .onSubscription() // Called when the upstream is ready
            .invoke(subscription -> log("We are subscribed!"))
          .onCancellation() // Called when the downstream cancels
            .invoke(() -> log("Cancelled :-("))
          .onRequest() // Called on downstream requests
            .invoke(n -> log("Downstream requested " + n + " items"))
          .subscribe()
            .with(item -> log("Subscriber received " + item));
        // </code>

        // <shortcut>
        Multi<String> multi = Multi.createFrom().items("a", "b", "c");
        multi.invoke(item -> System.out.println("Received item " + item));
        // </shortcut>

        // <call-uni>
        multi.call(item -> executeAnAsyncAction(item));
        // </call-uni>

        await().until(() -> out.get().contains("We are subscribed!"));
        await().until(() -> out.get().contains("Downstream requested"));
        await().until(() -> out.get().contains("Received item c"));
        await().until(() -> out.get().contains("Completed"));
    }

    private Uni<?> executeAnAsyncAction(String item) {
        return Uni.createFrom().nullItem();
    }
}
