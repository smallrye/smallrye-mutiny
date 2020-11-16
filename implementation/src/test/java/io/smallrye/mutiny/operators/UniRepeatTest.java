package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.test.Mocks;

public class UniRepeatTest {

    @Test
    public void testRepeatAtMost() {
        List<Integer> list = Uni.createFrom().item(1)
                .repeat().atMost(3)
                .collectItems().asList()
                .await().indefinitely();
        assertThat(list).hasSize(3).contains(1, 1, 1);
    }

    @Test
    public void testRepeatUntil() {
        List<String> items = Arrays.asList("a", "b", "c", "d", "e", "f");
        Iterator<String> iterator = items.iterator();
        List<String> list = Uni.createFrom().item(iterator::next)
                .repeat().until(v -> v.equalsIgnoreCase("d"))
                .collectItems().asList()
                .await().indefinitely();
        assertThat(list).hasSize(3).contains("a", "b", "c");
    }

    @Test
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

    @Test
    public void testRepeat0() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(0)
                .repeat().atMost(0)
                .collectItems().asList()
                .await().indefinitely());
    }

    @Test
    public void testRepeatUntilWithNullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(0)
                .repeat().until(null)
                .collectItems().asList()
                .await().indefinitely());
    }

    @Test
    public void testRepeatWhilstWithNullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(0)
                .repeat().whilst(null)
                .collectItems().asList()
                .await().indefinitely());
    }

    @Test
    public void testRepeat1() {
        AtomicInteger count = new AtomicInteger();
        List<Integer> list = Uni.createFrom().item(count::getAndIncrement)
                .repeat().atMost(1)
                .collectItems().asList()
                .await().indefinitely();

        assertThat(list).containsExactly(0);
        assertThat(count).hasValue(1);
    }

    @Test
    public void testRepeatUntilOnlyOnce() {
        AtomicInteger count = new AtomicInteger();
        AtomicBoolean once = new AtomicBoolean();
        List<Integer> list = Uni.createFrom().item(count::getAndIncrement)
                .repeat().until(x -> once.getAndSet(true))
                .collectItems().asList()
                .await().indefinitely();

        assertThat(list).containsExactly(0);
        assertThat(count).hasValue(2); // the first element and the element breaking the loop.
    }

    @Test
    public void testRepeatWhilstOnlyOnce() {
        AtomicInteger count = new AtomicInteger();
        AtomicBoolean once = new AtomicBoolean(true);
        List<Integer> list = Uni.createFrom().item(count::getAndIncrement)
                .repeat().whilst(x -> once.getAndSet(false))
                .collectItems().asList()
                .await().indefinitely();

        assertThat(list).containsExactly(0, 1);
        assertThat(count).hasValue(2);
    }

    @Test
    public void testNoRepeatUntil() {
        AtomicInteger count = new AtomicInteger();
        List<Integer> list = Uni.createFrom().item(count::getAndIncrement)
                .repeat().until(x -> true)
                .collectItems().asList()
                .await().indefinitely();

        assertThat(list).isEmpty();
        assertThat(count).hasValue(1);
    }

    @Test
    public void testNoRepeatWhilst() {
        AtomicInteger count = new AtomicInteger();
        List<Integer> list = Uni.createFrom().item(count::getAndIncrement)
                .repeat().whilst(x -> false)
                .collectItems().asList()
                .await().indefinitely();

        assertThat(list).containsExactly(0);
        assertThat(count).hasValue(1);
    }

    @Test
    public void testRepeatCancelledWithTake() {
        int num = 10;
        final AtomicInteger count = new AtomicInteger();
        int value = Uni.createFrom().item(count::incrementAndGet)
                .repeat().indefinitely()
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .transform().byTakingFirstItems(num)
                .collectItems().last()
                .await().indefinitely();
        assertThat(num).isEqualTo(value);
        assertThat(count).hasValue(value);
    }

    @Test
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
                .transform().byTakingFirstItems(num)
                .collectItems().last()
                .await().indefinitely();
        assertThat(num).isEqualTo(value);
        assertThat(count).hasValue(value);
        assertThat(invocations).hasValue(value);
    }

    @Test
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
                .transform().byTakingFirstItems(num)
                .collectItems().last()
                .await().indefinitely();
        assertThat(num).isEqualTo(value);
        assertThat(count).hasValue(value);
        assertThat(invocations).hasValue(value);
    }

    @Test
    public void testNoStackOverflow() {
        int value = Uni.createFrom().item(1).repeat().indefinitely()
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .transform().byTakingFirstItems(100000L)
                .collectItems().last()
                .await().indefinitely();
        assertThat(value).isEqualTo(1);
    }

    @Test
    public void testNoStackOverflowWithRepeatUntil() {
        AtomicInteger count = new AtomicInteger();
        int value = Uni.createFrom().item(1).repeat().until(x -> count.incrementAndGet() > 100000000L)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .transform().byTakingFirstItems(100000L)
                .collectItems().last()
                .await().indefinitely();
        assertThat(value).isEqualTo(1);
    }

    @Test
    public void testNoStackOverflowWithRepeatWhilst() {
        AtomicInteger count = new AtomicInteger();
        int value = Uni.createFrom().item(1).repeat().whilst(x -> count.incrementAndGet() < 100000000L)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .transform().byTakingFirstItems(100000L)
                .collectItems().last()
                .await().indefinitely();
        assertThat(value).isEqualTo(1);
    }

    @Test
    public void testNumberOfRepeat() {
        Subscriber<Integer> subscriber = Mocks.subscriber();

        Uni.createFrom().item(1).repeat().indefinitely()
                .transform().byTakingFirstItems(10)
                .subscribe(subscriber);

        verify(subscriber, times(10)).onNext(1);
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void testFailurePropagation() {
        Subscriber<Integer> subscriber = Mocks.subscriber();

        Uni.createFrom().<Integer> failure(() -> new IOException("boom")).repeat().indefinitely()
                .transform().byTakingFirstItems(10)
                .subscribe(subscriber);

        verify(subscriber).onError(any(IOException.class));
        verify(subscriber, never()).onComplete();
        verify(subscriber, never()).onNext(any());
    }

    @Test
    public void testFailurePropagationWithRepeatUntil() {
        Subscriber<Integer> subscriber = Mocks.subscriber();

        Uni.createFrom().<Integer> failure(() -> new IOException("boom")).repeat().until(x -> false)
                .transform().byTakingFirstItems(10)
                .subscribe(subscriber);

        verify(subscriber).onError(any(IOException.class));
        verify(subscriber, never()).onComplete();
        verify(subscriber, never()).onNext(any());
    }

    @Test
    public void testFailurePropagationWithRepeatWhilst() {
        Subscriber<Integer> subscriber = Mocks.subscriber();

        Uni.createFrom().<Integer> failure(() -> new IOException("boom")).repeat().whilst(x -> true)
                .transform().byTakingFirstItems(10)
                .subscribe(subscriber);

        verify(subscriber).onError(any(IOException.class));
        verify(subscriber, never()).onComplete();
        verify(subscriber, never()).onNext(any());
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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
                .await()
                .assertItems(1, 2)
                .assertFailedWith(IllegalStateException.class, "boom");
        assertThat(subscriber.getItems()).hasSize(2);
    }

    @Test
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
                .await()
                .assertItems(1, 2)
                .assertFailedWith(IllegalStateException.class, "boom");
        assertThat(subscriber.getItems()).hasSize(2);
    }

    @Test
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
                .await()
                .assertItems(1, 2)
                .assertFailedWith(IllegalStateException.class, "boom");
        assertThat(subscriber.getItems()).hasSize(2);
    }

    @Test
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
                .await()
                .assertItems(1, 2)
                .assertCompleted();
        assertThat(subscriber.getItems()).hasSize(2);
    }

    @Test
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
                .await()
                .assertItems(1, 2, 4, 5, 7, 8, 10)
                .assertCompleted();
        assertThat(count).hasValue(10);
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testRepetitionWithState() {
        List<Object> list = Uni.createFrom().emitter(
                () -> new AtomicInteger(0),
                (s, e) -> e.complete(s.getAndIncrement()))
                .repeat().atMost(3)
                .collectItems().asList()
                .await().indefinitely();
        assertThat(list).containsExactly(0, 1, 2);
    }

    public static class Page {
        List<Integer> items = new ArrayList<>();
        int next = -1;

        public Page(List<Integer> items, int next) {
            this.items.addAll(items);
            this.next = next;
        }
    }

}
