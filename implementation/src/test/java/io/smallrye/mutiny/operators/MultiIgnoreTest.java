package io.smallrye.mutiny.operators;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiIgnoreTest {

    @Test
    public void test() {
        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().ignore()
                .subscribe().with(MultiAssertSubscriber.create(4))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testAsUni() {
        CompletableFuture<Void> future = Multi.createFrom().items(1, 2, 3, 4)
                .onItem().ignoreAsUni()
                .subscribeAsCompletionStage();
        assertThat(future.join()).isNull();
    }

    @Test(expectedExceptions = CompletionException.class, expectedExceptionsMessageRegExp = ".*boom.*")
    public void testAsUniWithFailure() {
        CompletableFuture<Void> future = Multi.createFrom().items(1, 2, 3, 4)
                .onItem().mapToItem(i -> {
                    if (i == 3) {
                        throw new RuntimeException("boom");
                    }
                    return i;
                })
                .onItem().ignoreAsUni()
                .subscribeAsCompletionStage();
        assertThat(future.join()).isNull();
    }

    @Test
    public void testWithNever() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom().nothing()
                .onItem().ignore()
                .subscribe().with(MultiAssertSubscriber.create(4))
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
}
