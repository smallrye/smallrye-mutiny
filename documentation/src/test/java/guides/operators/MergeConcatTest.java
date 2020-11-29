package guides.operators;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({ "unchecked", "Convert2MethodRef" })
public class MergeConcatTest<T> {

    private final Random random = new Random();

    @Test
    public void testMerge() {
        // tag::merge[]
        Multi<T> multi1 = getFirstMulti();
        Multi<T> multi2 = getSecondMulti();

        Multi<T> merged = Multi.createBy().merging().streams(multi1, multi2);
        // end::merge[]

        List<Object> received = new CopyOnWriteArrayList<>();
        merged.subscribe().with(received::add);
        Awaitility.await().until(() -> received.size() == 9);
        assertThat(received).contains(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testConcat() {
        // tag::concat[]
        Multi<T> multi1 = getFirstMulti();
        Multi<T> multi2 = getSecondMulti();

        Multi<T> concatenated = Multi.createBy().concatenating().streams(multi1, multi2);
        // end::concat[]

        List<Object> received = new CopyOnWriteArrayList<>();
        concatenated.subscribe().with(received::add);
        Awaitility.await().until(() -> received.size() == 9);
        assertThat(received).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testMergeTicks() throws InterruptedException {
        // tag::merge-ticks[]
        Multi<String> first = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .onItem().transform(l -> "Stream 1 - " + l);

        Multi<String> second = Multi.createFrom().ticks().every(Duration.ofMillis(15))
                .onItem().transform(l -> "Stream 2 - " + l);

        Multi<String> third = Multi.createFrom().ticks().every(Duration.ofMillis(5))
                .onItem().transform(l -> "Stream 3 - " + l);

        Cancellable cancellable = Multi.createBy().merging().streams(first, second, third)
                .subscribe().with(s -> System.out.println("Got item: " + s));
        // end::merge-ticks[]

        Thread.sleep(50);
        cancellable.cancel();
    }

    @Test
    public void testConcatenateStrings() {
        // tag::concatenate-strings[]
        Multi<String> first = Multi.createFrom().items("A1", "A2", "A3");
        Multi<String> second = Multi.createFrom().items("B1", "B2", "B3");

        Multi.createBy().concatenating().streams(first, second)
                .subscribe().with(item -> System.out.print(item)); // "A1A2A3B1B2B3"

        Multi.createBy().concatenating().streams(second, first)
                .subscribe().with(item -> System.out.print(item)); // "B1B2B3A1A2A3"
        // end::concatenate-strings[]

        assertThat(
                Multi.createBy().concatenating().streams(first, second)
                        .collectItems().in(StringBuffer::new, StringBuffer::append)
                        .await().indefinitely()
        ).isEqualToIgnoringCase("A1A2A3B1B2B3");

        assertThat(
                Multi.createBy().concatenating().streams(second, first)
                        .collectItems().in(StringBuffer::new, StringBuffer::append)
                        .await().indefinitely()
        ).isEqualToIgnoringCase("B1B2B3A1A2A3");

    }

    private Multi<T> getFirstMulti() {
        return Multi.createFrom().items(1, 2, 3, 4, 5, 6)
                .onItem().call(() -> Uni.createFrom().nullItem().onItem().delayIt()
                        .by(Duration.ofMillis(random.nextInt(100) + 1)))
                .onItem().transform(i -> (T) i);
    }

    private Multi<T> getSecondMulti() {
        return Multi.createFrom().items(7, 8, 9)
                .onItem().call(() -> Uni.createFrom().nullItem().onItem().delayIt()
                        .by(Duration.ofMillis(random.nextInt(100) + 1)))
                .onItem().transform(i -> (T) i);
    }
}
