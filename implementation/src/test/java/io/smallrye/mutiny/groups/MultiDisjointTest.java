package io.smallrye.mutiny.groups;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.reactivex.rxjava3.core.Flowable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import junit5.support.InfrastructureResource;
import mutiny.zero.flow.adapters.AdaptersToFlow;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class MultiDisjointTest {

    @Test
    public void testWithMultis() {
        AtomicBoolean subscribed = new AtomicBoolean();
        AssertSubscriber<String> subscriber = Multi.createFrom().items(
                Multi.createFrom().items("a", "b", "c"),
                Multi.createFrom().items("d", "e"),
                Multi.createFrom().empty(),
                Multi.createFrom().items("f", "g")
                        .onSubscription().invoke(() -> subscribed.set(true)))
                .onItem().<String> disjoint()
                .subscribe().withSubscriber(AssertSubscriber.create(4));
        assertThat(subscribed).isFalse();
        subscriber.assertItems("a", "b", "c", "d");
        subscriber.request(3);
        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).contains("e", "f", "g");
        assertThat(subscribed).isTrue();
    }

    @Test
    public void testWithPublishers() {
        AtomicBoolean subscribed = new AtomicBoolean();
        AssertSubscriber<String> subscriber = Multi.createFrom().items(
                AdaptersToFlow.publisher(Flowable.just("a", "b", "c")),
                AdaptersToFlow.publisher(Flowable.just("d", "e")),
                AdaptersToFlow.publisher(Flowable.empty()),
                AdaptersToFlow.publisher(Flowable.just("f", "g")
                        .doOnSubscribe(s -> subscribed.set(true))))
                .onItem().<String> disjoint()
                .subscribe().withSubscriber(AssertSubscriber.create(4));
        assertThat(subscribed).isFalse();
        subscriber.assertItems("a", "b", "c", "d");
        subscriber.request(3);
        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).contains("e", "f", "g");
        assertThat(subscribed).isTrue();
    }

    @Test
    public void testWithArrays() {
        AssertSubscriber<String> subscriber = Multi.createFrom().items(
                new String[] { "a", "b", "c" },
                new String[] { "d", "e" },
                new String[] {},
                new String[] { "f", "g" })
                .onItem().<String> disjoint()
                .subscribe().withSubscriber(AssertSubscriber.create(4));
        subscriber.assertItems("a", "b", "c", "d");
        subscriber.request(3);
        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).contains("e", "f", "g");
    }

    @Test
    public void testWithIterables() {
        AssertSubscriber<String> subscriber = Multi.createFrom().items(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e"),
                Collections.emptySet(),
                Collections.singleton("f"),
                Collections.singleton("g"))
                .onItem().<String> disjoint()
                .subscribe().withSubscriber(AssertSubscriber.create(4));
        subscriber.assertItems("a", "b", "c", "d");
        subscriber.request(3);
        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).contains("e", "f", "g");
    }

    @Test
    public void testWithMultisWithAFailure() {
        AtomicBoolean subscribed = new AtomicBoolean();
        AssertSubscriber<String> subscriber = Multi.createFrom().items(
                Multi.createFrom().items("a", "b", "c"),
                Multi.createFrom().items("d", "e"),
                Multi.createFrom().failure(new IOException("boom")),
                Multi.createFrom().items("f", "g")
                        .onSubscription().invoke(() -> subscribed.set(true)))
                .onItem().<String> disjoint()
                .subscribe().withSubscriber(AssertSubscriber.create(4));
        assertThat(subscribed).isFalse();
        subscriber.assertItems("a", "b", "c", "d");
        subscriber.request(3);
        subscriber.assertFailedWith(IOException.class, "boom");
        assertThat(subscribed).isFalse();
    }

    @Test
    public void testWithMultisWithOneEmittingAFailure() {
        AtomicBoolean subscribed = new AtomicBoolean();
        AssertSubscriber<String> subscriber = Multi.createFrom().items(
                Multi.createFrom().items("a", "b", "c"),
                Multi.createFrom().items("d", "e"),
                Multi.createFrom().emitter(e -> {
                    e.emit("f");
                    e.fail(new IOException("boom"));
                }),
                Multi.createFrom().items("g")
                        .onSubscription().invoke(() -> subscribed.set(true)))
                .onItem().<String> disjoint()
                .subscribe().withSubscriber(AssertSubscriber.create(4));
        assertThat(subscribed).isFalse();
        subscriber.assertItems("a", "b", "c", "d");
        subscriber.request(3);
        subscriber.assertFailedWith(IOException.class, "boom");
        assertThat(subscribed).isFalse();
    }

    @Test
    public void testWithIterablesContainingNull() {
        AssertSubscriber<String> subscriber = Multi.createFrom().items(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e"),
                Collections.emptySet(),
                Collections.singleton(null),
                Collections.singleton("g"))
                .onItem().<String> disjoint()
                .subscribe().withSubscriber(AssertSubscriber.create(4));
        subscriber.assertItems("a", "b", "c", "d");
        subscriber.request(3);
        subscriber.assertFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testWithInvalidType() {
        Multi.createFrom().items("a", "b", "c")
                .onItem().disjoint()
                .subscribe().withSubscriber(AssertSubscriber.create(2))
                .assertFailedWith(IllegalArgumentException.class, "String");
    }

    @Test
    public void testFlatMapRequestsWithEmissionOnExecutor() {
        AssertSubscriber<String> subscriber = Multi.createFrom().items("a", "b", "c", "d", "e", "f", "g", "h")
                .onItem()
                .transformToUni(s -> Uni.createFrom().item(s.toUpperCase()).onItem().delayIt().by(Duration.ofMillis(10)))
                .concatenate()
                .subscribe().withSubscriber(AssertSubscriber.create(0));

        subscriber
                .awaitSubscription()
                .assertHasNotReceivedAnyItem()
                .request(1);

        subscriber.awaitNextItem()
                .awaitNextItems(2)
                .assertItems("A", "B", "C");

        subscriber.request(100);
        subscriber.awaitCompletion();
    }
}
