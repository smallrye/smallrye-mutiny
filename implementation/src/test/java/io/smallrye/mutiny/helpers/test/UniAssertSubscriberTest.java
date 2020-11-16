package io.smallrye.mutiny.helpers.test;

import java.io.IOException;
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
        Assertions.assertThat(subscriber.getItem()).isEqualTo(123);
        Assertions.assertThat(subscriber.getFailure()).isNull();
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
        Assertions.assertThat(subscriber.getItem()).isNull();
        Assertions.assertThat(subscriber.getFailure()).isNull();
    }

    @Test
    void testFailure() {
        UniAssertSubscriber<Object> subscriber = Uni.createFrom().failure(new IOException("boom"))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber
                .assertFailed()
                .assertFailedWith(IOException.class, "boom");
        Assertions.assertThat(subscriber.getItem()).isNull();
        Assertions.assertThat(subscriber.getFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");
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
}