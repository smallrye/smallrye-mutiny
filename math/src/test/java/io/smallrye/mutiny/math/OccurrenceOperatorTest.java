package io.smallrye.mutiny.math;

import static org.assertj.core.api.AssertionsForClassTypes.entry;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class OccurrenceOperatorTest {

    @Test
    public void testWithEmpty() {
        AssertSubscriber<Map<String, Long>> subscriber = Multi.createFrom().<String> empty()
                .plug(Math.occurrence())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .awaitItems(1)
                .assertItems(Collections.emptyMap())
                .awaitCompletion();
    }

    @Test
    public void testWithNever() {
        AssertSubscriber<Map<String, Long>> subscriber = Multi.createFrom().<String> nothing()
                .plug(Math.occurrence())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.cancel();
        subscriber.assertNotTerminated();
        Assertions.assertEquals(0, subscriber.getItems().size());
    }

    @Test
    public void testWithItems() {
        AssertSubscriber<Map<String, Long>> subscriber = Multi.createFrom()
                .items("a", "b", "a", "c", "b", "e", "e", "a")
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.occurrence())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .run(() -> assertThat(subscriber.getItems()).hasSize(3)
                        .anySatisfy(m -> assertThat(m).hasSize(1).contains(entry("a", 1L)))
                        .anySatisfy(m -> assertThat(m).hasSize(2).contains(entry("a", 1L), entry("b", 1L)))
                        .anySatisfy(m -> assertThat(m).hasSize(2).contains(entry("a", 2L), entry("b", 1L))))
                .request(10)
                .awaitCompletion()
                .run(() -> assertThat(subscriber.getItems()).hasSize(8)
                        .anySatisfy(m -> assertThat(m).hasSize(4)
                                .contains(entry("a", 3L), entry("b", 2L), entry("c", 1L), entry("e", 2L))));
    }

    @Test
    public void testLatest() {
        Map<String, Long> last = Multi.createFrom().items("a", "b", "a", "c", "b", "e", "e", "a")
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.occurrence())
                .collect().last()
                .await().indefinitely();

        assertThat(last).hasSize(4).contains(entry("a", 3L), entry("b", 2L), entry("c", 1L), entry("e", 2L));
    }

}
