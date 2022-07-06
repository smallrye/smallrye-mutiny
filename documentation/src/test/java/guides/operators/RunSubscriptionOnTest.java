package guides.operators;

import guides.extension.SystemOut;
import guides.extension.SystemOutCaptureExtension;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

@SuppressWarnings("Convert2MethodRef")
@ExtendWith(SystemOutCaptureExtension.class)
public class RunSubscriptionOnTest {

    @Test
    public void testRunSubscriptionOn(SystemOut out) {
        Executor executor = Infrastructure.getDefaultExecutor();
        AtomicBoolean completed = new AtomicBoolean();
        //<runSubscriptionOn>
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
        //</runSubscriptionOn>

        await().untilAtomic(completed, is(true));
    }

    @Test
    public void testEmitOn(SystemOut out) {
        Executor executor = Infrastructure.getDefaultExecutor();
        AtomicBoolean completed = new AtomicBoolean();
        //<emitOn>
        Multi.createFrom().items(this::retrieveItemsFromSource)
                .emitOn(executor)
                .onItem().transform(this::applySomeOperation)
                .subscribe().with(
                item -> System.out.println("Item: " + item),
                Throwable::printStackTrace,
                () -> completed.set(true)
        );
        //</emitOn>

        await().untilAtomic(completed, is(true));
    }

    public String applySomeOperation(String s) {
        return s.toUpperCase();
    }

    public Stream<String> retrieveItemsFromSource() {
        return Stream.of("a", "b", "c");
    }
}
