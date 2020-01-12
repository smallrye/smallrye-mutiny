package snippets;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.test.MultiAssertSubscriber;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;

public class PaginationTest {

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @SuppressWarnings("Convert2MethodRef")
    @Test
    public void test() {
        // tag::code[]
        PaginatedApi api = new PaginatedApi();

        Multi<String> stream = Multi.createBy().repeating()
                    .completionStage(
                            () -> new AtomicInteger(),
                            state -> api.getPage(state.getAndIncrement()))
                    .until(list -> list.isEmpty())
                .onItem().disjoint();
        // end::code[]
        stream.subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived("a", "b", "c", "d", "e", "f", "g", "h");

    }

    private class PaginatedApi {

        Map<Integer, List<String>> pages = new LinkedHashMap<>();

        public PaginatedApi() {
            pages.put(0, Arrays.asList("a", "b", "c"));
            pages.put(1, Arrays.asList("d", "e", "f"));
            pages.put(2, Arrays.asList("g", "h"));
            pages.put(3, Collections.emptyList());
        }

        CompletionStage<List<String>> getPage(int page) {
            List<String> strings = pages.get(page);
            if (strings == null) {
                strings = Collections.emptyList();
            }
            return CompletableFuture.completedFuture(strings);
        }
    }

}
