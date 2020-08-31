package snippets;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import io.smallrye.mutiny.subscription.MultiSubscriber;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

public class MultiCreationTest {

    @Test
    public void test() {
        // tag::code[]

        // Creation from a known item(s), or computed at subscription time
        Multi.createFrom().item("some known value");
        Multi.createFrom().item(() -> "some value computed at subscription time");
        Multi.createFrom().items("a", "b", "c");
        Multi.createFrom().items(() -> Stream.of("computed", "at", "subscription", "time"));
        Multi.createFrom().iterable(Arrays.asList("some", "iterable"));

        // Creation from a completion stage or completable future
        Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> "result"))
                .subscribe().with(
                        item -> System.out.println("Received: " + item),
                        failure -> System.out.println("Failed with " + failure.getMessage()));

        // Creation from a failure
        Multi.createFrom().failure(() -> new Exception("exception created at subscription time"));

        // Creation from an emitter
        Multi.createFrom().emitter(emitter -> {
            // ...
            emitter.emit("a");
            emitter.emit("b");
            emitter.complete();
            //...
        });

        // Create from a Reactive Streams Publisher or a Multi
        Multi.createFrom().publisher(Multi.createFrom().ticks().every(Duration.ofMillis(1)))
                .transform().byTakingFirstItems(2)
                .subscribe().with(
                        item -> System.out.println("Received tick " + item),
                        failure -> System.out.println("Failed with " + failure.getMessage()));

        // Defer the creation of the uni until subscription time
        Multi.createFrom().deferred(() -> Multi.createFrom().item("create the uni at subscription time"));

        // Created from a Uni
        Multi.createFrom().uni(Uni.createFrom().item("hello"));

        // Created from periodic ticks
        Multi.createFrom().ticks().every(Duration.ofMillis(1))
                .transform().byTakingFirstItems(2);

        // Created from integer range
        Multi.createFrom().range(1, 11);

        // end::code[]
    }

    @Test
    public void subscription() {
        // tag::subscription[]
        Multi<String> multi = Multi.createFrom().items("a", "b", "c");

        Cancellable cancellable = multi.subscribe().with(
                item -> System.out.println("Got " + item));
        // you can use the returned Cancellable to cancel the computation
        cancellable.cancel();

        cancellable = multi.subscribe().with(
                item -> System.out.println("Got " + item),
                failure -> System.out.println("Got a failure: " + failure),
                () -> System.out.println("Got the completion event"));

        multi.subscribe().withSubscriber(new MultiSubscriber<String>() {
            @Override
            public void onSubscribe(Subscription s) {
                System.out.println("Got subscription: " + s);
            }

            @Override
            public void onItem(String item) {
                System.out.println("Got an item: " + item);
            }

            @Override
            public void onFailure(Throwable failure) {
                System.out.println("Got a failure: " + failure);
            }

            @Override
            public void onCompletion() {
                System.out.println("Got the completion event");
            }
        });
        // end::subscription[]
    }

}
