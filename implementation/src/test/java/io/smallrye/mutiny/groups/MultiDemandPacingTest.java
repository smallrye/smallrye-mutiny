package io.smallrye.mutiny.groups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.DemandPacer;
import io.smallrye.mutiny.subscription.FixedDemandPacer;

class MultiDemandPacingTest {

    @Test
    void rejectNullParameters() {
        Multi<Integer> multi = Multi.createFrom().range(1, 100);
        assertThatThrownBy(() -> multi.paceDemand().on(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be `null`");
        assertThatThrownBy(() -> multi.paceDemand().using(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be `null`");
    }

    @Test
    void fixedDemandPacer() {
        ArrayList<Long> requests = new ArrayList<>();
        FixedDemandPacer pacer = new FixedDemandPacer(25L, Duration.ofMillis(100L));
        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .onRequest().invoke(requests::add)
                .paceDemand().on(Infrastructure.getDefaultWorkerPool()).using(pacer)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.awaitCompletion();
        assertThat(sub.getItems()).hasSize(100).contains(58, 63, 69);
        assertThat(requests).containsExactly(25L, 25L, 25L, 25L);
    }

    @Test
    void fixedDemandPacerWithFailure() {
        ArrayList<Long> requests = new ArrayList<>();
        FixedDemandPacer pacer = new FixedDemandPacer(25L, Duration.ofMillis(100L));
        AssertSubscriber<Integer> sub = Multi.createFrom().<Integer> failure(new IOException("boom"))
                .onRequest().invoke(requests::add)
                .paceDemand().using(pacer)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.awaitFailure();
        assertThat(sub.getItems()).isEmpty();
        assertThat(requests).containsExactly(25L);
    }

    @Test
    void fixedDemandPacerWithCancellation() {
        ArrayList<Long> requests = new ArrayList<>();
        AtomicBoolean cancelled = new AtomicBoolean();
        FixedDemandPacer pacer = new FixedDemandPacer(25L, Duration.ofSeconds(5L));
        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .onCancellation().invoke(() -> cancelled.set(true))
                .onRequest().invoke(requests::add)
                .paceDemand().using(pacer)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.cancel();

        sub.assertNotTerminated();
        assertThat(cancelled).isTrue();
        assertThat(sub.getItems()).hasSize(25);
        assertThat(requests).containsExactly(25L);
    }

    @Test
    void fixedDemandPacerWithEarlyCancellation() {
        ArrayList<Long> requests = new ArrayList<>();
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicReference<Subscription> subscriptionBox = new AtomicReference<>();
        FixedDemandPacer pacer = new FixedDemandPacer(25L, Duration.ofSeconds(5L));
        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .onCancellation().invoke(() -> cancelled.set(true))
                .onRequest().invoke(e -> {
                    requests.add(e);
                    subscriptionBox.get().cancel();
                })
                .onSubscription().invoke(subscriptionBox::set)
                .paceDemand().using(pacer)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.assertNotTerminated();
        assertThat(cancelled).isTrue();
        assertThat(sub.getItems()).isEmpty();
        assertThat(requests).containsExactly(25L);
    }

    @Test
    void pacerMustNotReturnNullInitialRequest() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .paceDemand().using(new DemandPacer() {
                    @Override
                    public Request initial() {
                        return null;
                    }

                    @Override
                    public Request apply(Request previousRequest, long observedItemsCount) {
                        return new Request(100, Duration.ofSeconds(1L));
                    }
                }).subscribe().withSubscriber(AssertSubscriber.create());
        sub.awaitFailure().assertFailedWith(NullPointerException.class, "The pacer provided a null initial request");
    }

    @Test
    void pacerMustNotFailOnInitialRequest() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .paceDemand().using(new DemandPacer() {
                    @Override
                    public Request initial() {
                        throw new RuntimeException("boom");
                    }

                    @Override
                    public Request apply(Request previousRequest, long observedItemsCount) {
                        return new Request(100, Duration.ofSeconds(1L));
                    }
                }).subscribe().withSubscriber(AssertSubscriber.create());
        sub.awaitFailure().assertFailedWith(RuntimeException.class, "boom");
    }

    @Test
    void pacerMustNotReturnNullRequests() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .paceDemand().using(new DemandPacer() {
                    @Override
                    public Request initial() {
                        return new Request(1L, Duration.ofMillis(100L));
                    }

                    @Override
                    public Request apply(Request previousRequest, long observedItemsCount) {
                        return null;
                    }
                }).subscribe().withSubscriber(AssertSubscriber.create());
        sub.awaitFailure().assertFailedWith(NullPointerException.class, "The pacer provided a null request");
    }

    @Test
    void pacerMustNotFailRequests() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .paceDemand().using(new DemandPacer() {
                    @Override
                    public Request initial() {
                        return new Request(1L, Duration.ofMillis(100L));
                    }

                    @Override
                    public Request apply(Request previousRequest, long observedItemsCount) {
                        throw new RuntimeException("boom");
                    }
                }).subscribe().withSubscriber(AssertSubscriber.create());
        sub.awaitFailure().assertFailedWith(RuntimeException.class, "boom");
    }

    @Test
    void contextIsPropagated() {
        FixedDemandPacer pacer = new FixedDemandPacer(25L, Duration.ofMillis(50L));
        AssertSubscriber<String> sub = Multi.createFrom().range(0, 100)
                .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.<String> get("foo")))
                .paceDemand().using(pacer)
                .subscribe().withSubscriber(AssertSubscriber.create(Context.of("foo", "bar")));

        sub.awaitCompletion();
        assertThat(sub.getItems()).hasSize(100).contains("58::bar", "63::bar", "69::bar");
    }
}
