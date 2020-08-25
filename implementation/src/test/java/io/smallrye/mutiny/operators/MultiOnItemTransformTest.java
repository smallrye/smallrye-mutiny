package io.smallrye.mutiny.operators;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.MultiMapOp;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiOnItemTransformTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMapperCannotBeNull() {
        new MultiMapOp<>(Multi.createFrom().item(1), null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testUpstreamCannotBeNull() {
        new MultiMapOp<>(null, x -> x);
    }

    @Test
    public void testMapperMustNotReturnNull() {
        Multi.createFrom().items(1, 2, 3)
                .onItem().transform(i -> {
                    if (i == 2) {
                        return null;
                    }
                    return i + 1;
                })
                .subscribe().withSubscriber(AssertSubscriber.create(3))
                .assertHasFailedWith(NullPointerException.class, "")
                .assertReceived(2);
    }

    @Test
    public void testOnUpstreamFailure() {
        Multi<Integer> upstream1 = Multi.createFrom().items(1, 2);
        Multi<Integer> upstream2 = Multi.createFrom().failure(new IOException("boom"));

        Multi.createBy().concatenating().streams(upstream1, upstream2)
                .onItem().transform(i -> i + 1)
                .subscribe().withSubscriber(AssertSubscriber.create(3))
                .assertHasFailedWith(IOException.class, "boom")
                .assertReceived(2, 3);
    }

    @Test
    public void testNormal() {
        Multi.createFrom().items(1, 2, 3)
                .onItem().transform(i -> i + 1)
                .subscribe().withSubscriber(AssertSubscriber.create(3))
                .assertCompletedSuccessfully()
                .assertReceived(2, 3, 4);
    }

    @Test
    public void testNormalBackPressure() {
        Multi.createFrom().items(1, 2, 3)
                .onItem().transform(i -> i + 1)
                .subscribe().withSubscriber(AssertSubscriber.create())
                .assertNotTerminated()
                .assertSubscribed()
                .request(2)
                .assertReceived(2, 3)
                .assertNotTerminated()
                .request(2)
                .assertReceived(2, 3, 4)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testMapperThrowingException() {
        Multi.createFrom().items(1, 2, 3)
                .onItem().transform(i -> {
                    if (i == 2) {
                        throw new ArithmeticException("boom");
                    }
                    return i + 1;
                })
                .subscribe().withSubscriber(AssertSubscriber.create(3))
                .assertHasFailedWith(ArithmeticException.class, "boom")
                .assertReceived(2);
    }

    @Test
    public void testMapperNotCalledAfterCancellation() {
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        Multi.createFrom().<Integer> emitter(emitter::set)
                .onItem().transform(i -> i + 1)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertNotTerminated()
                .assertSubscribed()
                .assertHasNotReceivedAnyItem()
                .run(() -> emitter.get().emit(1).emit(2))
                .assertNotTerminated()
                .assertReceived(2)
                .cancel()
                .assertNotTerminated()
                .assertReceived(2)
                .run(() -> emitter.get().emit(3).complete())
                .assertNotTerminated()
                .assertReceived(2);

        Multi.createFrom().<Integer> emitter(emitter::set)
                .onItem().transform(i -> i + 1)
                .subscribe().withSubscriber(AssertSubscriber.create(2))
                .assertNotTerminated()
                .assertSubscribed()
                .assertHasNotReceivedAnyItem()
                .run(() -> emitter.get().emit(1).emit(2))
                .assertNotTerminated()
                .assertReceived(2, 3)
                .cancel()
                .assertNotTerminated()
                .assertReceived(2, 3)
                .run(() -> emitter.get().emit(3).fail(new IOException("boom")))
                .assertNotTerminated()
                .assertReceived(2, 3);
    }

}
