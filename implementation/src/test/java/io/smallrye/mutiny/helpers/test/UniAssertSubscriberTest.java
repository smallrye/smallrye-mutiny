package io.smallrye.mutiny.helpers.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

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

        subscriber.await();
        subscriber.assertCompleted();
        subscriber.assertTerminated();
        subscriber.assertItem(123);
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
        subscriber.onItem("Yo");
        subscriber.onItem("Yo");

        assertThatThrownBy(subscriber::assertSignalsReceivedInOrder)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("There are more than 1 onItem signals");
    }

    @Test
    public void testDoubleOnFailure() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        subscriber.onSubscribe(() -> {
        });
        subscriber.onFailure(new RuntimeException("Yo"));
        subscriber.onFailure(new RuntimeException("Yo"));

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
}
