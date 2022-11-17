package io.smallrye.mutiny.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.queues.SpscArrayQueue;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class BlockingIterableTest {

    @Test
    public void testToIterable() {
        List<Integer> values = new ArrayList<>();

        for (Integer i : Multi.createFrom().range(1, 11).subscribe().asIterable()) {
            values.add(i);
        }

        assertThat(values).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        values.clear();

        Multi.createFrom().range(1, 11).subscribe().asIterable().spliterator().forEachRemaining(values::add);
        assertThat(values).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testToIterableWithBufferSizeAndSupplier() {
        Queue<Integer> q = new ArrayBlockingQueue<>(1);
        List<Integer> values = new ArrayList<>();

        for (Integer i : Multi.createFrom().range(1, 11).subscribe().asIterable(1, () -> q)) {
            values.add(i);
        }

        assertThat(values).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testToIterableWithEmptyStream() {
        List<Integer> values = new ArrayList<>();

        for (Integer i : Multi.createFrom().<Integer> empty().subscribe().asIterable()) {
            values.add(i);
        }

        assertThat(values).isEmpty();
    }

    @Test
    public void testToIterableWithUpstreamFailure() {
        assertThrows(RuntimeException.class, () -> {
            List<Integer> values = new ArrayList<>();

            for (Integer i : Multi.createFrom().<Integer> failure(new RuntimeException("boom"))
                    .subscribe().asIterable()) {
                values.add(i);
            }

            assertThat(values).isEmpty();
        });
    }

    @Test
    public void testToStream() {
        List<Integer> values = new ArrayList<>();

        Multi.createFrom().range(1, 11)
                .subscribe().asStream()
                .forEach(values::add);

        assertThat(values).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testToStreamWithEmptyStream() {
        List<Integer> values = new ArrayList<>();
        Multi.createFrom().<Integer> empty().subscribe().asStream().forEach(values::add);
        assertThat(values).isEmpty();
    }

    @Test
    public void testCancellationOnClose() {
        List<Integer> values = new ArrayList<>();

        Stream<Integer> stream = Multi.createFrom().range(1, Integer.MAX_VALUE)
                .subscribe().asStream();
        stream.limit(10).forEach(values::add);
        stream.close();

        assertThat(values).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testParallelStreamComputation() {
        int n = 10_000;

        Optional<Integer> opt = Multi.createFrom().range(1, n)
                .subscribe().asStream()
                .parallel()
                .max(Integer::compare);
        assertThat(opt).hasValue(n - 1);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testToStreamWithFailure() {
        Multi<Integer> multi = Multi.createFrom().<Integer> emitter(e -> e.emit(1).emit(0).complete())
                .map(v -> 4 / v);

        assertThatThrownBy(() -> multi.subscribe().asStream().collect(Collectors.toList()))
                .isInstanceOf(ArithmeticException.class).hasMessageContaining("by zero");
    }

    @Test
    public void testToIterableWithFailure() {
        Multi<Integer> multi = Multi.createFrom().<Integer> emitter(e -> e.emit(1).emit(0).complete())
                .map(v -> 4 / v);

        assertThatThrownBy(() -> multi.subscribe().asIterable().forEach(i -> {
        })).isInstanceOf(ArithmeticException.class).hasMessageContaining("by zero");
    }

    @Test
    public void testToIterableWithCheckedFailure() {
        Multi<Integer> multi = Multi.createFrom().emitter(e -> e.emit(1).emit(0).fail(new IOException("boom")));

        assertThatThrownBy(() -> multi.subscribe().asIterable().forEach(i -> {
        })).isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasMessageContaining("boom");
    }

    @Test
    public void testQueueSupplierFailing() {
        assertThatThrownBy(() -> Multi.createFrom().items(1, 2, 3, 4, 5, 6)
                .subscribe().asIterable(10, () -> {
                    throw new IllegalArgumentException("boom");
                }).forEach(i -> {
                    // noop - the iterable is created lazily.
                })).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("boom");

    }

    @Test
    public void testQueueSupplierReturningNull() {
        assertThatThrownBy(() -> Multi.createFrom().items(1, 2, 3, 4, 5, 6)
                .subscribe().asIterable(10, () -> null).forEach(i -> {
                    // noop - the iterable is created lazily.
                })).isInstanceOf(IllegalStateException.class);

    }

    @Test
    public void testWaitingInterrupted() {
        List<Integer> values = new ArrayList<>();
        AtomicBoolean after = new AtomicBoolean();
        AtomicBoolean cancelled = new AtomicBoolean();

        Thread thread = new Thread(() -> {
            try {
                Multi.createFrom().<Integer> emitter(e -> e
                        .onTermination(() -> cancelled.set(true))
                        .emit(1))
                        .subscribe().asIterable()
                        .forEach(values::add);
            } catch (Throwable e) {
                assertThat(e).isInstanceOf(RuntimeException.class)
                        .hasCauseInstanceOf(InterruptedException.class);
                after.set(true);
            }
        });
        thread.start();

        await().until(() -> values.size() == 1);
        thread.interrupt();
        await().untilTrue(after);
        assertThat(cancelled).isTrue();
    }

    @Test
    public void testWithNullValues() {
        assertThrows(NullPointerException.class, () -> {
            Multi<Integer> rogue = new AbstractMulti<Integer>() {
                @Override
                public void subscribe(Subscriber<? super Integer> subscriber) {
                    subscriber.onNext(1);
                    subscriber.onNext(2);
                    subscriber.onNext(3);
                    subscriber.onNext(null);
                }
            };
            BlockingIterable<Integer> integers = rogue.subscribe().asIterable();
            integers.forEach(i -> assertThat(i).isPositive());
        });
    }

    @Test
    public void testOverflow() {
        assertThrows(BackPressureFailure.class, () -> {
            BlockingIterable<Integer> integers = Multi.createFrom()
                    .<Integer> emitter(e -> e.emit(1).emit(2).emit(3).emit(4).emit(5))
                    .subscribe().asIterable(10, () -> new SpscArrayQueue<>(4));
            integers.forEach(i -> assertThat(i).isPositive());
        });
    }

    @Nested
    @ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ_WRITE)
    class ThreadBlockingTest {

        @BeforeEach
        void reset() {
            Infrastructure.resetCanCallerThreadBeBlockedSupplier();
        }

        @Test
        void defaultBlocking() throws InterruptedException {
            Multi<Integer> empty = Multi.createFrom().emitter(e -> {
                // Never emit anything to block
            });
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            AtomicBoolean hasNext = new AtomicBoolean();

            Thread thread = new Thread(() -> {
                try {
                    hasNext.set(empty.subscribe().asIterable().iterator().hasNext());
                } catch (Throwable err) {
                    error.set(err);
                }
                latch.countDown();
            }, "my-thread");

            thread.start();
            boolean completedEarly = latch.await(1, TimeUnit.SECONDS);

            assertThat(completedEarly).isFalse();
            assertThat(hasNext).isFalse();
            assertThat(error.get()).isNull();
        }

        @Test
        void defaultBlockingAllow() throws InterruptedException {
            Infrastructure.setCanCallerThreadBeBlockedSupplier(() -> !Thread.currentThread().getName().contains("-forbidden-"));

            Multi<Integer> empty = Multi.createFrom().emitter(e -> {
                // Never emit anything to block
            });
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            AtomicBoolean hasNext = new AtomicBoolean();

            Thread thread = new Thread(() -> {
                try {
                    hasNext.set(empty.subscribe().asIterable().iterator().hasNext());
                } catch (Throwable err) {
                    error.set(err);
                }
                latch.countDown();
            }, "my-thread");

            thread.start();
            boolean completedEarly = latch.await(1, TimeUnit.SECONDS);

            assertThat(completedEarly).isFalse();
            assertThat(hasNext).isFalse();
            assertThat(error.get()).isNull();
        }

        @Test
        void defaultBlockingForbid() throws InterruptedException {
            Infrastructure.setCanCallerThreadBeBlockedSupplier(() -> !Thread.currentThread().getName().contains("-forbidden-"));

            Multi<Integer> empty = Multi.createFrom().emitter(e -> {
                // Never emit anything to block
            });
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            AtomicBoolean hasNext = new AtomicBoolean();

            Thread thread = new Thread(() -> {
                try {
                    hasNext.set(empty.subscribe().asIterable().iterator().hasNext());
                } catch (Throwable err) {
                    error.set(err);
                }
                latch.countDown();
            }, "my-forbidden-thread");

            thread.start();
            boolean completedEarly = latch.await(1, TimeUnit.SECONDS);

            assertThat(completedEarly).isTrue();
            assertThat(hasNext).isFalse();
            assertThat(error.get())
                    .isNotNull()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("The current thread cannot be blocked: my-forbidden-thread");
        }
    }
}
