package io.smallrye.mutiny.operators;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class MultiCacheTest {

    @Test
    public void testCachingWithResultsAndCompletion() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().deferred(() -> Multi.createFrom().items(count.incrementAndGet(),
                count.incrementAndGet()))
                .cache();
        multi.subscribe().withSubscriber(AssertSubscriber.create(2))
                .assertCompleted()
                .assertItems(1, 2);

        multi.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .assertCompleted()
                .assertItems(1, 2);
    }

    @Test
    public void testCachingWithFailure() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().<Integer> emitter(emitter -> emitter.emit(count.incrementAndGet())
                .emit(count.incrementAndGet())
                .fail(new IOException("boom-" + count.incrementAndGet())))
                .cache();
        multi.subscribe().withSubscriber(AssertSubscriber.create(2))
                .assertItems(1, 2)
                .assertFailedWith(IOException.class, "boom-3");

        multi.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .assertItems(1, 2)
                .assertFailedWith(IOException.class, "boom-3");
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
        AssertSubscriber<Integer> s1 = multi
                .subscribe().withSubscriber(AssertSubscriber.create(2))
                .assertItems(1, 2)
                .assertNotTerminated();

        AssertSubscriber<Integer> s2 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .assertItems(1, 2)
                .assertNotTerminated();

        reference.get().emit(count.incrementAndGet()).complete();
        s1.assertItems(1, 2).request(1).assertItems(1, 2, 3).assertCompleted();
        s2.assertItems(1, 2, 3).assertCompleted();
    }
}
