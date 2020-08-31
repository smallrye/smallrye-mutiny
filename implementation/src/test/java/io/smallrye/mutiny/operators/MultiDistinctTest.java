package io.smallrye.mutiny.operators;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiDistinctTest {

    @Test
    public void testDistinctWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .transform().byDroppingDuplicates()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDistinct() {
        Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .transform().byDroppingDuplicates()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testThatNullSubscriberAreRejected() {
        assertThrows(NullPointerException.class, () -> Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .transform().byDroppingDuplicates()
                .subscribe(null));
    }

    @Test
    public void testDistinctOnAStreamWithoutDuplicates() {
        Multi.createFrom().range(1, 5)
                .transform().byDroppingDuplicates()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testDropRepetitionsWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDropRepetitions() {
        Multi.createFrom().items(1, 2, 3, 4, 4, 2, 2, 4, 1, 1, 2, 4)
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 2, 4, 1, 2, 4);
    }

    @Test
    public void testDropRepetitionsOnAStreamWithoutDuplicates() {
        Multi.createFrom().range(1, 5)
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testNoEmissionAfterCancellation() {
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().emitter(
                (Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .transform().byDroppingDuplicates()
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        emitter.get().emit(1).emit(2).emit(1);
        subscriber.assertReceived(1, 2);

        subscriber.cancel();
        emitter.get().emit(1).emit(3).emit(4);
        subscriber.assertReceived(1, 2);
    }

    @Test
    public void testExceptionInComparator() {
        AtomicReference<MultiEmitter<? super BadlyComparableStuff>> emitter = new AtomicReference<>();
        AssertSubscriber<BadlyComparableStuff> subscriber = Multi.createFrom().emitter(
                (Consumer<MultiEmitter<? super BadlyComparableStuff>>) emitter::set)
                .transform().byDroppingDuplicates()
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        BadlyComparableStuff item1 = new BadlyComparableStuff();
        BadlyComparableStuff item2 = new BadlyComparableStuff();
        emitter.get().emit(item1).emit(item2).complete();
        subscriber.assertHasFailedWith(TestException.class, "boom");
    }

    private static class BadlyComparableStuff {

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        @Override
        public boolean equals(Object obj) {
            return Objects.equals(obj, this);
        }

        @Override
        public int hashCode() {
            throw new TestException("boom");
        }
    }

}
