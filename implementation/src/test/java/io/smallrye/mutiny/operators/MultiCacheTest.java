package io.smallrye.mutiny.operators;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiCacheTest {

    @Test
    public void testCachingWithResultsAndCompletion() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().deferred(() -> Multi.createFrom().items(count.incrementAndGet(),
                count.incrementAndGet()))
                .cache();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(2))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2);

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2);
    }

    @Test
    public void testCachingWithFailure() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().<Integer> emitter(emitter -> emitter.emit(count.incrementAndGet())
                .emit(count.incrementAndGet())
                .fail(new IOException("boom-" + count.incrementAndGet())))
                .cache();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(2))
                .assertReceived(1, 2)
                .assertHasFailedWith(IOException.class, "boom-3");

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .assertReceived(1, 2)
                .assertHasFailedWith(IOException.class, "boom-3");
    }

    @Test
    public void testCachingWithDeferredResult() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<MultiEmitter<? super Integer>> reference = new AtomicReference<>();
        Multi<Integer> multi = Multi.createFrom().<Integer> emitter(emitter -> {
            reference.set(emitter);
            emitter.emit(count.incrementAndGet())
                    .emit(count.incrementAndGet());
        })
                .cache();
        MultiAssertSubscriber<Integer> s1 = multi
                .subscribe().withSubscriber(MultiAssertSubscriber.create(2))
                .assertReceived(1, 2)
                .assertNotTerminated();

        MultiAssertSubscriber<Integer> s2 = multi.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .assertReceived(1, 2)
                .assertNotTerminated();

        reference.get().emit(count.incrementAndGet()).complete();
        s1.assertReceived(1, 2).request(1).assertReceived(1, 2, 3).assertCompletedSuccessfully();
        s2.assertReceived(1, 2, 3).assertCompletedSuccessfully();
    }
}
