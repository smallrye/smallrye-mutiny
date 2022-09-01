package io.smallrye.mutiny.jakarta.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.microprofile.reactive.streams.operators.CompletionRunner;
import org.eclipse.microprofile.reactive.streams.operators.CompletionSubscriber;
import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class APITest {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @AfterEach
    public void cleanup() {
        executor.shutdown();
    }

    @Test
    public void test1() throws ExecutionException, InterruptedException {
        CompletionStage<Integer> stage = ReactiveStreams.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .filter(i -> i % 2 == 0)
                .collect(Collectors.summingInt(i -> i)).run();
        int sum = stage.toCompletableFuture().get();
        assertThat(sum).isEqualTo(2 + 4 + 6 + 8 + 10);
    }

    @Test
    public void testIntsPublisherFromSpec() throws ExecutionException, InterruptedException {
        PublisherBuilder<Integer> intsPublisher = ReactiveStreams.of(1, 2, 3);
        CompletionRunner<List<Integer>> intsResult = intsPublisher.toList();

        assertThat(intsResult.run().toCompletableFuture().get()).containsExactly(1, 2, 3);
    }

    @Test
    public void testToStringProcessorFromSpec() throws ExecutionException, InterruptedException {
        ProcessorBuilder<Integer, String> toStringProcessor = ReactiveStreams.<Integer> builder().map(Object::toString);
        SubscriberBuilder<Integer, List<String>> toList = toStringProcessor.toList();

        CompletionStage<List<String>> stage = ReactiveStreams.of(1, 2, 3, 4, 5).to(toList).run();
        assertThat(stage.toCompletableFuture().get()).containsExactly("1", "2", "3", "4", "5");
    }

    @Test
    public void testTrivialClosedGraphFromSpec() throws ExecutionException, InterruptedException {
        CompletionStage<Optional<Integer>> result = ReactiveStreams
                .fromIterable(() -> IntStream.range(1, 1000).boxed().iterator())
                .filter(i -> (i & 1) == 1)
                .map(i -> i + 2)
                .collect(Collectors.reducing((i, j) -> i + j))
                .run();

        assertThat(result.toCompletableFuture().get()).contains(251000);
    }

    @Test
    public void testBuildingPublisherFromSpec() {
        List<MyDomainObject> domainObjects = Arrays.asList(new MyDomainObject("Clement", "Neo"),
                new MyDomainObject("Tintin", "Milou"));

        Publisher<ByteBuffer> publisher = ReactiveStreams.fromIterable(domainObjects)
                .map(obj -> String.format("%s,%s\n", obj.field1, obj.field2))
                .map(line -> ByteBuffer.wrap(line.getBytes()))
                .buildRs();

        List<String> list = new ArrayList<>();
        AtomicBoolean done = new AtomicBoolean();

        executor.submit(() -> publisher.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(5);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                list.add(new String(byteBuffer.array()));
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                done.set(true);
            }
        }));

        await().untilAtomic(done, is(true));
        assertThat(list).containsExactly("Clement,Neo\n", "Tintin,Milou\n");
    }

    private Processor<ByteBuffer, MyDomainObject> createParser() {
        return ReactiveStreams.<ByteBuffer> builder()
                .map(buffer -> new String(buffer.array()))
                .map(String::trim)
                .map(line -> line.split(","))
                .map(array -> new MyDomainObject(array[0], array[1])).buildRs();

    }

    @Test
    public void testBuildingSubscriberFromSpec() throws ExecutionException, InterruptedException {
        Processor<ByteBuffer, MyDomainObject> parser = createParser();

        CompletionSubscriber<ByteBuffer, List<MyDomainObject>> subscriber = ReactiveStreams.<ByteBuffer> builder()
                .via(parser)
                .toList()
                .build();

        CompletionStage<List<MyDomainObject>> result = subscriber.getCompletion();

        List<MyDomainObject> domainObjects = Arrays.asList(new MyDomainObject("Clement", "Neo"),
                new MyDomainObject("Tintin", "Milou"));
        Publisher<ByteBuffer> publisher = ReactiveStreams.fromIterable(domainObjects)
                .map(obj -> String.format("%s,%s\n", obj.field1, obj.field2))
                .map(line -> ByteBuffer.wrap(line.getBytes()))
                .buildRs();

        publisher.subscribe(subscriber);
        List<MyDomainObject> objects = result.toCompletableFuture().get();
        assertThat(objects.toString()).contains("Clement => Neo", "Tintin => Milou");
    }

    private class MyDomainObject {
        private final String field1;
        private final String field2;

        MyDomainObject(String f1, String f2) {
            this.field1 = f1;
            this.field2 = f2;
        }

        @Override
        public String toString() {
            return field1 + " => " + field2;
        }
    }
}
