package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Subscriber;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.test.Mocks;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

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

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRepeat0() {
        Uni.createFrom().item(0)
                .repeat().atMost(0)
                .collectItems().asList()
                .await().indefinitely();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRepeatUntilWithNullPredicate() {
        Uni.createFrom().item(0)
                .repeat().until(null)
                .collectItems().asList()
                .await().indefinitely();
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
    public void testRepeatCancelledWithTake() {
        int num = 10;
        final AtomicInteger count = new AtomicInteger();
        int value = Uni.createFrom().item(count::incrementAndGet)
                .repeat().indefinitely()
                .subscribeOn(Infrastructure.getDefaultWorkerPool())
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
                .subscribeOn(Infrastructure.getDefaultWorkerPool())
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
                .subscribeOn(Infrastructure.getDefaultWorkerPool())
                .transform().byTakingFirstItems(100000L)
                .collectItems().last()
                .await().indefinitely();
        assertThat(value).isEqualTo(1);
    }

    @Test
    public void testNoStackOverflowWithRepeatUntil() {
        AtomicInteger count = new AtomicInteger();
        int value = Uni.createFrom().item(1).repeat().until(x -> count.incrementAndGet() > 100000000L)
                .subscribeOn(Infrastructure.getDefaultWorkerPool())
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
    public void testRequestAndCancellation() {
        final AtomicInteger count = new AtomicInteger();
        MultiAssertSubscriber<Integer> subscriber = Uni.createFrom().item(count::incrementAndGet)
                .repeat().indefinitely()
                .subscribeOn(Infrastructure.getDefaultWorkerPool())
                .subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber.assertSubscribed().assertHasNotReceivedAnyItem();
        subscriber
                .request(2)
                .run(() -> {
                    await().until(() -> subscriber.items().size() == 2);
                    assertThat(subscriber.items()).containsExactly(1, 2);
                    assertThat(count).hasValue(2);
                })
                .request(1)
                .run(() -> {
                    await().until(() -> subscriber.items().size() == 3);
                    assertThat(subscriber.items()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                })
                .cancel()
                .run(() -> {
                    await().until(() -> subscriber.items().size() == 3);
                    assertThat(subscriber.items()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                })
                .request(20)
                .run(() -> {
                    await().until(() -> subscriber.items().size() == 3);
                    assertThat(subscriber.items()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                });
    }

    @Test
    public void testRequestAndCancellationWithRepeatUntil() {
        final AtomicInteger count = new AtomicInteger();
        MultiAssertSubscriber<Integer> subscriber = Uni.createFrom().item(count::incrementAndGet)
                .repeat().until(x -> false)
                .subscribeOn(Infrastructure.getDefaultWorkerPool())
                .subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber.assertSubscribed().assertHasNotReceivedAnyItem();
        subscriber
                .request(2)
                .run(() -> {
                    await().until(() -> subscriber.items().size() == 2);
                    assertThat(subscriber.items()).containsExactly(1, 2);
                    assertThat(count).hasValue(2);
                })
                .request(1)
                .run(() -> {
                    await().until(() -> subscriber.items().size() == 3);
                    assertThat(subscriber.items()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                })
                .cancel()
                .run(() -> {
                    await().until(() -> subscriber.items().size() == 3);
                    assertThat(subscriber.items()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                })
                .request(20)
                .run(() -> {
                    await().until(() -> subscriber.items().size() == 3);
                    assertThat(subscriber.items()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                });
    }

    @Test
    public void testRequestWithAtMost() {
        final AtomicInteger count = new AtomicInteger();
        MultiAssertSubscriber<Integer> subscriber = Uni.createFrom().item(count::incrementAndGet)
                .repeat().atMost(3)
                .subscribeOn(Infrastructure.getDefaultWorkerPool())
                .subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber.assertSubscribed().assertHasNotReceivedAnyItem();
        subscriber
                .request(2)
                .run(() -> {
                    await().until(() -> subscriber.items().size() == 2);
                    assertThat(subscriber.items()).containsExactly(1, 2);
                    assertThat(count).hasValue(2);
                })
                .request(20)
                .run(() -> {
                    await().until(() -> subscriber.items().size() == 3);
                    assertThat(subscriber.items()).containsExactly(1, 2, 3);
                    assertThat(count).hasValue(3);
                })
                .assertCompletedSuccessfully();
    }

    @Test
    public void testFailurePropagationAfterFewRepeats() {
        AtomicInteger count = new AtomicInteger();
        MultiAssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> {
            int v = count.incrementAndGet();
            if (v == 3) {
                throw new IllegalStateException("boom");
            }
            return v;
        })
                .repeat().indefinitely()
                .subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber.request(10)
                .await()
                .assertReceived(1, 2)
                .assertHasFailedWith(IllegalStateException.class, "boom");
        assertThat(subscriber.items()).hasSize(2);
    }

    @Test
    public void testFailurePropagationAfterFewRepeatsWithRepeatUntil() {
        AtomicInteger count = new AtomicInteger();
        MultiAssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> {
            int v = count.incrementAndGet();
            if (v == 3) {
                throw new IllegalStateException("boom");
            }
            return v;
        })
                .repeat().until(x -> false)
                .subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber.request(10)
                .await()
                .assertReceived(1, 2)
                .assertHasFailedWith(IllegalStateException.class, "boom");
        assertThat(subscriber.items()).hasSize(2);
    }

    @Test
    public void testFailurePropagationAfterMaxRepeats() {
        AtomicInteger count = new AtomicInteger();
        MultiAssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> {
            int v = count.incrementAndGet();
            if (v == 3) {
                throw new IllegalStateException("boom");
            }
            return v;
        })
                .repeat().atMost(2)
                .subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber.request(10)
                .await()
                .assertReceived(1, 2)
                .assertCompletedSuccessfully();
        assertThat(subscriber.items()).hasSize(2);
    }

    @Test
    public void testEmptyUniOnceInAWhileWithAtMost() {
        AtomicInteger count = new AtomicInteger();
        MultiAssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> {
            int v = count.incrementAndGet();
            if (v % 3 == 0) {
                return null;
            }
            return v;
        })
                .repeat().atMost(10)
                .subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber.request(100)
                .await()
                .assertReceived(1, 2, 4, 5, 7, 8, 10)
                .assertCompletedSuccessfully();
        assertThat(count).hasValue(10);
    }

    @Test
    public void testEmptyUniOnceInAWhileWithIndefinitely() {
        AtomicInteger count = new AtomicInteger();
        MultiAssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> {
            int v = count.incrementAndGet();
            if (v % 3 == 0) {
                return null;
            }
            return v;
        })
                .repeat().indefinitely()
                .subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber.request(10)
                .run(() -> await().until(() -> subscriber.items().size() == 10))
                .assertReceived(1, 2, 4, 5, 7, 8, 10, 11, 13, 14)
                .cancel();
        assertThat(count).hasValue(14);
    }

    @Test
    public void testPredicateFailure() {
        AtomicInteger count = new AtomicInteger();
        MultiAssertSubscriber<Integer> subscriber = Uni.createFrom().item(count::incrementAndGet)
                .repeat().until(v -> {
                    if (v % 3 == 0) {
                        throw new IllegalStateException("boom");
                    }
                    return false;
                })
                .subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber.request(10)
                .assertHasFailedWith(IllegalStateException.class, "boom")
                .assertReceived(1, 2);
        assertThat(count).hasValue(3);
    }

    @Test
    public void testEmptyUniOnceInAWhileWithUntil() {
        AtomicInteger count = new AtomicInteger();
        MultiAssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> {
            int v = count.incrementAndGet();
            if (v % 3 == 0) {
                return null;
            }
            return v;
        })
                .repeat().until(value -> value >= 1000)
                .subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber.request(10)
                .run(() -> await().until(() -> subscriber.items().size() == 10))
                .assertReceived(1, 2, 4, 5, 7, 8, 10, 11, 13, 14)
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
}
