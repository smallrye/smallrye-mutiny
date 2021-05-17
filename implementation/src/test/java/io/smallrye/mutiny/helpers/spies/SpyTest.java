package io.smallrye.mutiny.helpers.spies;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.MultiOverflowStrategy;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

class SpyTest {

    // --------------------------------------------------------------------- //

    @DisplayName("Spy on Uni")
    @Nested
    class SpyUni {
        @Test
        @DisplayName("Spy onSubscription()")
        void spyOnSubscribeSpy() {
            UniOnSubscribeSpy<Integer> spy = Spy.onSubscribe(Uni.createFrom().item(69));
            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertCompleted().assertItem(69);
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            UniSubscription firstSubscription = spy.lastSubscription();
            assertThat(firstSubscription).isNotNull();

            subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());
            subscriber.assertCompleted().assertItem(69);
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(2);
            UniSubscription secondSubscription = spy.lastSubscription();
            assertThat(secondSubscription).isNotNull();

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastSubscription()).isNull();
        }

        @Test
        @DisplayName("Spy onCancellation()")
        void spyOnCancellation() {
            UniOnCancellationSpy<Integer> spy = Spy.onCancellation(Uni.createFrom().emitter(e -> {
                // Do not emit anything
            }));
            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.cancel();
            subscriber.assertNotTerminated();
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Spy onTermination() when an item is emitted")
        void spyOnTerminationEmitted() {
            UniOnTerminationSpy<Integer> spy = Spy.onTermination(Uni.createFrom().item(69));

            assertThat(spy.lastTerminationItem()).isNull();
            assertThat(spy.lastTerminationWasCancelled()).isFalse();
            assertThat(spy.lastTerminationFailure()).isNull();

            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());
            subscriber.assertCompleted().assertItem(69);
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastTerminationItem()).isEqualTo(69);
            assertThat(spy.lastTerminationFailure()).isNull();
            assertThat(spy.lastTerminationWasCancelled()).isFalse();

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastTerminationItem()).isNull();
            assertThat(spy.lastTerminationWasCancelled()).isFalse();
            assertThat(spy.lastTerminationFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onTermination() when a failure is emitted")
        void spyOnTerminationFailure() {
            UniOnTerminationSpy<Integer> spy = Spy.onTermination(Uni.createFrom().failure(new IOException("boom")));

            assertThat(spy.lastTerminationItem()).isNull();
            assertThat(spy.lastTerminationWasCancelled()).isFalse();
            assertThat(spy.lastTerminationFailure()).isNull();

            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());
            subscriber.assertFailedWith(IOException.class, "boom");
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastTerminationItem()).isNull();
            assertThat(spy.lastTerminationFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");
            assertThat(spy.lastTerminationWasCancelled()).isFalse();

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastTerminationItem()).isNull();
            assertThat(spy.lastTerminationWasCancelled()).isFalse();
            assertThat(spy.lastTerminationFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onTermination() when the subscription is cancelled")
        void spyOnTerminationCancelled() {
            UniOnTerminationSpy<Integer> spy = Spy.onTermination(Uni.createFrom().emitter(e -> {
                // Do not emit anything
            }));

            assertThat(spy.lastTerminationItem()).isNull();
            assertThat(spy.lastTerminationWasCancelled()).isFalse();
            assertThat(spy.lastTerminationFailure()).isNull();

            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());
            subscriber.cancel();
            subscriber.assertNotTerminated();
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastTerminationItem()).isNull();
            assertThat(spy.lastTerminationFailure()).isNull();
            assertThat(spy.lastTerminationWasCancelled()).isTrue();

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastTerminationItem()).isNull();
            assertThat(spy.lastTerminationWasCancelled()).isFalse();
            assertThat(spy.lastTerminationFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onItem()")
        void spyOnItem() {
            UniOnItemSpy<Integer> spy = Spy.onItem(Uni.createFrom().item(69));
            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertCompleted().assertItem(69);
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastItem()).isEqualTo(69);

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastItem()).isNull();
        }

        @Test
        @DisplayName("Spy onItemOrFailure() when an item is emitted")
        void spyOnItemOrFailureItem() {
            UniOnItemOrFailureSpy<Integer> spy = Spy.onItemOrFailure(Uni.createFrom().item(69));
            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertCompleted().assertItem(69);
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.hasFailed()).isFalse();
            assertThat(spy.lastItem()).isEqualTo(69);
            assertThat(spy.lastFailure()).isNull();

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();
            assertThat(spy.lastItem()).isNull();
        }

        @Test
        @DisplayName("Spy onItemOrFailure() when a failure is emitted")
        void spyOnItemOrFailureFailure() {
            UniOnItemOrFailureSpy<Integer> spy = Spy.onItemOrFailure(Uni.createFrom().failure(new IOException("boom")));
            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertFailedWith(IOException.class, "boom");
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.hasFailed()).isTrue();
            assertThat(spy.lastItem()).isNull();
            assertThat(spy.lastFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();
            assertThat(spy.lastItem()).isNull();
        }

        @Test
        @DisplayName("Spy onFailure() with no selector")
        void spyOnFailure() {
            UniOnFailureSpy<Object> spy = Spy.onFailure(Uni.createFrom().failure(new IOException("boom")));
            UniAssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertFailedWith(IOException.class, "boom");
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onFailure() with a matching class selector")
        void spyOnFailureClassMatching() {
            UniOnFailureSpy<Object> spy = Spy.onFailure(Uni.createFrom().failure(new IOException("boom")), IOException.class);
            UniAssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertFailedWith(IOException.class, "boom");
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onFailure() with a matching predicate selector")
        void spyOnFailurePredicatedMatching() {
            UniOnFailureSpy<Object> spy = Spy.onFailure(Uni.createFrom().failure(new IOException("boom")), t -> true);
            UniAssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertFailedWith(IOException.class, "boom");
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onFailure() with a non-matching class selector")
        void spyOnFailureClassNotMatching() {
            UniOnFailureSpy<Object> spy = Spy.onFailure(Uni.createFrom().failure(new IOException("boom")),
                    RuntimeException.class);
            UniAssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertFailedWith(IOException.class, "boom");
            assertThat(spy.invoked()).isFalse();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onFailure() with a non-matching predicate selector")
        void spyOnFailurePredicatedNotMatching() {
            UniOnFailureSpy<Object> spy = Spy.onFailure(Uni.createFrom().failure(new IOException("boom")), t -> false);
            UniAssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertFailedWith(IOException.class, "boom");
            assertThat(spy.invoked()).isFalse();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();
        }

        @Test
        @DisplayName("Spy globally")
        void globally() {
            UniGlobalSpy<Integer> spy = Spy.globally(Uni.createFrom().item(69));
            UniAssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertCompleted().assertItem(69);
            assertThat(spy.onCancellationSpy().invoked()).isFalse();
            assertThat(spy.onFailureSpy().invoked()).isFalse();
            assertThat(spy.onItemSpy().invoked()).isTrue();
            assertThat(spy.onItemOrFailureSpy().invoked()).isTrue();
            assertThat(spy.onSubscribeSpy().invoked()).isTrue();
            assertThat(spy.onTerminationSpy().invoked()).isTrue();

            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(4);

            assertThat(spy.toString())
                    .contains("{lastItem=69, lastFailure=null}")
                    .contains("Tuple{item1=69,item2=null,item3=false}");

            spy.reset();
            assertThat(spy.invoked()).isFalse();
            assertThat(spy.invocationCount()).isEqualTo(0);
        }
    }

    // --------------------------------------------------------------------- //

    @DisplayName("Spy on Multi")
    @Nested
    class SpyMulti {

        @Test
        @DisplayName("Spy onCancellation()")
        void spyOnCancellation() {
            MultiOnCancellationSpy<Object> spy = Spy.onCancellation(Multi.createFrom().emitter(e -> {
                // Do not emit anything
            }));
            AssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create());
            subscriber.cancel();

            subscriber.assertNotTerminated().assertHasNotReceivedAnyItem();
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Spy onCompletion()")
        void spyOnCompletion() {
            MultiOnCompletionSpy<Integer> spy = Spy.onCompletion(Multi.createFrom().item(69));
            AssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(10));

            subscriber.assertCompleted();
            assertThat(subscriber.getItems()).containsExactly(69);
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Spy onFailure() with no selector")
        void spyOnFailure() {
            MultiOnFailureSpy<Object> spy = Spy.onFailure(Multi.createFrom().failure(new IOException("boom")));
            AssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(10));

            subscriber.assertFailedWith(IOException.class, "boom");
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onFailure() with a matching class selector")
        void spyOnFailureClassMatching() {
            MultiOnFailureSpy<Object> spy = Spy.onFailure(Multi.createFrom().failure(new IOException("boom")),
                    IOException.class);
            AssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(10));

            subscriber.assertFailedWith(IOException.class, "boom");
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onFailure() with a non-matching class selector")
        void spyOnFailureClassNotMatching() {
            MultiOnFailureSpy<Object> spy = Spy.onFailure(Multi.createFrom().failure(new IOException("boom")),
                    IllegalStateException.class);
            AssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(10));

            subscriber.assertFailedWith(IOException.class, "boom");
            assertThat(spy.invoked()).isFalse();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onFailure() with a matching predicate selector")
        void spyOnFailurePredicateMatching() {
            MultiOnFailureSpy<Object> spy = Spy.onFailure(Multi.createFrom().failure(new IOException("boom")), t -> true);
            AssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(10));

            subscriber.assertFailedWith(IOException.class, "boom");
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onFailure() with a non-matching predicate selector")
        void spyOnFailurePredicateNotMatching() {
            MultiOnFailureSpy<Object> spy = Spy.onFailure(Multi.createFrom().failure(new IOException("boom")), t -> false);
            AssertSubscriber<Object> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(10));

            subscriber.assertFailedWith(IOException.class, "boom");
            assertThat(spy.invoked()).isFalse();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onItem()")
        void spyOnItem() {
            MultiOnItemSpy<Integer> spy = Spy.onItem(Multi.createFrom().items(1, 2, 3));
            AssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(10));

            subscriber.assertCompleted();
            assertThat(subscriber.getItems()).containsExactly(1, 2, 3);
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(3);
            assertThat(spy.items()).containsExactly(1, 2, 3);

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.items()).isEmpty();
        }

        @Test
        @DisplayName("Spy onItem() and track items")
        void spyOnItemTrackItems() {
            MultiOnItemSpy<Integer> spy = Spy.onItem(Multi.createFrom().items(1, 2, 3), true);
            AssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(10));

            subscriber.assertCompleted();
            assertThat(subscriber.getItems()).containsExactly(1, 2, 3);
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(3);
            assertThat(spy.items()).containsExactly(1, 2, 3);

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.items()).isEmpty();
        }

        @Test
        @DisplayName("Spy onItem() and do not track items")
        void spyOnItemDoNotTrackItems() {
            MultiOnItemSpy<Integer> spy = Spy.onItem(Multi.createFrom().items(1, 2, 3), false);
            AssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(10));

            subscriber.assertCompleted();
            assertThat(subscriber.getItems()).containsExactly(1, 2, 3);
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(3);
            assertThat(spy.items()).isEmpty();

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Spy onRequest()")
        void spyOnRequest() {
            MultiOnRequestSpy<Integer> spy = Spy.onRequest(Multi.createFrom().items(1, 2, 3));
            AssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(10));
            subscriber.request(5);

            subscriber.assertCompleted();
            assertThat(subscriber.getItems()).containsExactly(1, 2, 3);
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(2);
            assertThat(spy.requestedCount()).isEqualTo(15);

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.requestedCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Spy onSubscription()")
        void spyOnSubscribe() {
            MultiOnSubscribeSpy<Integer> spy = Spy.onSubscribe(Multi.createFrom().items(1, 2, 3));
            AssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(10));

            subscriber.assertCompleted();
            assertThat(subscriber.getItems()).containsExactly(1, 2, 3);
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastSubscription()).isNotNull();

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastSubscription()).isNull();
        }

        @Test
        @DisplayName("Spy onTermination() with completion")
        void spyOnTerminationComplete() {
            MultiOnTerminationSpy<Integer> spy = Spy.onTermination(Multi.createFrom().item(69));

            assertThat(spy.lastTerminationWasCancelled()).isFalse();
            assertThat(spy.lastTerminationFailure()).isNull();

            AssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(10));

            subscriber.assertCompleted();
            assertThat(subscriber.getItems()).containsExactly(69);
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastTerminationFailure()).isNull();
            assertThat(spy.lastTerminationWasCancelled()).isFalse();

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastTerminationWasCancelled()).isFalse();
            assertThat(spy.lastTerminationFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onTermination() with failure")
        void spyOnTerminationFailure() {
            MultiOnTerminationSpy<Integer> spy = Spy.onTermination(Multi.createFrom().failure(new IOException("boom")));

            assertThat(spy.lastTerminationWasCancelled()).isFalse();
            assertThat(spy.lastTerminationFailure()).isNull();

            AssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(10));

            subscriber.assertFailedWith(IOException.class, "boom");
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastTerminationFailure()).isNotNull().isInstanceOf(IOException.class).hasMessage("boom");
            assertThat(spy.lastTerminationWasCancelled()).isFalse();

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastTerminationWasCancelled()).isFalse();
            assertThat(spy.lastTerminationFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onTermination() with cancellation")
        void spyOnTerminationCancellation() {
            MultiOnTerminationSpy<Integer> spy = Spy.onTermination(Multi.createFrom().item(69));

            assertThat(spy.lastTerminationWasCancelled()).isFalse();
            assertThat(spy.lastTerminationFailure()).isNull();

            AssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create());
            subscriber.cancel();

            subscriber.assertHasNotReceivedAnyItem().assertNotTerminated();
            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(1);
            assertThat(spy.lastTerminationFailure()).isNull();
            assertThat(spy.lastTerminationWasCancelled()).isTrue();

            spy.reset();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.lastTerminationWasCancelled()).isFalse();
            assertThat(spy.lastTerminationFailure()).isNull();
        }

        @Test
        @DisplayName("Spy onOverflow() and track items")
        void spyOnOverflowTrackItems() {
            Multi<Integer> multi = Multi.createFrom().range(1, 10);
            MultiOnOverflowSpy<Integer> spy = Spy.onOverflow(multi, MultiOverflowStrategy::drop);

            assertThat(spy.invoked()).isFalse();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.droppedItems()).isEmpty();

            AssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(5));

            subscriber.assertCompleted().assertItems(1, 2, 3, 4, 5);

            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(4);
            assertThat(spy.droppedItems()).containsExactly(6, 7, 8, 9);

            spy.reset();
            assertThat(spy.invoked()).isFalse();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.droppedItems()).isEmpty();
        }

        @Test
        @DisplayName("Spy onOverflow() but do not track items")
        void spyOnOverflowDoNotTrackItems() {
            Multi<Integer> multi = Multi.createFrom().range(1, 10);
            MultiOnOverflowSpy<Integer> spy = Spy.onOverflow(multi, false, MultiOverflowStrategy::drop);

            assertThat(spy.invoked()).isFalse();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.droppedItems()).isEmpty();

            AssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(5));

            subscriber.assertCompleted().assertItems(1, 2, 3, 4, 5);

            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(4);
            assertThat(spy.droppedItems()).isEmpty();

            spy.reset();
            assertThat(spy.invoked()).isFalse();
            assertThat(spy.invocationCount()).isEqualTo(0);
            assertThat(spy.droppedItems()).isEmpty();
        }

        @Test
        @DisplayName("Spy globally")
        void globally() {
            MultiGlobalSpy<Integer> spy = Spy.globally(Multi.createFrom().items(1, 2, 3));
            AssertSubscriber<Integer> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create());

            subscriber.request(1);
            subscriber.request(10);

            subscriber.assertCompleted();
            assertThat(subscriber.getItems()).containsExactly(1, 2, 3);

            assertThat(spy.onCancellationSpy().invoked()).isFalse();
            assertThat(spy.onCompletionSpy().invoked()).isTrue();
            assertThat(spy.onFailureSpy().invoked()).isFalse();
            assertThat(spy.onItemSpy().invoked()).isTrue();
            assertThat(spy.onRequestSpy().invoked()).isTrue();
            assertThat(spy.onSubscribeSpy().invoked()).isTrue();
            assertThat(spy.onTerminationSpy().invoked()).isTrue();

            assertThat(spy.invoked()).isTrue();
            assertThat(spy.invocationCount()).isEqualTo(8);

            assertThat(spy.toString())
                    .contains("onItemSpy=MultiOnItemSpy{items=[1, 2, 3]}")
                    .contains("onRequestSpy=MultiOnRequestSpy{requestedCount=11}");

            spy.reset();
            assertThat(spy.invoked()).isFalse();
            assertThat(spy.invocationCount()).isEqualTo(0);
        }
    }
}
