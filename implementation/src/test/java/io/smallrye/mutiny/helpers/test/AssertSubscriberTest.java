package io.smallrye.mutiny.helpers.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class AssertSubscriberTest {

    private final Duration SMALL = Duration.ofMillis(200);
    private final Duration MEDIUM = Duration.ofMillis(1000);

    private final Uni<Void> smallDelay = Uni.createFrom().voidItem()
            .onItem().delayIt().by(SMALL);
    private final Uni<Void> mediumDelay = Uni.createFrom().voidItem()
            .onItem().delayIt().by(MEDIUM);

    @Test
    public void testItemsAndCompletion() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);
        subscriber.assertNotTerminated();
        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        verify(subscription).request(2);
        subscriber.assertSubscribed();
        subscriber.onNext("a");
        subscriber.onNext("b");
        subscriber.onComplete();

        subscriber.assertItems("a", "b")
                .assertCompleted();

        assertThatThrownBy(() -> subscriber.assertItems("a", "B"))
                .isInstanceOf(AssertionError.class);

        assertThatThrownBy(subscriber::assertHasNotReceivedAnyItem)
                .isInstanceOf(AssertionError.class);

        assertThatThrownBy(() -> subscriber.assertItems("a", "B", "c"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected <b> to be equal to <B>")
                .hasMessageContaining("Missing expected item <c>");

        assertThatThrownBy(() -> subscriber.assertItems("a"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("The following items were not expected:\n<b>");
    }

    @Test
    public void testAssertItemsMessages() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        verify(subscription).request(2);

        subscriber.onNext("a");
        subscriber.onNext("b");
        subscriber.onNext("c");

        subscriber.assertItems("a", "b", "c");

        String m1 = "\nExpected to have received exactly:\n" +
                "<a,b,c,d>\n" +
                "but received:\n" +
                "<a,b,c>.\n" +
                "Mismatches are:\n" +
                "\t- Missing expected item <d>";
        assertThatThrownBy(() -> subscriber.assertItems("a", "b", "c", "d"))
                .isInstanceOf(AssertionError.class)
                .hasMessage(m1);

        String m2 = "\nExpected to have received exactly\n" +
                "<a,b>\n" +
                "but received\n" +
                "<a,b,c>.\n" +
                "The following items were not expected:\n" +
                "<c>";
        assertThatThrownBy(() -> subscriber.assertItems("a", "b"))
                .isInstanceOf(AssertionError.class)
                .hasMessage(m2);
    }

    @Test
    public void testItemsAndFailure() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        verify(subscription).request(2);
        subscriber.onNext("a");
        subscriber.onNext("b");
        subscriber.onError(new IOException("boom"));

        subscriber.assertItems("a", "b")
                .assertFailedWith(IOException.class, "boom")
                .assertTerminated();
    }

    @Test
    public void testNotCompleted() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        verify(subscription).request(2);
        subscriber.onNext("a");
        subscriber.onNext("b");

        subscriber.assertItems("a", "b")
                .assertNotTerminated();

        assertThatThrownBy(subscriber::assertCompleted)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("completion");

        subscriber.onError(new Exception("boom"));

        assertThatThrownBy(subscriber::assertCompleted)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure");
    }

    @Test
    public void testNotFailed() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        verify(subscription).request(2);
        subscriber.onNext("a");
        subscriber.onNext("b");

        subscriber.assertItems("a", "b")
                .assertNotTerminated();

        assertThatThrownBy(() -> subscriber.assertFailedWith(Exception.class, ""))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure");

        subscriber.onComplete();

        assertThatThrownBy(() -> subscriber.assertFailedWith(Exception.class, ""))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("completion event");
    }

    @Test
    public void testNoItems() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);

        subscriber.assertNotTerminated();
        subscriber.assertHasNotReceivedAnyItem();

        assertThatThrownBy(() -> subscriber.assertItems("a"))
                .isInstanceOf(AssertionError.class);

        subscriber.cancel();
    }

    @Test
    public void testAwait() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);

        new Thread(() -> {
            subscriber.onNext("1");
            subscriber.onNext("2");
            subscriber.onComplete();
        }).start();

        subscriber.awaitCompletion();
    }

    @Test
    public void testAwaitWithDuration() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);

        new Thread(() -> {
            subscriber.onNext("1");
            subscriber.onNext("2");
            subscriber.onComplete();
        }).start();

        subscriber.awaitCompletion();
    }

    @Test
    public void testAwaitOnFailure() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);

        new Thread(() -> {
            subscriber.onNext("1");
            subscriber.onNext("2");
            subscriber.onError(new Exception("boom"));
        }).start();

        subscriber.awaitFailure();
        subscriber.assertFailedWith(Exception.class, "boom");

        assertThatThrownBy(() -> subscriber.assertFailedWith(IllegalStateException.class, ""))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure");

        assertThatThrownBy(() -> subscriber.assertFailedWith(Exception.class, "nope"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure");
    }

    @Test
    public void testAwaitAlreadyCompleted() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        subscriber.onComplete();

        subscriber.awaitCompletion();
    }

    @Test
    public void testAwaitAlreadyFailed() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        subscriber.onError(new Exception("boom"));

        subscriber.awaitFailure();
        subscriber.assertFailedWith(Exception.class, "boom");
    }

    @Test
    public void testUpfrontCancellation() {
        AssertSubscriber<String> subscriber = new AssertSubscriber<>(0, true);
        Subscription subscription = mock(Subscription.class);
        subscriber.onSubscribe(subscription);
        verify(subscription).cancel();
    }

    @Test
    public void testUpfrontRequest() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create(10);
        Subscription subscription = mock(Subscription.class);
        subscriber.onSubscribe(subscription);
        verify(subscription).request(10);
    }

    @Test
    public void testRun() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        AtomicInteger count = new AtomicInteger();
        subscriber.run(count::incrementAndGet).run(count::incrementAndGet);

        assertThat(count).hasValue(2);

        assertThatThrownBy(() -> subscriber.run(() -> {
            throw new IllegalStateException("boom");
        }))
                .isInstanceOf(AssertionError.class)
                .hasCauseInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> subscriber.run(() -> {
            throw new AssertionError("boom");
        })).isInstanceOf(AssertionError.class);
    }

    @Test
    public void testCancelWithoutSubscription() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        assertThatThrownBy(subscriber::cancel)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("subscription");
    }

    @Test
    public void testMultipleSubscriptions() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();

        subscriber.assertNotSubscribed();
        assertThatThrownBy(subscriber::assertSubscribed)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("subscription");

        subscriber.onSubscribe(mock(Subscription.class));
        assertThatThrownBy(subscriber::assertNotSubscribed)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("subscription");
        subscriber.assertSubscribed();

        subscriber.onSubscribe(mock(Subscription.class));
        assertThatThrownBy(subscriber::assertNotSubscribed)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("subscriptions")
                .hasMessageContaining("2");
        assertThatThrownBy(subscriber::assertSubscribed)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("subscriptions")
                .hasMessageContaining("2");
    }

    @Test
    public void testTermination() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();

        subscriber.assertNotTerminated();
        assertThatThrownBy(subscriber::assertTerminated)
                .isInstanceOf(AssertionError.class);

        subscriber.onComplete();
        assertThatThrownBy(subscriber::assertNotTerminated)
                .isInstanceOf(AssertionError.class);
        subscriber.assertTerminated();
    }

    @Test
    public void testAwaitSubscription() {
        // already subscribed
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1)
                .subscribe().withSubscriber(AssertSubscriber.create(0));
        assertThat(subscriber.awaitSubscription()).isSameAs(subscriber);

        // Delay
        subscriber = Multi.createFrom().items(1)
                .onSubscription().call(x -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(0));
        assertThat(subscriber.awaitSubscription()).isSameAs(subscriber);

        subscriber = Multi.createFrom().items(1)
                .onSubscription().call(x -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(0));
        assertThat(subscriber.awaitSubscription(MEDIUM)).isSameAs(subscriber);

        // timeout
        subscriber = Multi.createFrom().items(1)
                .onSubscription().call(x -> mediumDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(0));
        AssertSubscriber<Integer> tmp = subscriber;
        assertThatThrownBy(() -> tmp.awaitSubscription(SMALL)).isInstanceOf(AssertionError.class)
                .hasMessageContaining("subscription").hasMessageContaining(SMALL.toMillis() + " ms");
    }

    @Test
    public void testAwaitCompletion() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1)
                .subscribe().withSubscriber(AssertSubscriber.create(1));
        assertThat(subscriber.awaitCompletion()).isSameAs(subscriber);

        // Delay
        subscriber = Multi.createFrom().items(1)
                .onCompletion().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1));
        assertThat(subscriber.awaitCompletion()).isSameAs(subscriber);

        subscriber = Multi.createFrom().items(1)
                .onCompletion().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1));
        assertThat(subscriber.awaitCompletion(MEDIUM)).isSameAs(subscriber);

        // timeout
        subscriber = Multi.createFrom().items(1)
                .onCompletion().call(() -> Uni.createFrom().voidItem()
                        .onItem().delayIt().by(MEDIUM))
                .subscribe().withSubscriber(AssertSubscriber.create(1));
        AssertSubscriber<Integer> tmp = subscriber;
        assertThatThrownBy(() -> tmp.awaitCompletion(SMALL))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("completion").hasMessageContaining(SMALL.toMillis() + " ms");

        // Failure instead of completion
        assertThatThrownBy(() -> Multi.createFrom().<Integer> failure(new TestException())
                .onFailure().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1)).awaitCompletion()).isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure");
    }

    @Test
    public void testAwaitFailure() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> failure(new TestException())
                .subscribe().withSubscriber(AssertSubscriber.create(1));
        assertThat(subscriber.awaitFailure()).isSameAs(subscriber);

        assertThat(
                subscriber.awaitFailure(t -> assertThat(t).isInstanceOf(TestException.class))).isSameAs(subscriber);

        AssertSubscriber<Integer> tmp = subscriber;
        Consumer<Throwable> failedValidation = t -> assertThat(t).isInstanceOf(IOException.class);
        Consumer<Throwable> passedValidation = t -> assertThat(t).isInstanceOf(TestException.class);

        assertThatThrownBy(() -> tmp.awaitFailure(failedValidation))
                .isInstanceOf(AssertionError.class).hasMessageContaining("validation");

        // Delay
        subscriber = Multi.createFrom().<Integer> failure(new TestException())
                .onFailure().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1));
        assertThat(subscriber.awaitFailure()).isSameAs(subscriber);

        subscriber = Multi.createFrom().<Integer> failure(new TestException())
                .onFailure().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1));
        assertThat(subscriber.awaitFailure(passedValidation)).isSameAs(subscriber);

        subscriber = Multi.createFrom().<Integer> failure(new TestException())
                .onFailure().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1));
        assertThat(subscriber.awaitFailure(MEDIUM)).isSameAs(subscriber);

        // timeout
        subscriber = Multi.createFrom().<Integer> failure(new TestException())
                .onFailure().call(() -> mediumDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1));
        AssertSubscriber<Integer> tmp2 = subscriber;
        assertThatThrownBy(() -> tmp2.awaitFailure(SMALL)).isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure").hasMessageContaining(SMALL.toMillis() + " ms");

        // Completion instead of failure
        assertThatThrownBy(() -> Multi.createFrom().items(1, 2)
                .subscribe().withSubscriber(AssertSubscriber.create(2))
                .awaitFailure()).isInstanceOf(AssertionError.class).hasMessageContaining("completion");
    }

    @Test
    public void testFailureWithNoMessage() {
        Multi.createFrom().failure(new TimeoutException())
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitFailure()
                .assertFailedWith(TimeoutException.class);
    }

    @Test
    public void testAwaitItem() {
        // Already completed
        assertThatThrownBy(() -> Multi.createFrom().empty()
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitNextItem()).isInstanceOf(AssertionError.class)
                .hasMessageContaining("completion").hasMessageContaining("item");

        // Already failed
        assertThatThrownBy(() -> Multi.createFrom().failure(new TestException())
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitNextItem()).isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure").hasMessageContaining("item")
                .hasMessageContaining(TestException.class.getName());

        // Completion instead of item
        assertThatThrownBy(() -> Multi.createFrom().empty()
                .onCompletion().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitNextItem(SMALL.multipliedBy(2))).isInstanceOf(AssertionError.class)
                .hasMessageContaining("completion").hasMessageContaining("item");

        // Failure instead of item
        assertThatThrownBy(() -> Multi.createFrom().failure(new TestException())
                .onFailure().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitNextItem(SMALL.multipliedBy(2))).isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure").hasMessageContaining("item");

        // Item
        Multi.createFrom().items(1)
                .onItem().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitNextItem(MEDIUM)
                .assertItems(1);

        // Item group
        Multi.createFrom().items(1, 2, 3)
                .onItem().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitNextItem(MEDIUM);

        // Timeout
        assertThatThrownBy(() -> Multi.createFrom().item(1)
                .onItem().call(() -> mediumDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitNextItem(SMALL)).isInstanceOf(AssertionError.class)
                .hasMessageContaining("item")
                .hasMessageContaining(SMALL.toMillis() + " ms");
    }

    @Test
    public void testAwaitNextItems() {
        // Already completed
        assertThatThrownBy(() -> Multi.createFrom().empty()
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitNextItems(2)).isInstanceOf(AssertionError.class)
                .hasMessageContaining("completion").hasMessageContaining("item");

        assertThatThrownBy(() -> Multi.createFrom().empty()
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitNextItems(2, 1)).isInstanceOf(AssertionError.class)
                .hasMessageContaining("completion").hasMessageContaining("item");

        assertThatThrownBy(() -> Multi.createFrom().empty()
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitNextItems(2, 1, Duration.ofSeconds(1))).isInstanceOf(AssertionError.class)
                .hasMessageContaining("completion").hasMessageContaining("item");

        // Already failed
        assertThatThrownBy(() -> Multi.createFrom().failure(new TestException())
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitNextItems(2)).isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure").hasMessageContaining("item")
                .hasMessageContaining(TestException.class.getName());

        assertThatThrownBy(() -> Multi.createFrom().failure(new TestException())
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitNextItems(2, 1)).isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure").hasMessageContaining("item")
                .hasMessageContaining(TestException.class.getName());

        assertThatThrownBy(() -> Multi.createFrom().failure(new TestException())
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitNextItems(2, 1, Duration.ofSeconds(1))).isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure").hasMessageContaining("item")
                .hasMessageContaining(TestException.class.getName());

        // Completion instead of item
        assertThatThrownBy(() -> Multi.createFrom().emitter(e -> e.emit(1).complete())
                .onCompletion().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitNextItems(2, SMALL.multipliedBy(2))).isInstanceOf(AssertionError.class)
                .hasMessageContaining("completion").hasMessageContaining("item").hasMessageContaining("0");

        // Failure instead of item
        assertThatThrownBy(() -> Multi.createFrom().emitter(e -> e.emit(1).fail(new TestException()))
                .onFailure().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitNextItems(2, SMALL.multipliedBy(2))).isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure").hasMessageContaining("item").hasMessageContaining("0");

        // Item
        Multi.createFrom().items(1, 2, 3)
                .onItem().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(3))
                .awaitNextItems(3, MEDIUM)
                .assertItems(1, 2, 3)
                .assertLastItem(3);

        // Timeout
        assertThatThrownBy(() -> Multi.createFrom().emitter(e -> {
            e.emit(1).emit(2);
            try {
                Thread.sleep(MEDIUM.toMillis());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            e.emit(3);
        })
                .emitOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(AssertSubscriber.create(3))
                .awaitNextItems(3, SMALL)).isInstanceOf(AssertionError.class)
                .hasMessageContaining("item")
                .hasMessageContaining(SMALL.toMillis() + " ms");
    }

    @RepeatedTest(10)
    public void testAwaitItems() {
        // Already completed
        assertThatThrownBy(() -> Multi.createFrom().empty()
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitItems(2)).isInstanceOf(AssertionError.class)
                .hasMessageContaining("terminal").hasMessageContaining("item");

        // Already failed
        assertThatThrownBy(() -> Multi.createFrom().failure(new TestException())
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitItems(2)).isInstanceOf(AssertionError.class)
                .hasMessageContaining("terminal").hasMessageContaining("item");

        // Completion instead of item
        assertThatThrownBy(() -> Multi.createFrom().emitter(e -> e.emit(1).complete())
                .onCompletion().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitItems(2, SMALL.multipliedBy(100))).isInstanceOf(AssertionError.class)
                .hasMessageContaining("completion").hasMessageContaining("item").hasMessageContaining("1");

        // Failure instead of item
        assertThatThrownBy(() -> Multi.createFrom().emitter(e -> e.emit(1).fail(new TestException()))
                .onFailure().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .awaitItems(2, SMALL.multipliedBy(100))).isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure").hasMessageContaining("item").hasMessageContaining("1");

        // Item
        Multi.createFrom().items(1, 2, 3)
                .onItem().call(() -> smallDelay)
                .subscribe().withSubscriber(AssertSubscriber.create(3))
                .awaitItems(3, MEDIUM)
                .assertItems(1, 2, 3)
                .assertLastItem(3);

        // Timeout
        assertThatThrownBy(() -> Multi.createFrom().emitter(e -> {
            e.emit(1).emit(2);
            try {
                Thread.sleep(MEDIUM.toMillis());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            e.emit(3);
        })
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(AssertSubscriber.create(3))
                .awaitItems(3, SMALL)).isInstanceOf(AssertionError.class)
                .hasMessageContaining("item");

        // Have received more items than expected.
        assertThatThrownBy(() -> Multi.createFrom().items(1, 2, 3)
                .subscribe().withSubscriber(AssertSubscriber.create(3))
                .awaitItems(2, SMALL)).isInstanceOf(AssertionError.class)
                .hasMessageContaining("item").hasMessageContaining("2").hasMessageContaining("3");

        // Already Cancelled
        assertThatThrownBy(() -> Multi.createFrom().items(1, 2, 3)
                .subscribe().withSubscriber(new AssertSubscriber<>(1, true))
                .awaitItems(2, SMALL)).isInstanceOf(AssertionError.class)
                .hasMessageContaining("item").hasMessageContaining("2").hasMessageContaining("0")
                .hasMessageContaining("terminal");

        // Cancellation while waiting.
        assertThatThrownBy(() -> {
            AssertSubscriber<Object> subscriber = Multi.createFrom().nothing()
                    .subscribe().withSubscriber(new AssertSubscriber<>(1, true));
            subscriber
                    .run(() -> new Thread(() -> {
                        try {
                            Thread.sleep(SMALL.toMillis());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        subscriber.cancel();
                    }).start())
                    .awaitItems(2, MEDIUM);
        }).isInstanceOf(AssertionError.class)
                .hasMessageContaining("item").hasMessageContaining("2").hasMessageContaining("0")
                .hasMessageContaining("terminal");

        // Already receive the right number
        Multi.createFrom().items(1, 2, 3)
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .awaitItems(3)
                .cancel();
    }

    @Test
    public void testAssertLast() {
        assertThatThrownBy(
                () -> Multi.createFrom().empty().subscribe().withSubscriber(AssertSubscriber.create(1))
                        .assertLastItem(1))
                .isInstanceOf(AssertionError.class);

        Multi.createFrom().items(1, 2, 3, 4)
                .subscribe().withSubscriber(AssertSubscriber.create(2))
                .assertLastItem(2)
                .request(2)
                .assertLastItem(4);
    }
}
