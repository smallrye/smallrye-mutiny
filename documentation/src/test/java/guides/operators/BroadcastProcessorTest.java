package guides.operators;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import org.junit.jupiter.api.Test;

public class BroadcastProcessorTest {

    @Test
    public void test() {
        // <code>
        BroadcastProcessor<String> processor = BroadcastProcessor.create();
        Multi<String> multi = processor
                .onItem().transform(String::toUpperCase)
                .onFailure().recoverWithItem("d'oh");

        new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                processor.onNext(Integer.toString(i));
            }
            processor.onComplete();
        }).start();

        // Subscribers can subscribe at any time.
        // They will only receive items emitted after their subscription.
        // If the source is already terminated (by a completion or a failure signal)
        // the subscriber receives this signal.

        // </code>
        AssertSubscriber<String> subscriber = AssertSubscriber.create(Long.MAX_VALUE);
        multi.subscribe().withSubscriber(subscriber)
                .awaitCompletion();
    }
}
