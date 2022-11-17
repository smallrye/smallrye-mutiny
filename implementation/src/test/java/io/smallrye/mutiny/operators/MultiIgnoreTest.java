package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.MultiSubscribers;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.multi.MultiIgnoreOp;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.test.Mocks;

public class MultiIgnoreTest {

    @Test
    public void test() {
        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().ignore()
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testAsUni() {
        CompletableFuture<Void> future = Multi.createFrom().items(1, 2, 3, 4)
                .onItem().ignoreAsUni()
                .subscribeAsCompletionStage();
        assertThat(future.join()).isNull();
    }

    @Test
    public void testAsUniWithFailure() {
        assertThrows(CompletionException.class, () -> {
            CompletableFuture<Void> future = Multi.createFrom().items(1, 2, 3, 4)
                    .onItem().transform(i -> {
                        if (i == 3) {
                            throw new RuntimeException("boom");
                        }
                        return i;
                    })
                    .onItem().ignoreAsUni()
                    .subscribeAsCompletionStage();
            assertThat(future.join()).isNull();
        });
    }

    @Test
    public void testWithNever() {
        AssertSubscriber<Void> subscriber = Multi.createFrom().nothing()
                .onItem().ignore()
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .assertNotTerminated();

        subscriber.cancel();
    }

    @Test
    public void testAsUniWithNever() {
        CompletableFuture<Void> future = Multi.createFrom().nothing()
                .onItem().ignoreAsUni().subscribeAsCompletionStage();

        assertThat(future).isNotCompleted();
        future.cancel(true);
    }

    @Test
    public void testSubscriberCannotBeNull() {
        assertThrows(NullPointerException.class, () -> {
            MultiIgnoreOp<Integer> ignore = new MultiIgnoreOp<>(Multi.createFrom().items(1, 2, 3, 4));
            ignore.subscribe(null);
        });
    }

    @Test
    public void testSingleSubscriberAcceptedAndSingleRequest() {
        Subscription subscription1 = mock(Subscription.class);
        Subscription subscription2 = mock(Subscription.class);
        Flow.Subscriber<Void> mock = Mocks.subscriber();
        MultiSubscriber<Void> subscriber = MultiSubscribers.toMultiSubscriber(mock);
        MultiIgnoreOp.MultiIgnoreProcessor<Integer> ignore = new MultiIgnoreOp.MultiIgnoreProcessor<>(subscriber);
        ignore.onSubscribe(subscription1);
        ignore.onSubscribe(subscription2);

        verify(subscription1).request(Long.MAX_VALUE);
        verify(subscription1, never()).cancel();
        verify(mock).onSubscribe(ignore);
        verify(subscription2, never()).request(anyLong());
        verify(subscription2).cancel();
    }

    @Test
    public void testIllegalRequests() {
        AssertSubscriber<Void> subscriber = Multi.createFrom().nothing()
                .onItem().ignore()
                .subscribe().withSubscriber(AssertSubscriber.create(0));

        subscriber
                .request(-1)
                .assertFailedWith(IllegalArgumentException.class, null);
    }
}
