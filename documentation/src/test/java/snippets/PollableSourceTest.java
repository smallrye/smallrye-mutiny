package snippets;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;

public class PollableSourceTest {

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Test
    public void test() {
        // tag::code[]
        PollableDataSource source = new PollableDataSource();
        // First creates a uni that emit the polled item. Because `poll` blocks, let's use a specific executor
        Uni<String> pollItemFromSource = Uni.createFrom().item(source::poll).subscribeOn(executor);
        // To get the stream of items, just repeat the uni indefinitely
        Multi<String> stream = pollItemFromSource.repeat().indefinitely();

        Cancellable cancellable = stream.subscribe().with(item -> System.out.println("Polled item: " + item));
        // end::code[]
        await().until(() -> source.counter.get() >= 4);
        // tag::code[]
        // ... later ..
        // when you don't want the items anymore, cancel the subscription and close the source if needed.
        cancellable.cancel();
        source.close();
        // end::code[]
    }

    private class PollableDataSource {

        private final AtomicInteger counter = new AtomicInteger();

        String poll() {
            block();
            return Integer.toString(counter.getAndIncrement());
        }

        private void block() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public void close() {
            // do nothing.
        }
    }

}
