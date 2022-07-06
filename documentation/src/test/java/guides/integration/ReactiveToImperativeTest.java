package guides.integration;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
public class ReactiveToImperativeTest<T> {

    @Test
    public void testAwait() {
        Uni<T> uni = (Uni<T>) Uni.createFrom().item(() -> 1);

        // <await>
        T t = uni.await().indefinitely();
        // </await>

        assertThat(t).isEqualTo(1);
    }

    @Test
    public void testAwaitAtMost() {
        Uni<T> uni = (Uni<T>) Uni.createFrom().item(() -> 1);

        // <atMost>
        T t = uni.await().atMost(Duration.ofSeconds(1));
        // </atMost>

        assertThat(t).isEqualTo(1);
    }

    @Test
    public void test() {
        Multi<T> multi = (Multi<T>) Multi.createFrom().items(1, 2, 3);

        // <iterable>
        Iterable<T> iterable = multi.subscribe().asIterable();
        for (T item : iterable) {
            doSomethingWithItem(item);
        }
        // </iterable>

        // <stream>
        Stream<T> stream = multi.subscribe().asStream();
        stream.forEach(this::doSomethingWithItem);
        // </stream>
    }

    private void doSomethingWithItem(T item) {
        assertThat(item).isIn(1, 2, 3);
    }
}
