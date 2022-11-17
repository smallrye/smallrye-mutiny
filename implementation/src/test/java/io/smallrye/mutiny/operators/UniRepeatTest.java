package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.test.AssertSubscriber.DEFAULT_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.test.Mocks;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class UniRepeatTest {

    @RepeatedTest(10)
    public void testRepeatAtMost() {
        List<Integer> list = Uni.createFrom().item(1)
                .repeat().atMost(3)
                .collect().asList()
                .await().atMost(Duration.ofSeconds(5));
        assertThat(list).hasSize(3).contains(1, 1, 1);
    }

    @RepeatedTest(10)
    public void testRepeatAtMostWithDelay() {
        List<Long> times = new ArrayList<>();
        Uni<Integer> uni = Uni.createFrom().item(1)
                .onItem().invoke(() -> times.add(System.currentTimeMillis()));
        Duration delay = Duration.ofMillis(100);
        List<Integer> list = uni
                .repeat().withDelay(delay)
                .atMost(3)
                .collect().asList()
                .await().atMost(Duration.ofSeconds(5));
        assertThat(list).hasSize(3).contains(1, 1, 1);

        assertThat(times).hasSize(3);
        assertThat(times.get(0) + delay.toMillis()).isLessThanOrEqualTo(times.get(1));
        assertThat(times.get(1) + delay.toMillis()).isLessThanOrEqualTo(times.get(2));
    }

    @RepeatedTest(10)
    public void testRepeatUntil() {
        List<String> items = Arrays.asList("a", "b", "c", "d", "e", "f");
        Iterator<String> iterator = items.iterator();
        List<String> list = Uni.createFrom().item(iterator::next)
                .repeat().until(v -> v.equalsIgnoreCase("d"))
                .collect().asList()
                .await().atMost(Duration.ofSeconds(5));
        assertThat(list).hasSize(3).contains("a", "b", "c");
    }

    @RepeatedTest(10)
    public void testRepeatUntilWithDelay() {
        List<Long> times = new ArrayList<>();
        List<String> items = Arrays.asList("a", "b", "c", "d", "e", "f");
        Iterator<String> iterator = items.iterator();
        Uni<String> uni = Uni.createFrom().item(iterator::next)
                .onItem().invoke(() -> times.add(System.currentTimeMillis()));
        Duration delay = Duration.ofMillis(100);

        List<String> list = uni
                .repeat().withDelay(delay).until(v -> v.equalsIgnoreCase("d"))
                .collect().asList()
                .await().atMost(Duration.ofSeconds(5));
        assertThat(list).hasSize(3).contains("a", "b", "c");
        assertThat(times).hasSize(4); // d has been emitted.
        assertThat(times.get(0) + delay.toMillis()).isLessThanOrEqualTo(times.get(1));
        assertThat(times.get(1) + delay.toMillis()).isLessThanOrEqualTo(times.get(2));
        assertThat(times.get(2) + delay.toMillis()).isLessThanOrEqualTo(times.get(3));
    }

    @RepeatedTest(10)
    public void testRepeatWhilst() {
        Page page1 = new Page(Arrays.asList(1, 2, 3), 1);
        Page page2 = new Page(Arrays.asList(4, 5, 6), 2);
        Page page3 = new Page(Arrays.asList(7, 8), -1);

        Page[] pages = new Page[] { page1, page2, page3 };
        AtomicInteger cursor = new AtomicInteger();

        AssertSubscriber<Integer> subscriber = Multi.createBy().repeating()
                .uni(() -> Uni.createFrom().item(pages[cursor.getAndIncrement()])).whilst(p -> p.next != -1)
                .onItem().transformToMulti(p -> Multi.createFrom().iterable(p.items)).concatenate()
                .subscribe().withSubscriber(AssertSubscriber.create(50));

        subscriber.assertCompleted()
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @RepeatedTest(10)
    public void testRepeatWhilstWithDelay() {
        List<Long> times = new ArrayList<>();
        Duration delay = Duration.ofMillis(100);

        Page page1 = new Page(Arrays.asList(1, 2, 3), 1);
        Page page2 = new Page(Arrays.asList(4, 5, 6), 2);
        Page page3 = new Page(Arrays.asList(7, 8), -1);

        Page[] pages = new Page[] { page1, page2, page3 };
        AtomicInteger cursor = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Multi.createBy().repeating()
                .uni(() -> Uni.createFrom().item(pages[cursor.getAndIncrement()]).onItem()
                        .invoke(() -> times.add(System.currentTimeMillis())))
                .withDelay(delay).whilst(p -> p.next != -1)
                .onItem().transformToMulti(p -> Multi.createFrom().iterable(p.items)).concatenate()
                .subscribe().withSubscriber(AssertSubscriber.create(50));

        subscriber.awaitCompletion(DEFAULT_TIMEOUT)
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8);

        assertThat(times).hasSize(3);
        for (int i = 0; i < times.size() - 1; i++) {
            assertThat(times.get(i) + delay.toMillis()).isLessThanOrEqualTo(times.get(i + 1));
        }
    }

    @Test
    public void testRepeat0() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(0)
                .repeat().atMost(0)
                .collect().asList()
                .await().indefinitely());
    }

    @Test
    public void testRepeatUntilWithNullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(0)
                .repeat().until(null)
                .collect().asList()
                .await().indefinitely());
    }

    @Test
    public void testRepeatWhilstWithNullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(0)
                .repeat().whilst(null)
                .collect().asList()
                .await().indefinitely());
    }

    @RepeatedTest(10)
    public void testRepeat1() {
        AtomicInteger count = new AtomicInteger();
        List<Integer> list = Uni.createFrom().item(count::getAndIncrement)
                .repeat().atMost(1)
                .collect().asList()
                .await().atMost(Duration.ofSeconds(5));

        assertThat(list).containsExactly(0);
        assertThat(count).hasValue(1);
    }

    @RepeatedTest(10)
    public void testRepeatUntilOnlyOnce() {
        AtomicInteger count = new AtomicInteger();
        AtomicBoolean once = new AtomicBoolean();
        List<Integer> list = Uni.createFrom().item(count::getAndIncrement)
                .repeat().until(x -> once.getAndSet(true))
                .collect().asList()
                .await().atMost(Duration.ofSeconds(5));

        assertThat(list).containsExactly(0);
        assertThat(count).hasValue(2); // the first element and the element breaking the loop.
    }

    @RepeatedTest(10)
    public void testRepeatWhilstOnlyOnce() {
        AtomicInteger count = new AtomicInteger();
        AtomicBoolean once = new AtomicBoolean(true);
        List<Integer> list = Uni.createFrom().item(count::getAndIncrement)
                .repeat().whilst(x -> once.getAndSet(false))
                .collect().asList()
                .await().atMost(Duration.ofSeconds(5));

        assertThat(list).containsExactly(0, 1);
        assertThat(count).hasValue(2);
    }

    @RepeatedTest(10)
    public void testNoRepeatUntil() {
        AtomicInteger count = new AtomicInteger();
        List<Integer> list = Uni.createFrom().item(count::getAndIncrement)
                .repeat().until(x -> true)
                .collect().asList()
                .await().atMost(Duration.ofSeconds(5));

        assertThat(list).isEmpty();
        assertThat(count).hasValue(1);
    }

    @RepeatedTest(10)
    public void testNoRepeatWhilst() {
        AtomicInteger count = new AtomicInteger();
        List<Integer> list = Uni.createFrom().item(count::getAndIncrement)
                .repeat().whilst(x -> false)
                .collect().asList()
                .await().atMost(Duration.ofSeconds(5));

        assertThat(list).containsExactly(0);
        assertThat(count).hasValue(1);
    }

    @RepeatedTest(10)
    public void testRepeatCancelledWithTake() {
        int num = 10;
        final AtomicInteger count = new AtomicInteger();
        int value = Uni.createFrom().item(count::incrementAndGet)
                .repeat().indefinitely()
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .select().first(num)
                .collect().last()
                .await().atMost(Duration.ofSeconds(5));
        assertThat(num).isEqualTo(value);
        assertThat(count).hasValue(value);
    }

    @RepeatedTest(10)
    public void testRepeatUntilCancelledWithTake() {
        int num = 10;
        final AtomicInteger invocations = new AtomicInteger();
        final AtomicInteger count = new AtomicInteger();
        int value = Uni.createFrom().item(count::incrementAndGet)
                .repeat().until(x -> {
                    invocations.incrementAndGet();
                    return false;
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .select().first(num)
                .collect().last()
                .await().atMost(Duration.ofSeconds(5));
        assertThat(num).isEqualTo(value);
        assertThat(count).hasValue(value);
        assertThat(invocations).hasValue(value);
    }

    @RepeatedTest(10)
    public void testRepeatWhilstCancelledWithTake() {
        int num = 10;
        final AtomicInteger invocations = new AtomicInteger();
        final AtomicInteger count = new AtomicInteger();
        int value = Uni.createFrom().item(count::incrementAndGet)
                .repeat().whilst(x -> {
                    invocations.incrementAndGet();
                    return true;
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .select().first(num)
                .collect().last()
                .await().atMost(Duration.ofSeconds(5));
        assertThat(num).isEqualTo(value);
        assertThat(count).hasValue(value);
        assertThat(invocations).hasValue(value);
    }

    @RepeatedTest(10)
    public void testNoStackOverflow() {
        int value = Uni.createFrom().item(1).repeat().indefinitely()
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .select().first(100000)
                .collect().last()
                .await().atMost(Duration.ofSeconds(5));
        assertThat(value).isEqualTo(1);
    }

    @RepeatedTest(10)
    public void testNoStackOverflowWithRepeatUntil() {
        AtomicInteger count = new AtomicInteger();
        int value = Uni.createFrom().item(1).repeat().until(x -> count.incrementAndGet() > 100000000L)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .select().first(100000)
                .collect().last()
                .await().atMost(Duration.ofSeconds(5));
        assertThat(value).isEqualTo(1);
    }

    @RepeatedTest(10)
    public void testNoStackOverflowWithRepeatWhilst() {
        AtomicInteger count = new AtomicInteger();
        int value = Uni.createFrom().item(1).repeat().whilst(x -> count.incrementAndGet() < 100000000L)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .select().first(100000)
                .collect().last()
                .await().atMost(Duration.ofSeconds(5));
        assertThat(value).isEqualTo(1);
    }

    @RepeatedTest(10)
    public void testNumberOfRepeat() {
        Subscriber<Integer> subscriber = Mocks.subscriber();

        Uni.createFrom().item(1).repeat().indefinitely()
                .select().first(10)
                .subscribe(subscriber);

        verify(subscriber, times(10)).onNext(1);
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @RepeatedTest(10)
    public void testFailurePropagation() {
        Subscriber<Integer> subscriber = Mocks.subscriber();

        Uni.createFrom().<Integer> failure(() -> new IOException("boom")).repeat().indefinitely()
                .select().first(10)
                .subscribe(subscriber);

        verify(subscriber).onError(any(IOException.class));
        verify(subscriber, never()).onComplete();
        verify(subscriber, never()).onNext(any());
    }

    @RepeatedTest(10)
    public void testFailurePropagationWithRepeatUntil() {
        Subscriber<Integer> subscriber = Mocks.subscriber();

        Uni.createFrom().<Integer> failure(() -> new IOException("boom")).repeat().until(x -> false)
                .select().first(10)
                .subscribe(subscriber);

        verify(subscriber).onError(any(IOException.class));
        verify(subscriber, never()).onComplete();
        verify(subscriber, never()).onNext(any());
    }

    @RepeatedTest(10)
    public void testFailurePropagationWithRepeatWhilst() {
        Subscriber<Integer> subscriber = Mocks.subscriber();

        Uni.createFrom().<Integer> failure(() -> new IOException("boom")).repeat().whilst(x -> true)
                .select().first(10)
                .subscribe(subscriber);

        verify(subscriber).onError(any(IOException.class));
        verify(subscriber, never()).onComplete();
        verify(subscriber, never()).onNext(any());
    }

    @RepeatedTest(10)
    public void testRequestAndCancellation() {
        final AtomicInteger count = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Uni.createFrom().item(count::incrementAndGet)
                .repeat().indefinitely()
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .subscribe().withSubscriber(AssertSubscriber.create());

        await().untilAsserted(subscriber::assertSubscribed);
        subscriber.assertSubscribed().assertHasNotReceivedAnyItem();
        subscriber
                .request(2)
                .run(() -> {
                    await().until(() -> subscriber.getItems().size() == 2);
                    assertThat(subscriber.getItems()).containsExactly(1, 2);
                    assertThat(count).hasValue(2);
                })
                .request(1)
                .run(() -> {
                    await().until(() -> subscriber.getItems().size() == 3);
                    assertThat(subscriber.getItems()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                })
                .cancel()
                .run(() -> {
                    await().until(() -> subscriber.getItems().size() == 3);
                    assertThat(subscriber.getItems()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                })
                .request(20)
                .run(() -> {
                    await().until(() -> subscriber.getItems().size() == 3);
                    assertThat(subscriber.getItems()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                });
    }

    @RepeatedTest(10)
    public void testRequestAndCancellationWithRepeatUntil() {
        final AtomicInteger count = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Uni.createFrom().item(count::incrementAndGet)
                .repeat().until(x -> false)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .subscribe().withSubscriber(AssertSubscriber.create());

        await().untilAsserted(subscriber::assertSubscribed);
        subscriber.assertSubscribed().assertHasNotReceivedAnyItem();
        subscriber
                .request(2)
                .run(() -> {
                    await().until(() -> subscriber.getItems().size() == 2);
                    assertThat(subscriber.getItems()).containsExactly(1, 2);
                    assertThat(count).hasValue(2);
                })
                .request(1)
                .run(() -> {
                    await().until(() -> subscriber.getItems().size() == 3);
                    assertThat(subscriber.getItems()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                })
                .cancel()
                .run(() -> {
                    await().until(() -> subscriber.getItems().size() == 3);
                    assertThat(subscriber.getItems()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                })
                .request(20)
                .run(() -> {
                    await().until(() -> subscriber.getItems().size() == 3);
                    assertThat(subscriber.getItems()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                });
    }

    @RepeatedTest(10)
    public void testRequestAndCancellationWithRepeatWhilst() {
        final AtomicInteger count = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Uni.createFrom().item(count::incrementAndGet)
                .repeat().whilst(x -> true)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .subscribe().withSubscriber(AssertSubscriber.create());

        await().untilAsserted(subscriber::assertSubscribed);
        subscriber.assertSubscribed().assertHasNotReceivedAnyItem();
        subscriber
                .request(2)
                .run(() -> {
                    await().until(() -> subscriber.getItems().size() == 2);
                    assertThat(subscriber.getItems()).containsExactly(1, 2);
                    assertThat(count).hasValue(2);
                })
                .request(1)
                .run(() -> {
                    await().until(() -> subscriber.getItems().size() == 3);
                    assertThat(subscriber.getItems()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                })
                .cancel()
                .run(() -> {
                    await().until(() -> subscriber.getItems().size() == 3);
                    assertThat(subscriber.getItems()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                })
                .request(20)
                .run(() -> {
                    await().until(() -> subscriber.getItems().size() == 3);
                    assertThat(subscriber.getItems()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                });
    }

    @RepeatedTest(10)
    public void testRequestWithAtMost() {
        final AtomicInteger count = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Uni.createFrom().item(count::incrementAndGet)
                .repeat().atMost(3)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .subscribe().withSubscriber(AssertSubscriber.create());

        await().untilAsserted(subscriber::assertSubscribed);
        subscriber.assertSubscribed().assertHasNotReceivedAnyItem();
        subscriber
                .request(2)
                .run(() -> {
                    await().until(() -> subscriber.getItems().size() == 2);
                    assertThat(subscriber.getItems()).containsExactly(1, 2);
                    assertThat(count).hasValue(2);
                })
                .request(20)
                .run(() -> {
                    await().until(() -> subscriber.getItems().size() == 3);
                    assertThat(subscriber.getItems()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                })
                .assertCompleted();
    }

    @RepeatedTest(10)
    public void testFailurePropagationAfterFewRepeats() {
        AtomicInteger count = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> {
            int v = count.incrementAndGet();
            if (v == 3) {
                throw new IllegalStateException("boom");
            }
            return v;
        })
                .repeat().indefinitely()
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(10)
                .awaitFailure()
                .assertItems(1, 2)
                .assertFailedWith(IllegalStateException.class, "boom");
        assertThat(subscriber.getItems()).hasSize(2);
    }

    @RepeatedTest(10)
    public void testFailurePropagationAfterFewRepeatsWithRepeatUntil() {
        AtomicInteger count = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> {
            int v = count.incrementAndGet();
            if (v == 3) {
                throw new IllegalStateException("boom");
            }
            return v;
        })
                .repeat().until(x -> false)
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(10)
                .awaitFailure()
                .assertItems(1, 2)
                .assertFailedWith(IllegalStateException.class, "boom");
        assertThat(subscriber.getItems()).hasSize(2);
    }

    @RepeatedTest(10)
    public void testFailurePropagationAfterFewRepeatsWithRepeatWhilst() {
        AtomicInteger count = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> {
            int v = count.incrementAndGet();
            if (v == 3) {
                throw new IllegalStateException("boom");
            }
            return v;
        })
                .repeat().whilst(x -> true)
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(10)
                .awaitFailure()
                .assertItems(1, 2)
                .assertFailedWith(IllegalStateException.class, "boom");
        assertThat(subscriber.getItems()).hasSize(2);
    }

    @RepeatedTest(10)
    public void testFailurePropagationAfterMaxRepeats() {
        AtomicInteger count = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> {
            int v = count.incrementAndGet();
            if (v == 3) {
                throw new IllegalStateException("boom");
            }
            return v;
        })
                .repeat().atMost(2)
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(10)
                .awaitCompletion()
                .assertItems(1, 2);
        assertThat(subscriber.getItems()).hasSize(2);
    }

    @RepeatedTest(10)
    public void testEmptyUniOnceInAWhileWithAtMost() {
        AtomicInteger count = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> {
            int v = count.incrementAndGet();
            if (v % 3 == 0) {
                return null;
            }
            return v;
        })
                .repeat().atMost(10)
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(100)
                .awaitCompletion()
                .assertItems(1, 2, 4, 5, 7, 8, 10);
        assertThat(count).hasValue(10);
    }

    @RepeatedTest(10)
    public void testEmptyUniOnceInAWhileWithIndefinitely() {
        AtomicInteger count = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> {
            int v = count.incrementAndGet();
            if (v % 3 == 0) {
                return null;
            }
            return v;
        })
                .repeat().indefinitely()
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(10)
                .run(() -> await().until(() -> subscriber.getItems().size() == 10))
                .assertItems(1, 2, 4, 5, 7, 8, 10, 11, 13, 14)
                .cancel();
        assertThat(count).hasValue(14);
    }

    @RepeatedTest(10)
    public void testPredicateFailureWithUntil() {
        AtomicInteger count = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Uni.createFrom().item(count::incrementAndGet)
                .repeat().until(v -> {
                    if (v % 3 == 0) {
                        throw new IllegalStateException("boom");
                    }
                    return false;
                })
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(10)
                .assertFailedWith(IllegalStateException.class, "boom")
                .assertItems(1, 2);
        assertThat(count).hasValue(3);
    }

    @RepeatedTest(10)
    public void testPredicateFailureWithWhilst() {
        AtomicInteger count = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Uni.createFrom().item(count::incrementAndGet)
                .repeat().whilst(v -> {
                    if (v % 3 == 0) {
                        throw new IllegalStateException("boom");
                    }
                    return true;
                })
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(10)
                .assertFailedWith(IllegalStateException.class, "boom")
                .assertItems(1, 2);
        assertThat(count).hasValue(3);
    }

    @RepeatedTest(10)
    public void testEmptyUniOnceInAWhileWithUntil() {
        AtomicInteger count = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> {
            int v = count.incrementAndGet();
            if (v % 3 == 0) {
                return null;
            }
            return v;
        })
                .repeat().until(value -> value >= 1000)
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(10)
                .run(() -> await().until(() -> subscriber.getItems().size() == 10))
                .assertItems(1, 2, 4, 5, 7, 8, 10, 11, 13, 14)
                .cancel();
        assertThat(count).hasValue(14);
    }

    @RepeatedTest(10)
    public void testEmptyUniOnceInAWhileWithWhilst() {
        AtomicInteger count = new AtomicInteger();
        AssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> {
            int v = count.incrementAndGet();
            if (v % 3 == 0) {
                return null;
            }
            return v;
        })
                .repeat().whilst(value -> value < 1000)
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(10)
                .run(() -> await().until(() -> subscriber.getItems().size() == 10))
                .assertItems(1, 2, 4, 5, 7, 8, 10, 11, 13, 14)
                .cancel();
        assertThat(count).hasValue(14);
    }

    @RepeatedTest(10)
    public void testRepetitionWithState() {
        List<Object> list = Uni.createFrom().emitter(
                () -> new AtomicInteger(0),
                (s, e) -> e.complete(s.getAndIncrement()))
                .repeat().atMost(3)
                .collect().asList()
                .await().atMost(Duration.ofSeconds(5));
        assertThat(list).containsExactly(0, 1, 2);
    }

    public static class Page {
        List<Integer> items = new ArrayList<>();
        int next;

        public Page(List<Integer> items, int next) {
            this.items.addAll(items);
            this.next = next;
        }
    }

    @Test
    // https://github.com/smallrye/smallrye-mutiny/issues/447
    void testThatRepetitionIsStoppedOnFailure() {
        class TestSupplier implements Supplier<Integer> {
            private final AtomicInteger iter = new AtomicInteger();

            @Override
            public Integer get() {
                if (iter.incrementAndGet() < 5) {
                    throw new NoSuchElementException();
                } else {
                    return 123;
                }
            }
        }

        Integer result = Multi.createBy()
                .repeating().supplier(new TestSupplier()).indefinitely()
                .onFailure(NoSuchElementException.class).retry().indefinitely()
                .collect().first()
                .await().atMost(Duration.ofSeconds(10));

        assertThat(result).isEqualTo(123);
    }

    @Test
    // https://github.com/smallrye/smallrye-mutiny/issues/447
    void testThatRepetitionContinueIfUniEmitsNull() {
        class TestSupplier implements Supplier<Integer> {
            private final AtomicInteger iter = new AtomicInteger();

            @Override
            public Integer get() {
                if (iter.incrementAndGet() < 5) {
                    return null;
                } else {
                    return 123;
                }
            }
        }

        Integer result = Multi.createBy()
                .repeating().supplier(new TestSupplier()).indefinitely()
                .collect().first()
                .await().atMost(Duration.ofSeconds(10));

        assertThat(result).isEqualTo(123);

    }

    @Test
    // https://github.com/smallrye/smallrye-mutiny/issues/447
    void testThatRepetitionContinueIfUniEmitsNullInTheMiuddleOfTheSequence() {
        class TestSupplier implements Supplier<Integer> {
            private final AtomicInteger iter = new AtomicInteger();

            @Override
            public Integer get() {
                if (iter.incrementAndGet() == 3) {
                    return null;
                } else {
                    return iter.get();
                }
            }
        }

        List<Integer> list = Multi.createBy()
                .repeating().supplier(new TestSupplier()).indefinitely()
                .select().first(5)
                .collect().asList()
                .await().atMost(Duration.ofSeconds(10));

        assertThat(list).containsExactly(1, 2, 4, 5, 6);

    }

    @Test
    public void testDelayAndCancellationWhileWaiting() throws InterruptedException {
        AssertSubscriber<Integer> subscriber = Multi.createBy().repeating().supplier(() -> 1)
                .withDelay(Duration.ofSeconds(1))
                .atMost(2)
                .subscribe().withSubscriber(AssertSubscriber.create(2));

        await().until(() -> subscriber.getItems().size() == 1);
        subscriber.cancel();

        Thread.sleep(1500);
        assertThat(subscriber.getItems()).hasSize(1);
    }

    @Test
    public void testInvalidDelays() {
        assertThatThrownBy(() -> Multi.createBy().repeating().supplier(() -> 1)
                .withDelay(Duration.ofSeconds(0))
                .atMost(2)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("delay");

        assertThatThrownBy(() -> Multi.createBy().repeating().supplier(() -> 1)
                .withDelay(Duration.ofSeconds(-1))
                .atMost(2)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("delay");

        assertThatThrownBy(() -> Multi.createBy().repeating().supplier(() -> 1)
                .withDelay(null)
                .atMost(2)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("delay");
    }

}
