package snippets;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

public class RunSubscriptionOnTest {

    @Test
    public void testRunSubscriptionOn() {
        Executor executor = Infrastructure.getDefaultExecutor();
        AtomicBoolean completed = new AtomicBoolean();
        //tag::runSubscriptionOn[]
        Multi.createFrom().items(() -> {
            // called on a thread from the executor
            return retrieveItemsFromSource();
        })
                .onItem().transform(this::applySomeOperation)
                .runSubscriptionOn(executor)
                .subscribe().with(
                item -> System.out.println("Item: " + item),
                Throwable::printStackTrace,
                () -> completed.set(true)
        );
        //end::runSubscriptionOn[]

        await().untilAtomic(completed, is(true));
    }

    @Test
    public void testEmitOn() {
        Executor executor = Infrastructure.getDefaultExecutor();
        AtomicBoolean completed = new AtomicBoolean();
        //tag::emitOn[]
        Multi.createFrom().items(this::retrieveItemsFromSource)
                .emitOn(executor)
                .onItem().transform(this::applySomeOperation)
                .subscribe().with(
                item -> System.out.println("Item: " + item),
                Throwable::printStackTrace,
                () -> completed.set(true)
        );
        //end::emitOn[]

        await().untilAtomic(completed, is(true));
    }

    public String applySomeOperation(String s) {
        return s.toUpperCase();
    }

    public Stream<String> retrieveItemsFromSource() {
        return Stream.of("a", "b", "c");
    }
}
