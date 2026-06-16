package io.smallrye.mutiny.operators.multi.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.MultiEmitter;

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

    @Test
    public void zeroDemandSubscriberShouldNotReceivePrematureCompletion() {
        var emitterRef = new AtomicReference<MultiEmitter<? super Integer>>();
        Multi<Integer> upstream = Multi.createFrom().<Integer> emitter(emitterRef::set);

        var operator = new ReplayOperator<>(upstream, 1);

        var sub1 = operator.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        emitterRef.get().emit(1);
        sub1.assertItems(1);

        // Second subscriber with zero initial demand — cursor positioned at Cell(1)
        var sub2 = operator.subscribe().withSubscriber(AssertSubscriber.create());

        // Complete upstream — triggerDrainLoops calls drain on sub2 with demand=0
        emitterRef.get().complete();
        sub1.assertCompleted();

        // sub2 should receive item 1 before completion
        sub2.request(1);
        sub2.assertItems(1).assertCompleted();
    }

    @Test
    public void zeroDemandSubscriberWithUnboundedReplayShouldNotReceivePrematureCompletion() {
        var emitterRef = new AtomicReference<MultiEmitter<? super Integer>>();
        Multi<Integer> upstream = Multi.createFrom().<Integer> emitter(emitterRef::set);

        var operator = new ReplayOperator<>(upstream, Long.MAX_VALUE);

        var sub1 = operator.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        emitterRef.get().emit(1);
        emitterRef.get().emit(2);
        emitterRef.get().emit(3);
        sub1.assertItems(1, 2, 3);

        var sub2 = operator.subscribe().withSubscriber(AssertSubscriber.create());

        emitterRef.get().complete();
        sub1.assertCompleted();

        sub2.request(3);
        sub2.assertItems(1, 2, 3).assertCompleted();
    }

    @Test
    public void zeroDemandSubscriberShouldReceiveFailureAfterItems() {
        var emitterRef = new AtomicReference<MultiEmitter<? super Integer>>();
        Multi<Integer> upstream = Multi.createFrom().<Integer> emitter(emitterRef::set);

        var operator = new ReplayOperator<>(upstream, 1);

        var sub1 = operator.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        emitterRef.get().emit(1);
        sub1.assertItems(1);

        // Second subscriber with zero demand — cursor positioned at Cell(1)
        var sub2 = operator.subscribe().withSubscriber(AssertSubscriber.create());

        // Fail upstream — triggerDrainLoops calls drain on sub2 with demand=0
        emitterRef.get().fail(new IOException("boom"));
        sub1.assertFailedWith(IOException.class, "boom");

        // sub2 should receive item 1 then the failure
        sub2.request(1);
        sub2.assertItems(1).assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void zeroDemandSubscriberShouldReceiveFailureWhenAllItemsConsumed() {
        var emitterRef = new AtomicReference<MultiEmitter<? super Integer>>();
        Multi<Integer> upstream = Multi.createFrom().<Integer> emitter(emitterRef::set);

        var operator = new ReplayOperator<>(upstream, 1);

        var sub1 = operator.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        emitterRef.get().emit(1);
        sub1.assertItems(1);

        // Second subscriber requests 1 item, consuming the only replayed item
        var sub2 = operator.subscribe().withSubscriber(AssertSubscriber.create(1));
        sub2.assertItems(1);

        // Fail upstream — sub2 has demand=0 but all items consumed, failure should arrive
        emitterRef.get().fail(new IOException("boom"));
        sub1.assertFailedWith(IOException.class, "boom");
        sub2.assertFailedWith(IOException.class, "boom");
    }
}
