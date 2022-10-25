package guides;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.spies.MultiGlobalSpy;
import io.smallrye.mutiny.helpers.spies.MultiOnCompletionSpy;
import io.smallrye.mutiny.helpers.spies.MultiOnRequestSpy;
import io.smallrye.mutiny.helpers.spies.Spy;
import org.junit.jupiter.api.Test;

public class SpiesTest {

    @Test
    public void trackItems() {
        // <selected>
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3);
        MultiOnRequestSpy<Integer> requestSpy = Spy.onRequest(multi);
        MultiOnCompletionSpy<Integer> completionSpy = Spy.onCompletion(requestSpy);

        completionSpy.subscribe().with(System.out::println);

        System.out.println("Number of requests: " + requestSpy.requestedCount());
        System.out.println("Completed? " + completionSpy.invoked());
        // </selected>
    }

    @Test
    public void trackEverything() {
        // <global>
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3);
        MultiGlobalSpy<Integer> spy = Spy.globally(multi);

        spy.subscribe().with(System.out::println);

        System.out.println("Number of requests: " + spy.onRequestSpy().requestedCount());
        System.out.println("Cancelled? " + spy.onCancellationSpy().isCancelled());
        System.out.println("Failure? " + spy.onFailureSpy().lastFailure());
        System.out.println("Items: " + spy.onItemSpy().items());
        // </global>
    }
}