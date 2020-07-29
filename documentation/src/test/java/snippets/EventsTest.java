package snippets;

import io.smallrye.mutiny.Uni;
import org.junit.Test;

import io.smallrye.mutiny.Multi;

public class EventsTest {

    @Test
    public void test() {
        // tag::code[]
        Multi<String> source = Multi.createFrom().items("a", "b", "c");

        source
                .onItem().invoke(item -> System.out.println("Received item " + item))
                .onFailure().invoke(failure -> System.out.println("Failed with " + failure.getMessage()))
                .onCompletion().invoke(() -> System.out.println("Completed"))
                .onSubscribe().invoke(subscription -> System.out.println("We are subscribed!"))
                .onCancellation().invoke(() -> System.out.println("Downstream has cancelled the interaction"))
                .onRequest().invoke(n -> System.out.println("Downstream requested " + n + " items"))
                .subscribe().with(item -> System.out.println("Subscriber received " + item));
        // end::code[]

        // tag::shortcut[]
        Multi<String> multi = Multi.createFrom().items("a", "b", "c");
        multi.invoke(item -> System.out.println("Received item " + item));
        // end::shortcut[]

        // tag::invoke-uni[]
        multi.invokeUni(item -> executeAnAsyncAction(item));
        // end::invoke-uni[]

    }

    private Uni<?> executeAnAsyncAction(String item) {
        return Uni.createFrom().nullItem();
    }
}
