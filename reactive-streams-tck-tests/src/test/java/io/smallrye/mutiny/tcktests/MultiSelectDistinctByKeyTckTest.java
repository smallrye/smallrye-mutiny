package io.smallrye.mutiny.tcktests;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.testng.Assert;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;

public class MultiSelectDistinctByKeyTckTest extends AbstractPublisherTck<MultiSelectDistinctByKeyTckTest.KeyTester> {
    @Test
    public void distinctStageShouldReturnDistinctElements() {

        KeyTester kt1 = new KeyTester(1, "foo");
        KeyTester kt2 = new KeyTester(2, "bar");
        KeyTester kt3 = new KeyTester(3, "baz");

        Assert.assertEquals(
                Await.await(
                        Multi.createFrom().items(kt1, kt2, kt2, kt3, kt2, kt1, kt3)
                                .select().distinct(kt -> kt.id)
                                .collect().asList()
                                .subscribeAsCompletionStage()),
                Arrays.asList(kt1, kt2, kt3));
    }

    @Test
    public void distinctStageShouldReturnAnEmptyStreamWhenCalledOnEmptyStreams() {
        Assert.assertEquals(
                Await.await(Multi.createFrom().<KeyTester> empty()
                        .select().distinct(kt -> kt.id)
                        .collect().asList()
                        .subscribeAsCompletionStage()),
                Collections.emptyList());
    }

    @Test
    public void distinctStageShouldPropagateUpstreamExceptions() {
        Assert.assertThrows(QuietRuntimeException.class,
                () -> Await.await(
                        Multi.createFrom().<KeyTester> failure(new QuietRuntimeException("failed"))
                                .select().distinct(kt -> kt.id)
                                .collect().asList()
                                .subscribeAsCompletionStage()));
    }

    @Test
    public void distinctStageShouldPropagateExceptionsThrownByKeyEquals() {
        Assert.assertThrows(
                QuietRuntimeException.class,
                () -> {
                    CompletableFuture<Void> cancelled = new CompletableFuture<>();
                    class ObjectThatThrowsFromKeyEquals {

                        Key key = new Key();

                        class Key {
                            @Override
                            public int hashCode() {
                                return 1;
                            }

                            @Override
                            public boolean equals(Object obj) {
                                throw new QuietRuntimeException("failed");
                            }
                        }
                    }
                    CompletionStage<List<ObjectThatThrowsFromKeyEquals>> result = Multi.createFrom()
                            .items(new ObjectThatThrowsFromKeyEquals(), new ObjectThatThrowsFromKeyEquals())
                            .onTermination()
                            .invoke(() -> cancelled.complete(null))
                            .select()
                            .distinct(o -> o.key)
                            .collect()
                            .asList()
                            .subscribeAsCompletionStage();
                    Await.await(cancelled);
                    Await.await(result);
                });
    }

    @Test
    public void distinctStageShouldPropagateCancel() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        infiniteStream()
                .onTermination().invoke(() -> cancelled.complete(null))
                .map(id -> new KeyTester(id, "text-" + id))
                .select().distinct(kt -> kt.id).subscribe()
                .withSubscriber(new Subscriptions.CancelledSubscriber<>());
        Await.await(cancelled);
    }

    @Override
    public Flow.Publisher<KeyTester> createFlowPublisher(long elements) {
        return upstream(elements)
                .map(id -> new KeyTester(id, "text-" + id))
                .select().distinct(kt -> kt.id);
    }

    @Override
    public Flow.Publisher<KeyTester> createFailedFlowPublisher() {
        return failedUpstream()
                .map(id -> new KeyTester(id, "text-" + id))
                .select().distinct(kt -> kt.id);
    }

    public static final class KeyTester {

        private final long id;
        private final String text;

        private KeyTester(long id, String text) {
            this.id = id;
            this.text = text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            KeyTester keyTester = (KeyTester) o;
            return id == keyTester.id && Objects.equals(text, keyTester.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, text);
        }
    }
}
