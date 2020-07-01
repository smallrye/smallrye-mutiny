package snippets;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class UnicastProcessorTest {

    @Test
    public void test() {
        // tag::code[]
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

        // end::code[]
        MultiAssertSubscriber<String> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        multi.subscribe().withSubscriber(subscriber)
                .await()
                .run(() -> assertThat(subscriber.items()).hasSize(1000));
    }
}
