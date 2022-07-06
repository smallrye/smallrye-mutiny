package guides.operators;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class PaginationTest {

    @SuppressWarnings("Convert2MethodRef")
    @Test
    public void test() {
        // <code>
        PaginatedApi api = new PaginatedApi();

        Multi<String> stream = Multi.createBy().repeating()
                .completionStage(
                        () -> new AtomicInteger(),
                        state -> api.getPage(state.getAndIncrement()))
                .until(list -> list.isEmpty())
                .onItem().disjoint();
        // </code>
        stream.subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems("a", "b", "c", "d", "e", "f", "g", "h");

    }

    @SuppressWarnings("Convert2MethodRef")
    @Test
    public void test2() {
        // <code2>
        PaginatedApi api = new PaginatedApi();

        Multi<Page> stream = Multi.createBy().repeating()
                .uni(
                        () -> new AtomicInteger(),
                        state -> api.retrieve(state.getAndIncrement()))
                .whilst(page -> page.hasNext());
        // </code2>
        AssertSubscriber<Page> subscriber = stream.subscribe()
                .withSubscriber(AssertSubscriber.create(10))
                .assertCompleted();

        assertThat(subscriber.getItems()).hasSize(3);

    }

    private static class Page {
        final boolean hasNext;

        private Page(boolean hasNext) {
            this.hasNext = hasNext;
        }

        public boolean hasNext() {
            return hasNext;
        }
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


        Uni<Page> retrieve(int page) {
            if (page == 2) {
                return Uni.createFrom().item(new Page(false));
            }
            return Uni.createFrom().item(new Page(true));
        }
    }

}
