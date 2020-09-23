package io.smallrye.mutiny.helpers.spies;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

class SpyUniTest {

    @Test
    @DisplayName("Spy onSubscribe()")
    void testOnSubscribeSpy() {
        UniOnSubscribeSpy<Integer> spy = SpyUni.onSubscribe(Uni.createFrom().item(69));
        UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompletedSuccessfully().assertItem(69);
        assertThat(spy.invoked()).isTrue();
        assertThat(spy.invocationCount()).isEqualTo(1);
        UniSubscription firstSubscription = spy.lastSubscription();
        assertThat(firstSubscription).isNotNull();

        subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompletedSuccessfully().assertItem(69);
        assertThat(spy.invoked()).isTrue();
        assertThat(spy.invocationCount()).isEqualTo(2);
        UniSubscription secondSubscription = spy.lastSubscription();
        assertThat(secondSubscription).isNotNull();
        assertThat(firstSubscription).isNotSameAs(secondSubscription);
    }
}