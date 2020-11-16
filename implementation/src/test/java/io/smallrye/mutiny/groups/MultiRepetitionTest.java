package io.smallrye.mutiny.groups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.UniEmitter;

public class MultiRepetitionTest {

    @Test
    public void testWithEmitterWithSharedState() {
        AtomicInteger shared = new AtomicInteger();
        Multi<Integer> multi = Multi.createBy().repeating()
                .<AtomicInteger, Integer> uni(() -> shared,
                        (state, emitter) -> emitter.complete(state.incrementAndGet()))
                .atMost(2);

        assertThat(shared).hasValue(0);
        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertItems(1);
        assertThat(shared).hasValue(1);
        subscriber.request(1);
        subscriber.assertCompleted().assertItems(1, 2);
    }

    @Test
    public void testWithEmitter() {
        AtomicInteger shared = new AtomicInteger();
        Multi<Integer> multi = Multi.createBy().repeating()
                .<Integer> uni(emitter -> emitter.complete(shared.incrementAndGet()))
                .atMost(2);

        assertThat(shared).hasValue(0);
        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertItems(1);
        assertThat(shared).hasValue(1);
        subscriber.request(1);
        subscriber.assertCompleted().assertItems(1, 2);
    }

    @Test
    public void testWithEmitterProducingFailureWithSharedState() {
        Supplier<AtomicInteger> boom = () -> {
            throw new IllegalStateException("boom");
        };

        Multi<Integer> multi = Multi.createBy().repeating()
                .<AtomicInteger, Integer> uni(boom,
                        (state, emitter) -> emitter.complete(state.incrementAndGet()))
                .atMost(2);

        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithEmitterProducingNullWithSharedState() {
        Supplier<AtomicInteger> boom = () -> null;

        Multi<Integer> multi = Multi.createBy().repeating()
                .<AtomicInteger, Integer> uni(boom,
                        (state, emitter) -> emitter.complete(state.incrementAndGet()))
                .atMost(2);

        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertFailedWith(NullPointerException.class, "supplier");
    }

    @Test
    public void testThatStateSupplierCannotBeNullWithEmitterWithSharedState() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createBy().repeating().uni(null,
                (x, emitter) -> {
                }));
    }

    @Test
    public void testThatFunctionCannotBeNullWithEmitterWithSharedState() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createBy().repeating().uni(() -> "hello",
                (BiConsumer<String, UniEmitter<? super String>>) null));
    }

    @Test
    public void testThatFunctionCannotBeNullWithEmitter() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createBy().repeating().uni((Consumer<UniEmitter<? super Object>>) null));
    }

    @Test
    public void testWithUniWithSharedState() {
        AtomicInteger shared = new AtomicInteger();
        Multi<Integer> multi = Multi.createBy().repeating()
                .uni(() -> shared, (state) -> Uni.createFrom().item(state.incrementAndGet()))
                .atMost(2);

        assertThat(shared).hasValue(0);
        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertItems(1);
        assertThat(shared).hasValue(1);
        subscriber.request(1);
        subscriber.assertCompleted().assertItems(1, 2);
    }

    @Test
    public void testWithUni() {
        AtomicInteger shared = new AtomicInteger();
        Multi<Integer> multi = Multi.createBy().repeating()
                .uni(() -> Uni.createFrom().item(shared.incrementAndGet()))
                .atMost(2);

        assertThat(shared).hasValue(0);
        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertItems(1);
        assertThat(shared).hasValue(1);
        subscriber.request(1);
        subscriber.assertCompleted().assertItems(1, 2);
    }

    @Test
    public void testWithUniProducingFailureWithSharedState() {
        Supplier<AtomicInteger> boom = () -> {
            throw new IllegalStateException("boom");
        };

        Multi<Integer> multi = Multi.createBy().repeating()
                .uni(boom, (state) -> Uni.createFrom().item(state.incrementAndGet()))
                .atMost(2);

        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithUniProducingFailure() {
        Multi<Integer> multi = Multi.createBy().repeating()
                .<Integer> uni(() -> Uni.createFrom().failure(new IllegalStateException("boom")))
                .atMost(2);

        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithUniProducingNull() {
        Multi<Integer> multi = Multi.createBy().repeating()
                .<Integer> uni(() -> null)
                .atMost(2);

        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testWithUniProducingNullWithSharedState() {
        Supplier<AtomicInteger> boom = () -> null;

        Multi<Integer> multi = Multi.createBy().repeating()
                .uni(boom, (state) -> Uni.createFrom().item(state.incrementAndGet()))
                .atMost(2);

        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertFailedWith(NullPointerException.class, "supplier");
    }

    @Test
    public void testThatStateSupplierCannotBeNullWithUniWithSharedState() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createBy().repeating().uni(null,
                (x, emitter) -> {
                }));
    }

    @Test
    public void testThatFunctionCannotBeNullWithUniWithSharedState() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createBy().repeating().uni(() -> "hello", (Function<String, Uni<? extends String>>) null));
    }

    @Test
    public void testWithCompletionStageWithSharedState() {
        AtomicInteger shared = new AtomicInteger();
        Multi<Integer> multi = Multi.createBy().repeating()
                .completionStage(() -> shared, (state) -> CompletableFuture.completedFuture(state.incrementAndGet()))
                .atMost(2);

        assertThat(shared).hasValue(0);
        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertItems(1);
        assertThat(shared).hasValue(1);
        subscriber.request(1);
        subscriber.assertCompleted().assertItems(1, 2);
    }

    @Test
    public void testWithCompletionStage() {
        AtomicInteger shared = new AtomicInteger();
        Multi<Integer> multi = Multi.createBy().repeating()
                .completionStage(() -> CompletableFuture.completedFuture(shared.incrementAndGet()))
                .atMost(2);

        assertThat(shared).hasValue(0);
        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertItems(1);
        assertThat(shared).hasValue(1);
        subscriber.request(1);
        subscriber.assertCompleted().assertItems(1, 2);
    }

    @Test
    public void testWithCompletionStageProducingFailureWithSharedState() {
        Supplier<AtomicInteger> boom = () -> {
            throw new IllegalStateException("boom");
        };

        Multi<Integer> multi = Multi.createBy().repeating()
                .completionStage(boom, (state) -> CompletableFuture.completedFuture(state.incrementAndGet()))
                .atMost(2);

        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithCompletionStageProducingFailure() {
        Multi<Integer> multi = Multi.createBy().repeating()
                .<Integer> completionStage(() -> {
                    throw new IllegalStateException("boom");
                })
                .atMost(2);

        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithCompletionStageProducingNullWithSharedState() {
        Supplier<AtomicInteger> boom = () -> null;

        Multi<Integer> multi = Multi.createBy().repeating()
                .completionStage(boom, (state) -> CompletableFuture.completedFuture(state.incrementAndGet()))
                .atMost(2);

        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertFailedWith(NullPointerException.class, "supplier");
    }

    @Test
    public void testWithCompletionStageProducingNull() {
        Multi<Integer> multi = Multi.createBy().repeating()
                .<Integer> completionStage(() -> CompletableFuture.completedFuture(null))
                .atMost(2);

        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(2));

        subscriber.assertTerminated().assertCompleted().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testThatStateSupplierCannotBeNullWithCompletionStageWithSharedState() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createBy().repeating().completionStage(null,
                x -> CompletableFuture.completedFuture(1)));
    }

    @Test
    public void testThatFunctionCannotBeNullWithCompletionStageWithSharedState() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createBy().repeating().completionStage(() -> "hello",
                (Function<String, CompletableFuture<? extends String>>) null));

    }

    @Test
    public void testWithItemWithSharedState() {
        AtomicInteger shared = new AtomicInteger();
        Multi<Integer> multi = Multi.createBy().repeating()
                .supplier(() -> shared,
                        (state) -> shared.incrementAndGet())
                .atMost(2);

        assertThat(shared).hasValue(0);
        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertItems(1);
        assertThat(shared).hasValue(1);
        subscriber.request(1);
        subscriber.assertCompleted().assertItems(1, 2);
    }

    @Test
    public void testWithItem() {
        AtomicInteger shared = new AtomicInteger();
        Multi<Integer> multi = Multi.createBy().repeating()
                .supplier(shared::incrementAndGet)
                .atMost(2);

        assertThat(shared).hasValue(0);
        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertItems(1);
        assertThat(shared).hasValue(1);
        subscriber.request(1);
        subscriber.assertCompleted().assertItems(1, 2);
    }

}
