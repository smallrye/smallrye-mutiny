package guides.operators;

import guides.extension.SystemOut;
import guides.extension.SystemOutCaptureExtension;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple;
import io.smallrye.mutiny.tuples.Tuple2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SuppressWarnings({ "unchecked", "Convert2MethodRef" })
@ExtendWith(SystemOutCaptureExtension.class)
public class CombiningItemsTest<A, B> {

    @Test
    public void testWithUni(SystemOut out) throws InterruptedException {
        // <invocations>
        Uni<Response> uniA = invokeHttpServiceA();
        Uni<Response> uniB = invokeHttpServiceB();
        // </invocations>

        // <combination>
        Uni<Tuple2<Response, Response>> responses = Uni.combine()
                .all().unis(uniA, uniB).asTuple();
        // </combination>

        // <subscription>
        Uni.combine().all().unis(uniA, uniB).asTuple()
                .subscribe().with(tuple -> {
            System.out.println("Response from A: " + tuple.getItem1());
            System.out.println("Response from B: " + tuple.getItem2());
        });
        // </subscription>

        await().until(() -> out.get().contains("Response from A: Response{content='A'}"));
        await().until(() -> out.get().contains("Response from B: Response{content='B'}"));

        // <combined-with>
        Uni<Map<String, Response>> uni = Uni.combine()
                .all().unis(uniA, uniB).combinedWith(
                        listOfResponses -> {
                            Map<String, Response> map = new LinkedHashMap<>();
                            map.put("A", (Response) listOfResponses.get(0));
                            map.put("B", (Response) listOfResponses.get(1));
                            return map;
                        }
                );
        // </combined-with>

        assertThat(uni.await().indefinitely()).containsKeys("A", "B");

        Thread.sleep(100);
    }

    @Test
    public void testWithMulti() throws InterruptedException {
        Multi<A> multiA = getMultiA();
        Multi<B> multiB = getMultiB();

        // <combine-multi>
        Multi<Tuple2<A, B>> combined = Multi.createBy().combining()
                .streams(multiA, multiB).asTuple();
        // </combine-multi>

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger i = new AtomicInteger();
        combined.subscribe().with(
                tuple -> {
                    int v = i.incrementAndGet();
                    assertThat(tuple.getItem1()).isEqualTo("a" + v);
                    assertThat(tuple.getItem2()).isEqualTo("b" + v);
                },
                () -> latch.countDown()
        );

        assertThat(latch.await(10, TimeUnit.MILLISECONDS)).isTrue();

        // <combine-multi-with>
        Multi.createBy().combining()
                .streams(multiA, multiB).using(list -> combineItems(list))
                .subscribe().with(x -> {
                    // do something with the combined items
                });
        // </combine-multi-with>
    }

    @Test
    public void testCombineLast() {
        Multi<A> multiA = getMultiA();
        Multi<B> multiB = getMultiB();

        // <combine-last>
        Multi<Tuple2<A, B>> multi1 = Multi.createBy().combining()
                .streams(multiA, multiB)
                .latestItems().asTuple();

        // or

        Multi<String> multi2 = Multi.createBy().combining()
                .streams(multiA, multiB)
                .latestItems().using(list -> combineItems(list));
        // </combine-last>

        List<Tuple2<A, B>> list = multi1.collect().asList().await().indefinitely();
        assertThat(list).hasSize(3);
        assertThat(list.toString()).contains("a3", "b1", "b2", "b3").doesNotContain("a1", "a2");
        List<String> strings = multi2.collect().asList().await().indefinitely();
        assertThat(strings).hasSize(3).containsExactly("a3b1", "a3b2", "a3b3");
    }

    private String combineItems(List<?> list) {
        return "" + list.get(0) + list.get(1);
    }

    private Multi<A> getMultiA() {
        return Multi.createFrom().items("a1", "a2", "a3")
                .onItem().transform(s -> (A) s);
    }

    private Multi<B> getMultiB() {
        return Multi.createFrom().items("b1", "b2", "b3")
                .onItem().transform(s -> (B) s);
    }

    private Uni<Response> invokeHttpServiceA() {
        return Uni.createFrom().item(new Response("A"))
                .onItem().delayIt().by(Duration.ofMillis(10));
    }

    private Uni<Response> invokeHttpServiceB() {
        return Uni.createFrom().item(new Response("B"))
                .onItem().delayIt().by(Duration.ofMillis(30));
    }

    static class Response {

        private final String content;

        public Response(String content) {
            this.content = content;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "content='" + content + '\'' +
                    '}';
        }
    }

}
