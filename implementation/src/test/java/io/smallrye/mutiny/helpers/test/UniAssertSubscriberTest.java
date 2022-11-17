package io.smallrye.mutiny.helpers.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
class UniAssertSubscriberTest {

    @Test
    void testCompletion() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(123)
                .subscribe().withSubscriber(new UniAssertSubscriber<>());

        subscriber.assertCompleted().assertItem(123);
        assertThat(subscriber.getItem()).isEqualTo(123);
        assertThat(subscriber.getFailure()).isNull();

        assertThatThrownBy(() -> subscriber.assertItem(2))
                .isInstanceOf(AssertionError.class);

        assertThatThrownBy(() -> subscriber.assertItem(null))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void testAwait() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(123)
                .subscribe().withSubscriber(new UniAssertSubscriber<>());

        assertThat(subscriber.awaitItem().getItem()).isEqualTo(123);
        subscriber.awaitItem();
        subscriber.assertCompleted();
        subscriber.assertTerminated();
    }

    @Test
    void testUpfrontCancellation() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(123)
                .subscribe().withSubscriber(new UniAssertSubscriber<>(true));

        subscriber.assertNotTerminated();
        assertThat(subscriber.getItem()).isNull();
        assertThat(subscriber.getFailure()).isNull();
    }

    @Test
    void testFailure() {
        UniAssertSubscriber<Object> subscriber = Uni.createFrom().failure(new IOException("boom"))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber
                .assertFailed()
                .assertFailedWith(IOException.class, "boom");
        assertThat(subscriber.getItem()).isNull();
        assertThat(subscriber.getFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");
    }

    @Test
    void testEmitter() {
        AtomicReference<UniEmitter<Object>> emitterRef = new AtomicReference<>();
        UniAssertSubscriber<Object> subscriber = Uni.createFrom()
                .emitter((Consumer<UniEmitter<? super Object>>) emitterRef::set)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertSubscribed();
        emitterRef.get().complete("abc");
        subscriber.assertCompleted().assertItem("abc");
    }

    @Test
    public void testCancellationWithoutSubscription() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        subscriber.assertNotSubscribed();
        subscriber.cancel();

        Uni.createFrom().item("x")
                .subscribe().withSubscriber(subscriber);
        subscriber.assertNotTerminated();
    }

    @Test
    public void testCancellationAfterEmission() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        subscriber.assertNotSubscribed();
        Uni.createFrom().item("x")
                .subscribe().withSubscriber(subscriber);
        subscriber.cancel();
        subscriber.assertCompleted();
    }

    @Test
    public void testCorrectSignalsOrder_1() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        subscriber.onSubscribe(() -> {
        });
        subscriber.onItem("Yo");
        subscriber.cancel();
        subscriber.assertSignalsReceivedInOrder();

        List<UniSignal> signals = subscriber.getSignals();
        assertThat(signals).hasSize(3);
        assertThat(signals.get(1)).isInstanceOf(OnItemUniSignal.class);
        assertThat(signals.get(1).value()).isEqualTo("Yo");
    }

    @Test
    public void testCorrectSignalsOrder_2() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        subscriber.onSubscribe(() -> {
        });
        subscriber.onFailure(new RuntimeException("Yo"));
        subscriber.cancel();
        subscriber.assertSignalsReceivedInOrder();

        List<UniSignal> signals = subscriber.getSignals();
        assertThat(signals).hasSize(3);
        assertThat(signals.get(1).value()).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testCorrectSignalsOrder_3() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        subscriber.onSubscribe(() -> {
        });
        subscriber.cancel();
        subscriber.assertSignalsReceivedInOrder();
    }

    @Test
    public void testCorrectSignalsOrder_4() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        subscriber.assertSignalsReceivedInOrder();
    }

    @Test
    public void testOnSubscribeAfterFailure() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        subscriber.onFailure(new RuntimeException("Yo"));
        subscriber.onSubscribe(() -> {
        });

        assertThatThrownBy(subscriber::assertSignalsReceivedInOrder)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("The first signal is neither onSubscribe nor cancel");
    }

    @Test
    public void testOnSubscribeAfterItem() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        subscriber.onItem("Yo");
        subscriber.onSubscribe(() -> {
        });

        assertThatThrownBy(subscriber::assertSignalsReceivedInOrder)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("The first signal is neither onSubscribe nor cancel");
    }

    @Test
    public void testSignalsWithImmediateCancellation() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        subscriber.cancel();
        Uni.createFrom().item("yolo").subscribe().withSubscriber(subscriber).assertSignalsReceivedInOrder();
    }

    @Test
    public void testSignalsWithoutImmediateCancellation() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().item("yolo").subscribe().withSubscriber(subscriber).assertSignalsReceivedInOrder();
    }

    @Test
    public void testDoubleOnSubscribe() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        subscriber.onSubscribe(() -> {
        });
        subscriber.onFailure(new RuntimeException("Yo"));
        subscriber.onSubscribe(() -> {
        });

        assertThatThrownBy(subscriber::assertSignalsReceivedInOrder)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("There are more than 1 onSubscribe signals");
    }

    @Test
    public void testDoubleOnItem() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        subscriber.onSubscribe(() -> {
        });
        subscriber.onItem("Yo1");
        subscriber.onItem("Yo2");

        assertThatThrownBy(subscriber::assertSignalsReceivedInOrder)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("There are more than 1 onItem signals");
    }

    @Test
    public void testDoubleOnFailure() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        subscriber.onSubscribe(() -> {
        });
        subscriber.onFailure(new RuntimeException("Yo1"));
        subscriber.onFailure(new RuntimeException("Yo2"));

        assertThatThrownBy(subscriber::assertSignalsReceivedInOrder)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("There are more than 1 onFailure signals");
    }

    @Test
    public void testBothOnFailureAndOnItem() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        subscriber.onSubscribe(() -> {
        });
        subscriber.onItem("Yo");
        subscriber.onFailure(new RuntimeException("Yo"));

        assertThatThrownBy(subscriber::assertSignalsReceivedInOrder)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("There are both onItem and onFailure");
    }

    @Test
    public void testAwaitSubscription() {
        // already subscribed
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(1)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertThat(subscriber.awaitSubscription()).isSameAs(subscriber);

        // Delay
        subscriber = Uni.createFrom().item(1)
                .onSubscription().call(x -> Uni.createFrom().item(x).onItem().delayIt().by(Duration.ofSeconds(1)))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertThat(subscriber.awaitSubscription()).isSameAs(subscriber);

        subscriber = Uni.createFrom().item(1)
                .onSubscription().call(x -> Uni.createFrom().item(x).onItem().delayIt().by(Duration.ofSeconds(1)))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertThat(subscriber.awaitSubscription(Duration.ofSeconds(5))).isSameAs(subscriber);

        // timeout
        subscriber = Uni.createFrom().item(1)
                .onSubscription().call(x -> Uni.createFrom().item(x).onItem().delayIt().by(Duration.ofSeconds(10)))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Integer> tmp = subscriber;
        assertThatThrownBy(() -> tmp.awaitSubscription(Duration.ofMillis(100))).isInstanceOf(AssertionError.class)
                .hasMessageContaining("subscription").hasMessageContaining("100 ms");
    }

    @Test
    public void testAwaitItem() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(1)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertThat(subscriber.awaitItem().getItem()).isEqualTo(1);

        // Delay
        subscriber = Uni.createFrom().item(1)
                .onItem().call(() -> Uni.createFrom().voidItem()
                        .onItem().delayIt().by(Duration.ofSeconds(1)))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertThat(subscriber.awaitItem().getItem()).isEqualTo(1);

        subscriber = Uni.createFrom().item(1)
                .onItem().call(() -> Uni.createFrom().voidItem()
                        .onItem().delayIt().by(Duration.ofSeconds(1)))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertThat(subscriber.awaitItem(Duration.ofSeconds(5)).getItem()).isEqualTo(1);

        // timeout
        subscriber = Uni.createFrom().item(1)
                .onItem().call(() -> Uni.createFrom().voidItem()
                        .onItem().delayIt().by(Duration.ofSeconds(10)))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Integer> tmp = subscriber;
        assertThatThrownBy(() -> tmp.awaitItem(Duration.ofMillis(100))).isInstanceOf(AssertionError.class)
                .hasMessageContaining("item").hasMessageContaining("100 ms");
    }

    @Test
    public void testAwaitFailure() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().<Integer> failure(new TestException())
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertThat(subscriber.awaitFailure()).isSameAs(subscriber);

        assertThat(
                subscriber.awaitFailure(t -> assertThat(t).isInstanceOf(TestException.class))).isSameAs(subscriber);

        UniAssertSubscriber<Integer> tmp = subscriber;
        Consumer<Throwable> failedValidation = t -> assertThat(t).isInstanceOf(IOException.class);
        Consumer<Throwable> passedValidation = t -> assertThat(t).isInstanceOf(TestException.class);

        assertThatThrownBy(() -> tmp.awaitFailure(failedValidation))
                .isInstanceOf(AssertionError.class).hasMessageContaining("validation");

        // Delay
        subscriber = Uni.createFrom().<Integer> failure(new TestException())
                .onFailure().call(() -> Uni.createFrom().voidItem()
                        .onItem().delayIt().by(Duration.ofSeconds(1)))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertThat(subscriber.awaitFailure()).isSameAs(subscriber);

        subscriber = Uni.createFrom().<Integer> failure(new TestException())
                .onFailure().call(() -> Uni.createFrom().item(0)
                        .onItem().delayIt().by(Duration.ofSeconds(1)))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertThat(subscriber.awaitFailure(passedValidation)).isSameAs(subscriber);

        subscriber = Uni.createFrom().<Integer> failure(new TestException())
                .onFailure().call(() -> Uni.createFrom().voidItem()
                        .onItem().delayIt().by(Duration.ofSeconds(1)))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertThat(subscriber.awaitFailure(Duration.ofSeconds(5))).isSameAs(subscriber);

        // timeout
        subscriber = Uni.createFrom().<Integer> failure(new TestException())
                .onFailure().call(() -> Uni.createFrom().voidItem()
                        .onItem().delayIt().by(Duration.ofSeconds(10)))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Integer> tmp2 = subscriber;
        assertThatThrownBy(() -> tmp2.awaitFailure(Duration.ofMillis(100))).isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure").hasMessageContaining("100 ms");
    }

    @Test
    public void testFailureWithNoMessage() {
        Uni.createFrom().failure(new TimeoutException())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure()
                .assertFailedWith(TimeoutException.class);
    }
}
