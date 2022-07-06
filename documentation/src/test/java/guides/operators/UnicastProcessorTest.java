package guides.operators;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UnicastProcessorTest {

    @Test
    public void test() {
        // <code>
        UnicastProcessor<String> processor = UnicastProcessor.create();
        Multi<String> multi = processor
                .onItem().transform(String::toUpperCase)
                .onFailure().recoverWithItem("d'oh");

        // Create a source of items that does not follow the request protocol
        new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                processor.onNext(Integer.toString(i));
            }
            processor.onComplete();
        }).start();

        // </code>
        AssertSubscriber<String> subscriber = AssertSubscriber.create(Long.MAX_VALUE);
        multi.subscribe().withSubscriber(subscriber)
                .awaitCompletion()
                .run(() -> assertThat(subscriber.getItems()).hasSize(1000));
    }
}
