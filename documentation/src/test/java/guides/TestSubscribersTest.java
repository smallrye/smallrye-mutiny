package guides;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertMulti;
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

        subscriber
                .awaitItem()
                .assertItem(63);
        // end::uni[]
    }

    @Test
    void multi() {
        // tag::multi[]
        Multi<Integer> multi = Multi.createFrom().range(1, 5)
                .onItem().transform(n -> n * 10);

        AssertSubscriber<Integer> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .awaitCompletion()
                .assertItems(10, 20, 30, 40);
        // end::multi[]
    }

    @Test
    void failing() {
        // tag::failing[]
        Multi<Object> multi = Multi.createFrom().failure(() -> new IOException("Boom"));

        AssertSubscriber<Object> subscriber = multi
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .awaitFailure()
                .assertFailedWith(IOException.class, "Boom");
        // end::failing[]
    }

    @Test
    void uniPredicate() {
        // tag::uni-predicate[]
        Uni<Integer> uni = Uni.createFrom().item(63);

        UniAssertSubscriber<Integer> subscriber = uni
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber
                .awaitItem()
                .assertItem(i -> i > 0, "positive number")
                .inspectItem(item -> {
                    // use any assertion library here
                    assert item == 63;
                });
        // end::uni-predicate[]
    }

    @Test
    void multiPredicate() {
        // tag::multi-predicate[]
        Multi<Integer> multi = Multi.createFrom().range(1, 5)
                .onItem().transform(n -> n * 10);

        AssertSubscriber<Integer> subscriber = multi
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .awaitCompletion()
                .assertLastItem(i -> i == 40, "last item is 40")
                .assertItemCount(4)
                .inspectItems(items -> {
                    // use any assertion library here
                    assert items.size() == 4;
                });
        // end::multi-predicate[]
    }

    @Test
    void assertMulti() {
        // tag::assert-multi[]
        Multi<Integer> multi = Multi.createFrom().range(1, 6);

        AssertMulti.create(multi)
                .expectNext(1, 2, 3)
                .expectNextMatches(i -> i > 3, "greater than 3")
                .expectNext(5)
                .expectComplete()
                .verify();
        // end::assert-multi[]
    }

    @Test
    void assertMultiFailure() {
        // tag::assert-multi-failure[]
        Multi<Object> multi = Multi.createFrom().failure(new IOException("boom"));

        AssertMulti.create(multi)
                .expectFailure(IOException.class, "boom")
                .verify();
        // end::assert-multi-failure[]
    }

    @Test
    void assertMultiDemand() {
        // tag::assert-multi-demand[]
        Multi<Integer> multi = Multi.createFrom().range(1, 4);

        AssertMulti.create(multi)
                .thenRequest(10)
                .expectNext(1, 2, 3)
                .expectComplete()
                .verify();
        // end::assert-multi-demand[]
    }
}
