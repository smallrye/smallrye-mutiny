package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.MultiEmitter;

@SuppressWarnings("ConstantConditions")
public class MultiDistinctTest {

    @Test
    public void testDistinctWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .transform().byDroppingDuplicates()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDistinct() {
        Multi.createFrom().items(1, 2, 3, 4, 2, 4, 2, 4)
                .transform().byDroppingDuplicates()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
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
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testDropRepetitionsWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDropRepetitions() {
        Multi.createFrom().items(1, 2, 3, 4, 4, 2, 2, 4, 1, 1, 2, 4)
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 2, 4, 1, 2, 4);
    }

    @Test
    public void testDropRepetitionsWithCancellation() {
        AtomicLong count = new AtomicLong();
        AtomicBoolean cancelled = new AtomicBoolean();
        AssertSubscriber<Long> subscriber = Multi.createFrom().ticks().every(Duration.ofMillis(1))
                .onCancellation().invoke(() -> cancelled.set(true))
                .onItem().transform(l -> {
                    if (count.getAndIncrement() % 2 == 0) {
                        return l;
                    } else {
                        return l - 1;
                    }
                })
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        await().until(() -> subscriber.getItems().size() >= 10);
        subscriber.cancel();
        assertThat(cancelled).isTrue();
    }

    @Test
    public void testDropRepetitionsWithImmediateCancellation() {
        AtomicLong count = new AtomicLong();
        AtomicBoolean cancelled = new AtomicBoolean();
        Multi.createFrom().ticks().every(Duration.ofMillis(1))
                .onCancellation().invoke(() -> cancelled.set(true))
                .onItem().transform(l -> {
                    if (count.getAndIncrement() % 2 == 0) {
                        return l;
                    } else {
                        return l - 1;
                    }
                })
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(new AssertSubscriber<>(Long.MAX_VALUE, true));

        assertThat(cancelled).isTrue();
        assertThat(count).hasValue(0);
    }

    @Test
    public void testDropRepetitionsOnAStreamWithoutDuplicates() {
        Multi.createFrom().range(1, 5)
                .transform().byDroppingRepetitions()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
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
        subscriber.assertItems(1, 2);

        subscriber.cancel();
        emitter.get().emit(1).emit(3).emit(4);
        subscriber.assertItems(1, 2);
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
        subscriber.assertFailedWith(TestException.class, "boom");
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
