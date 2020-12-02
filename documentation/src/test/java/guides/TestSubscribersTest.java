package guides;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TestSubscribersTest {

    @Test
    void uni() {
        // tag::uni[]
        Uni<Integer> uni = Uni.createFrom().item(63);

        UniAssertSubscriber<Integer> subscriber = uni
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(63);
        // end::uni[]
    }

    @Test
    void multi() {
        // tag::multi[]
        Multi<Integer> multi = Multi.createFrom().range(1, 5)
                .onItem().transform(n -> n * 10);

        AssertSubscriber<Integer> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertCompleted()
                .assertItems(10, 20, 30, 40);
        // end::multi[]
    }

    @Test
    void failing() {
        // tag::failing[]
        Multi<Object> multi = Multi.createFrom().failure(() -> new IOException("Boom"));

        AssertSubscriber<Object> subscriber = multi
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertFailedWith(IOException.class, "Boom");
        // end::failing[]
    }
}
