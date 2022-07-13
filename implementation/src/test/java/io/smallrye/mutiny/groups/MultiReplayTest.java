package io.smallrye.mutiny.groups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.MultiSubscriber;

class MultiReplayTest {

    private final Random random = new Random();

    @Test
    void rejectBadBuilderArguments() {
        assertThatThrownBy(() -> Multi.createBy().replaying().ofMulti(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be `null`");

        assertThatThrownBy(() -> Multi.createBy().replaying().ofSeedAndMulti(null, Multi.createFrom().item(123)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be `null`");

        assertThatThrownBy(() -> Multi.createBy().replaying().ofSeedAndMulti(new ArrayList<Integer>(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be `null`");

        assertThatThrownBy(() -> Multi.createBy().replaying().upTo(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be greater than zero");

        assertThatThrownBy(() -> Multi.createBy().replaying().upTo(-10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be greater than zero");
    }

    @Test
    void basicReplayAll() {
        Multi<Integer> upstream = Multi.createFrom().range(1, 10);
        Multi<Integer> replay = Multi.createBy().replaying().ofMulti(upstream);

        AssertSubscriber<Integer> sub = replay.subscribe().withSubscriber(AssertSubscriber.create());
        sub.request(Long.MAX_VALUE);
        assertThat(sub.getItems()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
        sub.assertCompleted();

        sub = replay.subscribe().withSubscriber(AssertSubscriber.create());
        sub.request(4);
        assertThat(sub.getItems()).containsExactly(1, 2, 3, 4);
        sub.request(Long.MAX_VALUE);
        assertThat(sub.getItems()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
        sub.assertCompleted();
    }

    @Test
    void basicReplayLatest3() {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        AtomicBoolean step = new AtomicBoolean();

        Multi<Integer> upstream = Multi.createFrom().<Integer> emitter(emitter -> {
            await().untilTrue(step);
            for (int i = 0; i <= 10; i++) {
                emitter.emit(i);
            }
            emitter.complete();
        }).runSubscriptionOn(pool);
        Multi<Integer> replay = Multi.createBy().replaying().upTo(3).ofMulti(upstream);

        try {
            AssertSubscriber<Integer> sub = replay.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
            step.set(true);
            sub.awaitCompletion();
            assertThat(sub.getItems()).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

            sub = replay.subscribe().withSubscriber(AssertSubscriber.create());
            sub.request(1);
            assertThat(sub.getItems()).containsExactly(8);
            sub.request(1);
            assertThat(sub.getItems()).containsExactly(8, 9);
            sub.request(3000);
            assertThat(sub.getItems()).containsExactly(8, 9, 10);
            sub.assertCompleted();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void replayLast3AfterFailure() {
        Multi<Integer> upstream = Multi.createBy().concatenating().streams(
                Multi.createFrom().range(1, 10),
                Multi.createFrom().failure(() -> new IOException("boom")));
        Multi<Integer> replay = Multi.createBy().replaying().upTo(3).ofMulti(upstream);

        AssertSubscriber<Integer> sub = replay.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        sub.assertFailedWith(IOException.class, "boom");
        sub.assertItems(7, 8, 9);

        sub = replay.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        sub.assertFailedWith(IOException.class, "boom");
        sub.assertItems(7, 8, 9);
    }

    @Test
    void replayWithSeed() {
        List<Integer> seed = Arrays.asList(-100, -10, -1);
        Multi<Integer> upstream = Multi.createFrom().range(0, 11);
        Multi<Integer> replay = Multi.createBy().replaying().ofSeedAndMulti(seed, upstream);

        AssertSubscriber<Integer> sub = replay.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        sub.assertCompleted();
        assertThat(sub.getItems()).containsExactly(-100, -10, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    void forbidSeedWithNull() {
        List<Integer> seed = Arrays.asList(-100, -10, -1, null);
        Multi<Integer> upstream = Multi.createFrom().range(0, 11);
        assertThatThrownBy(() -> Multi.createBy().replaying().ofSeedAndMulti(seed, upstream))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("`item` must not be `null`");
    }

    @Test
    void rejectBadRequests() {
        List<Integer> seed = Arrays.asList(-100, -10, -1);
        Multi<Integer> upstream = Multi.createFrom().range(0, 11);
        Multi<Integer> replay = Multi.createBy().replaying().ofSeedAndMulti(seed, upstream);

        DirectSubscriber<Integer> sub = replay.subscribe().withSubscriber(new DirectSubscriber<>());
        sub.request(-1);
        assertThat(sub.failure).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid request number, must be greater than 0");

        sub = replay.subscribe().withSubscriber(new DirectSubscriber<>());
        sub.request(0);
        assertThat(sub.failure).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid request number, must be greater than 0");
    }

    @Test
    void acceptContext() {
        Multi<String> upstream = Multi.createFrom().range(1, 10)
                .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.get("foo")));
        Multi<String> replay = Multi.createBy().replaying().upTo(3).ofMulti(upstream);

        AssertSubscriber<String> sub = replay.subscribe()
                .withSubscriber(AssertSubscriber.create(Context.of("foo", "foo-bar"), Long.MAX_VALUE));
        sub.assertCompleted();
        assertThat(sub.getItems()).containsExactly("7::foo-bar", "8::foo-bar", "9::foo-bar");

        sub = replay.subscribe().withSubscriber(AssertSubscriber.create(Context.of("foo", "foo-bar-baz"), Long.MAX_VALUE));
        sub.assertCompleted();
        assertThat(sub.getItems()).containsExactly("7::foo-bar", "8::foo-bar", "9::foo-bar");
    }

    @Test
    void acceptCancellation() {
        Multi<Integer> replay = Multi.createBy().replaying().ofMulti(Multi.createFrom().range(1, 10));

        DirectSubscriber<Integer> sub = replay.subscribe().withSubscriber(new DirectSubscriber<>());
        sub.request(Long.MAX_VALUE);
        assertThat(sub.completed).isTrue();
        assertThat(sub.items).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);

        sub = replay.subscribe().withSubscriber(new DirectSubscriber<>());
        sub.request(3);
        assertThat(sub.completed).isFalse();
        assertThat(sub.items).containsExactly(1, 2, 3);
        sub.cancel();
        sub.request(Long.MAX_VALUE);
        assertThat(sub.completed).isFalse();
        assertThat(sub.items).containsExactly(1, 2, 3);

        sub = replay.subscribe().withSubscriber(new DirectSubscriber<>());
        sub.request(5);
        assertThat(sub.completed).isFalse();
        assertThat(sub.items).containsExactly(1, 2, 3, 4, 5);
        sub.request(1);
        sub.cancel();
        sub.request(Long.MAX_VALUE);
        assertThat(sub.completed).isFalse();
        assertThat(sub.items).containsExactly(1, 2, 3, 4, 5, 6);
    }

    // Avoid wrapping that does not allow us to test cancellation and requests directly on the operator
    static class DirectSubscriber<T> implements MultiSubscriber<T> {

        private final ArrayList<T> items = new ArrayList<>();
        private Throwable failure;
        private boolean completed;
        private Subscription subscription;

        @Override
        public void onItem(T item) {
            items.add(item);
        }

        @Override
        public void onFailure(Throwable failure) {
            this.failure = failure;
        }

        @Override
        public void onCompletion() {
            completed = true;
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
        }

        public void request(long demand) {
            subscription.request(demand);
        }

        public void cancel() {
            subscription.cancel();
        }
    }

    @Test
    void raceBetweenPushAndCancel() throws InterruptedException, TimeoutException {
        ExecutorService pool = Executors.newCachedThreadPool();
        try {

            final int N = 32;
            CountDownLatch startLatch = new CountDownLatch(N);
            CountDownLatch endLatch = new CountDownLatch(N);

            Multi<Long> upstream = Multi.createFrom().<Long> emitter(emitter -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    emitter.fail(e);
                }
                long i = 0;
                while (endLatch.getCount() != 0) {
                    emitter.emit(i++);
                }
                emitter.complete();
            }).runSubscriptionOn(pool);

            Multi<Long> replay = Multi.createBy().replaying().ofMulti(upstream)
                    .runSubscriptionOn(pool);

            CopyOnWriteArrayList<List<Long>> items = new CopyOnWriteArrayList<>();
            for (int i = 0; i < N; i++) {
                AssertSubscriber<Long> sub = replay.subscribe().withSubscriber(AssertSubscriber.create());
                pool.submit(() -> {
                    startLatch.countDown();
                    randomSleep();
                    sub.request(Long.MAX_VALUE);
                    randomSleep();
                    sub.cancel();
                    items.add(sub.getItems());
                    endLatch.countDown();
                });
            }

            if (!endLatch.await(10, TimeUnit.SECONDS)) {
                throw new TimeoutException("The test did not finish within 10 seconds");
            }

            assertThat(items).hasSize(N);
            items.forEach(list -> {
                if (list.isEmpty()) {
                    // Might happen due to subscriber timing
                    return;
                }
                assertThat(list).isNotEmpty();
                AtomicLong prev = new AtomicLong(list.get(0));
                list.stream().skip(1).forEach(n -> {
                    assertThat(n).isEqualTo(prev.get() + 1);
                    prev.set(n);
                });
            });
        } finally {
            pool.shutdownNow();
        }
    }

    private void randomSleep() {
        try {
            Thread.sleep(250 + random.nextInt(250));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
