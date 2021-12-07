package io.smallrye.mutiny.operators.uni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.PrintStream;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ_WRITE)
class UniAndMultiLoggerTest {

    private static final PrintStream systemOut = System.out;

    @BeforeEach
    @AfterEach
    public void cleanup() {
        System.setOut(systemOut);
        Infrastructure.resetOperatorLogger();
    }

    @Test
    void checkNotNull() {
        assertThatThrownBy(() -> Infrastructure.setOperatorLogger(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operatorLogger");
    }

    @Test
    void uniNoEmptyIdentifier() {
        assertThatThrownBy(() -> Uni.createFrom().item(58).log(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The identifier cannot be an empty string");
    }

    private static class Event {
        String identifier;
        String event;
        Object value;
        Throwable failure;

        Event(String identifier, String event, Object value, Throwable failure) {
            this.identifier = identifier;
            this.event = event;
            this.value = value;
            this.failure = failure;
        }
    }

    @Test
    void checkUniOnItem() {
        ArrayList<Event> events = new ArrayList<>();
        Infrastructure.setOperatorLogger((identifier, event, value, failure) -> {
            events.add(new Event(identifier, event, value, failure));
        });

        UniAssertSubscriber<Integer> sub = UniAssertSubscriber.create();
        Uni.createFrom().item(58).log().subscribe().withSubscriber(sub);

        assertThat(events).hasSize(2);

        Event event = events.get(0);
        assertThat(event.identifier).isEqualTo("Uni.UniCreateFromKnownItem.0");
        assertThat(event.event).isEqualTo("onSubscribe");
        assertThat(event.value).isNull();
        assertThat(event.failure).isNull();

        event = events.get(1);
        assertThat(event.identifier).isEqualTo("Uni.UniCreateFromKnownItem.0");
        assertThat(event.event).isEqualTo("onItem");
        assertThat(event.value).isEqualTo(58);
        assertThat(event.failure).isNull();
    }

    @Test
    void checkCancelledUniOnItem() {
        ArrayList<Event> events = new ArrayList<>();
        Infrastructure.setOperatorLogger((identifier, event, value, failure) -> {
            events.add(new Event(identifier, event, value, failure));
        });

        UniAssertSubscriber<Integer> sub = UniAssertSubscriber.create();
        sub.cancel();
        Uni.createFrom().item(58).log().subscribe().withSubscriber(sub);

        assertThat(events).hasSize(2);

        Event event = events.get(0);
        assertThat(event.identifier).isEqualTo("Uni.UniCreateFromKnownItem.0");
        assertThat(event.event).isEqualTo("onSubscribe");
        assertThat(event.value).isNull();
        assertThat(event.failure).isNull();

        event = events.get(1);
        assertThat(event.identifier).isEqualTo("Uni.UniCreateFromKnownItem.0");
        assertThat(event.event).isEqualTo("cancel");
        assertThat(event.value).isNull();
        assertThat(event.failure).isNull();
    }

    @Test
    void checkUniOnFailure() {
        ArrayList<Event> events = new ArrayList<>();
        Infrastructure.setOperatorLogger((identifier, event, value, failure) -> {
            events.add(new Event(identifier, event, value, failure));
        });

        UniAssertSubscriber<Object> sub = UniAssertSubscriber.create();
        Uni.createFrom().failure(new RuntimeException("boom")).log().subscribe().withSubscriber(sub);

        assertThat(events).hasSize(2);

        Event event = events.get(0);
        assertThat(event.identifier).isEqualTo("Uni.UniCreateFromKnownFailure.0");
        assertThat(event.event).isEqualTo("onSubscribe");
        assertThat(event.value).isNull();
        assertThat(event.failure).isNull();

        event = events.get(1);
        assertThat(event.identifier).isEqualTo("Uni.UniCreateFromKnownFailure.0");
        assertThat(event.event).isEqualTo("onFailure");
        assertThat(event.value).isNull();
        assertThat(event.failure).isInstanceOf(RuntimeException.class).hasMessage("boom");
    }

    @Test
    void multiNoEmptyIdentifier() {
        assertThatThrownBy(() -> Multi.createFrom().item(58).log(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The identifier cannot be an empty string");
    }

    @Test
    void checkMultiWithItems() {
        ArrayList<Event> events = new ArrayList<>();
        Infrastructure.setOperatorLogger((identifier, event, value, failure) -> {
            events.add(new Event(identifier, event, value, failure));
        });

        AssertSubscriber<Integer> sub = AssertSubscriber.create(10L);
        Multi.createFrom().items(1, 2, 3).log().subscribe().withSubscriber(sub);

        assertThat(events).hasSize(6);

        Event event = events.get(0);
        assertThat(event.identifier).isEqualTo("Multi.CollectionBasedMulti.0");
        assertThat(event.event).isEqualTo("onSubscribe");
        assertThat(event.value).isNull();
        assertThat(event.failure).isNull();

        event = events.get(1);
        assertThat(event.identifier).isEqualTo("Multi.CollectionBasedMulti.0");
        assertThat(event.event).isEqualTo("request");
        assertThat(event.value).isEqualTo(10L);
        assertThat(event.failure).isNull();

        event = events.get(2);
        assertThat(event.identifier).isEqualTo("Multi.CollectionBasedMulti.0");
        assertThat(event.event).isEqualTo("onItem");
        assertThat(event.value).isEqualTo(1);
        assertThat(event.failure).isNull();

        event = events.get(3);
        assertThat(event.identifier).isEqualTo("Multi.CollectionBasedMulti.0");
        assertThat(event.event).isEqualTo("onItem");
        assertThat(event.value).isEqualTo(2);
        assertThat(event.failure).isNull();

        event = events.get(4);
        assertThat(event.identifier).isEqualTo("Multi.CollectionBasedMulti.0");
        assertThat(event.event).isEqualTo("onItem");
        assertThat(event.value).isEqualTo(3);
        assertThat(event.failure).isNull();

        event = events.get(5);
        assertThat(event.identifier).isEqualTo("Multi.CollectionBasedMulti.0");
        assertThat(event.event).isEqualTo("onCompletion");
        assertThat(event.value).isNull();
        assertThat(event.failure).isNull();
    }

    @Test
    void checkCancelledMultiWithItems() {
        ArrayList<Event> events = new ArrayList<>();
        Infrastructure.setOperatorLogger((identifier, event, value, failure) -> {
            events.add(new Event(identifier, event, value, failure));
        });

        AssertSubscriber<Integer> sub = AssertSubscriber.create(0L);
        Multi.createFrom().items(1, 2, 3).log().subscribe().withSubscriber(sub);
        sub.cancel();
        sub.request(10L);

        assertThat(events).hasSize(2);

        Event event = events.get(0);
        assertThat(event.identifier).isEqualTo("Multi.CollectionBasedMulti.0");
        assertThat(event.event).isEqualTo("onSubscribe");
        assertThat(event.value).isNull();
        assertThat(event.failure).isNull();

        event = events.get(1);
        assertThat(event.identifier).isEqualTo("Multi.CollectionBasedMulti.0");
        assertThat(event.event).isEqualTo("cancel");
        assertThat(event.value).isNull();
        assertThat(event.failure).isNull();
    }

    @Test
    void checkMultiWithFailure() {
        ArrayList<Event> events = new ArrayList<>();
        Infrastructure.setOperatorLogger((identifier, event, value, failure) -> {
            events.add(new Event(identifier, event, value, failure));
        });

        AssertSubscriber<Object> sub = AssertSubscriber.create(10L);
        Multi.createFrom().failure(new RuntimeException("boom")).log().subscribe().withSubscriber(sub);

        assertThat(events).hasSize(3);

        Event event = events.get(0);
        assertThat(event.identifier).isEqualTo("Multi.FailedMulti.0");
        assertThat(event.event).isEqualTo("onSubscribe");
        assertThat(event.value).isNull();
        assertThat(event.failure).isNull();

        event = events.get(1);
        assertThat(event.identifier).isEqualTo("Multi.FailedMulti.0");
        assertThat(event.event).isEqualTo("request");
        assertThat(event.value).isEqualTo(10L);
        assertThat(event.failure).isNull();

        event = events.get(2);
        assertThat(event.identifier).isEqualTo("Multi.FailedMulti.0");
        assertThat(event.event).isEqualTo("onFailure");
        assertThat(event.value).isNull();
        assertThat(event.failure).isInstanceOf(RuntimeException.class).hasMessage("boom");
    }
}
