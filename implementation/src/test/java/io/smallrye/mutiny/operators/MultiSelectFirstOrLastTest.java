package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class MultiSelectFirstOrLastTest {

    private AtomicInteger counter;

    @BeforeEach
    public void init() {
        counter = new AtomicInteger();
    }

    @Test
    public void testSelectFirstWithLimit() {
        List<Integer> list = Multi.createFrom().range(1, 5)
                .onRequest().invoke(() -> counter.incrementAndGet())
                .select().first(2)
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(1, 2);
        assertThat(counter).hasValue(1);
    }

    @Test
    public void testSelectFirst() {
        List<Integer> list = Multi.createFrom().range(1, 5)
                .onRequest().invoke(() -> counter.incrementAndGet())
                .select().first(1)
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(1);
        assertThat(counter).hasValue(1);
    }

    @Test
    public void testSelectFirst0() {
        List<Integer> list = Multi.createFrom().range(1, 5).select().first(0)
                .collect().asList().await().indefinitely();

        assertThat(list).isEmpty();
    }

    @Test
    public void testSelectLastWithLimit() {
        List<Integer> list = Multi.createFrom().range(1, 5)
                .onRequest().invoke(() -> counter.incrementAndGet())
                .select().last(2)
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(3, 4);
        assertThat(counter).hasValue(1);
    }

    @Test
    public void testSelectLast() {
        List<Integer> list = Multi.createFrom().range(1, 5).select().last()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(4);
    }

    @Test
    public void testSelectLastWith0() {
        List<Integer> list = Multi.createFrom().range(1, 5)
                .select().last(0)
                .collect().asList().await().indefinitely();

        assertThat(list).isEmpty();
    }

    @Test
    public void testSelectFirstOnUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .select().first()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testSelectLastOnUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .select().last()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testSelectAll() {
        Multi.createFrom().range(1, 5).select().first(4)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testSelectLastAll() {
        Multi.createFrom().range(1, 5).select().last(4)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testInvalidLimit() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Multi.createFrom().items(1, 2, 3).select().first(-1));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Multi.createFrom().items(1, 2, 3).select().last(-1));
    }

    @Test
    public void testSelectLastWithBackPressure() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(0);

        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        Multi.createFrom().<Integer> emitter(emitter::set)
                .select().last(3)
                .subscribe(subscriber);

        subscriber.assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        emitter.get().emit(1).emit(2);

        subscriber.request(2)
                .assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        emitter.get().emit(3).emit(4);

        subscriber.request(5)
                .assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        emitter.get().emit(5).emit(6).emit(7).emit(8).emit(9).emit(10).complete();

        subscriber.request(5)
                .assertCompleted()
                .assertItems(8, 9, 10);
    }

    @Test
    public void testSelectSomeLastItems() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 11)
                .select().last(3)
                .subscribe(subscriber);

        subscriber.assertCompleted()
                .assertItems(8, 9, 10);
    }

    @Test
    public void testSelectWhileWithMethodThrowingException() {
        Multi.createFrom().range(1, 10).select().first(i -> {
            throw new IllegalStateException("boom");
        }).subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testSelectWhileWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .select().first(i -> i < 5)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testSelectWhileWithNullMethod() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().nothing().select().first((Predicate<? super Object>) null));
    }

    @Test
    public void testSelectWhile() {
        Multi.createFrom().range(1, 10).select().first(i -> i < 5)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testSelectWhileNone() {
        Multi.createFrom().items(1, 2, 3, 4).select().first(i -> false)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testSelectWhileAll() {
        Multi.createFrom().items(1, 2, 3, 4).select().first(i -> true)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testSelectWhileSomeWithBackPressure() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4)
                .select().first(i -> i < 3)
                .subscribe().withSubscriber(AssertSubscriber.create(0));

        subscriber.assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        subscriber.request(1);

        subscriber.assertNotTerminated()
                .assertItems(1);

        subscriber.request(2);

        subscriber.assertCompleted()
                .assertItems(1, 2);
    }

    @Test
    public void testLimitingInfiniteStream() {
        Multi.createFrom().ticks().every(Duration.ofMillis(2))
                .select().first(5)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .awaitCompletion()
                .assertItems(0L, 1L, 2L, 3L, 4L);
    }

    @Test
    public void testSelectWithNullDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().nothing().select().first((Duration) null));
    }

    @Test
    public void testSelectByTime() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().range(1, 100)
                .select().first(Duration.ofMillis(1000))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .awaitCompletion();

        assertThat(subscriber.getItems()).hasSize(10);
    }

    @Test
    public void testSelectByTimeWithFailure() {
        Multi<Integer> multi = Multi.createBy().concatenating().streams(
                Multi.createFrom().range(1, 5),
                Multi.createFrom().failure(new TestException("boom")),
                Multi.createFrom().range(5, 10));
        AssertSubscriber<Integer> subscriber = multi
                .select().first(Duration.ofMillis(1000))
                .subscribe().withSubscriber(AssertSubscriber.create(100))
                .awaitFailure()
                .assertFailedWith(TestException.class, "boom");

        assertThat(subscriber.getItems()).hasSize(4);
    }

    @Test
    public void testSelectByTimeWithCancellation() {
        Multi<Integer> multi = Multi.createBy().concatenating().streams(
                Multi.createFrom().range(1, 5),
                Multi.createFrom().range(5, 10));
        AssertSubscriber<Integer> subscriber = multi
                .select().first(Duration.ofMillis(1000))
                .subscribe().withSubscriber(AssertSubscriber.create(4));

        await().until(() -> subscriber.getItems().size() == 4);
        subscriber.cancel();

        assertThat(subscriber.getItems()).hasSize(4);
    }

    @Test
    public void testSelectByTimeWithImmediateCancellation() {
        Multi<Integer> multi = Multi.createBy().concatenating().streams(
                Multi.createFrom().range(1, 5),
                Multi.createFrom().range(5, 10));
        AssertSubscriber<Integer> subscriber = multi
                .select().first(Duration.ofMillis(1000))
                .subscribe().withSubscriber(new AssertSubscriber<>(4, true));

        subscriber.assertSubscribed()
                .assertNotTerminated()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testSelectByTimeWithRogueUpstreamSendingFailureAfterCompletion() {
        Multi<Integer> rogue = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(MultiSubscriber<? super Integer> subscriber) {
                subscriber.onItem(1);
                subscriber.onItem(2);
                subscriber.onComplete();
                subscriber.onItem(3);
                subscriber.onError(new IOException("boom"));
            }
        };
        AssertSubscriber<Integer> subscriber = rogue
                .select().first(Duration.ofMillis(1000))
                .subscribe().withSubscriber(AssertSubscriber.create(100))
                .awaitCompletion();

        assertThat(subscriber.getItems()).hasSize(2);
    }

    @Test
    public void testSelectByTimeWithRogueUpstreamSendingCompletionAfterFailure() {
        Multi<Integer> rogue = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(MultiSubscriber<? super Integer> subscriber) {
                subscriber.onItem(1);
                subscriber.onItem(2);
                subscriber.onError(new IOException("boom"));
                subscriber.onItem(3);
                subscriber.onComplete();
            }
        };
        AssertSubscriber<Integer> subscriber = rogue
                .select().first(Duration.ofMillis(1000))
                .subscribe().withSubscriber(AssertSubscriber.create(100))
                .awaitFailure()
                .assertFailedWith(IOException.class, "boom");

        assertThat(subscriber.getItems()).hasSize(2);
    }

    @Test
    public void testSkipByTimeWithInvalidDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().item(1).select().first(Duration.ofMillis(-1)));
    }

    @Test
    public void testSelectFirstWithTwoSubscribers() {
        AbstractMulti<Integer> upstream = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> subscriber) {
                Subscription subscription1 = mock(Subscription.class);
                Subscription subscription2 = mock(Subscription.class);
                subscriber.onSubscribe(subscription1);
                subscriber.onSubscribe(subscription2);
                verify(subscription2).cancel();
                subscriber.onNext(1);
                subscriber.onNext(2);
                subscriber.onNext(3);
                subscriber.onComplete();
            }
        };

        AssertSubscriber<Integer> subscriber = upstream
                .select().first(2)
                .subscribe().withSubscriber(AssertSubscriber.create(5));

        subscriber.assertSubscribed()
                .assertCompleted()
                .assertItems(1, 2);
    }

    @Test
    public void testSelectLastWithTwoSubscribers() {
        AbstractMulti<Integer> upstream = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> subscriber) {
                Subscription subscription1 = mock(Subscription.class);
                Subscription subscription2 = mock(Subscription.class);
                subscriber.onSubscribe(subscription1);
                subscriber.onSubscribe(subscription2);
                verify(subscription2).cancel();
                subscriber.onNext(1);
                subscriber.onNext(2);
                subscriber.onNext(3);
                subscriber.onComplete();
            }
        };

        AssertSubscriber<Integer> subscriber = upstream
                .select().last(2)
                .subscribe().withSubscriber(AssertSubscriber.create(5));

        subscriber.assertSubscribed()
                .assertCompleted()
                .assertItems(2, 3);
    }

    @Test
    public void testSelectFirstWith0() {
        AbstractMulti<Integer> upstream = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> subscriber) {
                Subscription subscription = mock(Subscription.class);
                subscriber.onSubscribe(subscription);
                subscriber.onNext(1);
                subscriber.onNext(2);
                subscriber.onNext(3);
                subscriber.onComplete();
            }
        };

        AssertSubscriber<Integer> subscriber = upstream
                .select().first(0)
                .subscribe().withSubscriber(AssertSubscriber.create(5));

        subscriber.assertSubscribed()
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testSelectLastWithZero() {
        AbstractMulti<Integer> upstream = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> subscriber) {
                Subscription subscription = mock(Subscription.class);
                subscriber.onSubscribe(subscription);
                subscriber.onNext(1);
                subscriber.onNext(2);
                subscriber.onNext(3);
                subscriber.onComplete();
            }
        };

        AssertSubscriber<Integer> subscriber = upstream
                .select().last(0)
                .subscribe().withSubscriber(AssertSubscriber.create(5));

        subscriber.assertSubscribed()
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testInvalidRequests() {
        AssertSubscriber<Integer> sub1 = AssertSubscriber.create();
        AssertSubscriber<Integer> sub2 = AssertSubscriber.create();
        Multi.createFrom().range(1, 4)
                .select().first(2)
                .subscribe(sub1);

        Multi.createFrom().range(1, 4)
                .select().last(2)
                .subscribe(sub2);

        sub1.request(-1)
                .assertFailedWith(IllegalArgumentException.class, "request");
        sub2.request(-1)
                .assertFailedWith(IllegalArgumentException.class, "request");
    }

    @Test
    public void testRequestGreaterThanNumberOfItems() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().range(1, 100)
                .select().first(2)
                .subscribe().withSubscriber(AssertSubscriber.create(5));

        subscriber
                .assertCompleted()
                .assertItems(1, 2);
    }

    @Test
    public void testRequestSmallerThanNumberOfItems() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().range(1, 100)
                .select().first(2)
                .subscribe().withSubscriber(AssertSubscriber.create(0));

        subscriber
                .assertSubscribed()
                .request(1)
                .assertItems(1)
                .request(1)
                .assertCompleted()
                .assertItems(1, 2);
    }

    /**
     * Reproducer for https://github.com/smallrye/smallrye-mutiny/issues/605.
     */
    @Test
    public void testThatCompletionEventCanBeSentAfterCancellation() {
        Uni<Object> stepOne = Multi.createFrom().items(new Object())
                .select()
                .first()
                .toUni();

        Uni<String> stringUni = stepOne
                .toMulti()
                .filter(o -> !(o instanceof Uni))
                .map(Object::toString)
                .toUni();

        assertThat(stringUni.await().indefinitely()).isNotNull();
    }

}
