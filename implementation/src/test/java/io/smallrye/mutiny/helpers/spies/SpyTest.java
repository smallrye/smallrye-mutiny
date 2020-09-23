package io.smallrye.mutiny.helpers.spies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

class SpyTest {

    @DisplayName("Spy on Uni")
    @Nested
    class SpyUni {
        @Test
        @DisplayName("Spy onSubscribe()")
        void spyOnSubscribeSpy() {
            UniOnSubscribeSpy<Integer> spy = Spy.onSubscribe(Uni.createFrom().item(69));
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

        @Test
        @DisplayName("Spy onCancellation()")
        void spyOnCancellation() {
            UniOnCancellationSpy<Integer> spy = Spy.onCancellation(Uni.createFrom().emitter(e -> {
            }));
            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.cancel();
            subscriber.assertNotCompleted();
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Spy onTermination() when an item is emitted")
        void spyOnTerminationEmitted() {
            UniOnTerminationSpy<Integer> spy = Spy.onTermination(Uni.createFrom().item(69));

            assertThatThrownBy(spy::lastTerminationItem).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(spy::lastTerminationFailure).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(spy::lastTerminationWasCancelled).isInstanceOf(IllegalStateException.class);

            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());
            subscriber.assertCompletedSuccessfully().assertItem(69);
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastTerminationItem()).isEqualTo(69);
            assertThat(spy.lastTerminationFailure()).isNull();
            assertThat(spy.lastTerminationWasCancelled()).isFalse();
        }

        @Test
        @DisplayName("Spy onTermination() when a failure is emitted")
        void spyOnTerminationFailure() {
            UniOnTerminationSpy<Integer> spy = Spy.onTermination(Uni.createFrom().failure(new IOException("boom")));

            assertThatThrownBy(spy::lastTerminationItem).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(spy::lastTerminationFailure).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(spy::lastTerminationWasCancelled).isInstanceOf(IllegalStateException.class);

            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());
            subscriber.assertFailure(IOException.class, "boom");
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastTerminationItem()).isNull();
            assertThat(spy.lastTerminationFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");
            assertThat(spy.lastTerminationWasCancelled()).isFalse();
        }

        @Test
        @DisplayName("Spy onTermination() when the subscription is cancelled")
        void spyOnTerminationCancelled() {
            UniOnTerminationSpy<Integer> spy = Spy.onTermination(Uni.createFrom().emitter(e -> {
            }));

            assertThatThrownBy(spy::lastTerminationItem).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(spy::lastTerminationFailure).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(spy::lastTerminationWasCancelled).isInstanceOf(IllegalStateException.class);

            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());
            subscriber.cancel();
            subscriber.assertNotCompleted();
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastTerminationItem()).isNull();
            assertThat(spy.lastTerminationFailure()).isNull();
            assertThat(spy.lastTerminationWasCancelled()).isTrue();
        }

        @Test
        @DisplayName("Spy onItem()")
        void spyOnItem() {
            UniOnItemSpy<Integer> spy = Spy.onItem(Uni.createFrom().item(69));
            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertCompletedSuccessfully().assertItem(69);
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastItem()).isEqualTo(69);
        }

        @Test
        @DisplayName("Spy onItemOrFailure() when an item is emitted")
        void spyOnItemOrFailureItem() {
            UniOnItemOrFailureSpy<Integer> spy = Spy.onItemOrFailure(Uni.createFrom().item(69));
            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertCompletedSuccessfully().assertItem(69);
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.hasFailed()).isFalse();
            assertThat(spy.lastItem()).isEqualTo(69);
            assertThat(spy.lastFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onItemOrFailure() when a failure is emitted")
        void spyOnItemOrFailureFailure() {
            UniOnItemOrFailureSpy<Integer> spy = Spy.onItemOrFailure(Uni.createFrom().failure(new IOException("boom")));
            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertFailure(IOException.class, "boom");
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.hasFailed()).isTrue();
            assertThat(spy.lastItem()).isNull();
            assertThat(spy.lastFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");
        }

        @Test
        @DisplayName("Spy onFailure() with no selector")
        void spyOnFailure() {
            UniOnFailureSpy<Object> spy = Spy.onFailure(Uni.createFrom().failure(new IOException("boom")));
            UniAssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertFailure(IOException.class, "boom");
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");
        }

        @Test
        @DisplayName("Spy onFailure() with a matching class selector")
        void spyOnFailureClassMatching() {
            UniOnFailureSpy<Object> spy = Spy.onFailure(Uni.createFrom().failure(new IOException("boom")), IOException.class);
            UniAssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertFailure(IOException.class, "boom");
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");
        }

        @Test
        @DisplayName("Spy onFailure() with a matching predicate selector")
        void spyOnFailurePredicatedMatching() {
            UniOnFailureSpy<Object> spy = Spy.onFailure(Uni.createFrom().failure(new IOException("boom")), t -> true);
            UniAssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertFailure(IOException.class, "boom");
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");
        }

        @Test
        @DisplayName("Spy onFailure() with a non-matching class selector")
        void spyOnFailureClassNotMatching() {
            UniOnFailureSpy<Object> spy = Spy.onFailure(Uni.createFrom().failure(new IOException("boom")),
                    RuntimeException.class);
            UniAssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertFailure(IOException.class, "boom");
            assertThat(spy.invoked()).isFalse();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onFailure() with a non-matching predicate selector")
        void spyOnFailurePredicatedNotMatching() {
            UniOnFailureSpy<Object> spy = Spy.onFailure(Uni.createFrom().failure(new IOException("boom")), t -> false);
            UniAssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertFailure(IOException.class, "boom");
            assertThat(spy.invoked()).isFalse();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();
        }
    }
}