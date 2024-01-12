package io.smallrye.mutiny.operators.multi.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class ReplayOperatorTest {

    @Test
    public void shouldRemoveSubscriptionAfterCompletion() {
        // given
        var upstream = Multi.createFrom().range(0, 3);
        var operator = new ReplayOperator<>(upstream, 3);

        // when
        var subscriber = operator.subscribe().withSubscriber(AssertSubscriber.create(3));
        var subscriber2 = operator.subscribe().withSubscriber(AssertSubscriber.create(3));

        // then
        subscriber.assertItems(0, 1, 2).assertCompleted();
        subscriber2.assertItems(0, 1, 2).assertCompleted();
        assertEquals(0, operator.subscriptions.size());
    }
}
