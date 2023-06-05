package io.smallrye.mutiny.operators;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiCreateFromGeneratorTest {

    @Test
    @DisplayName("Generate a finite sequence")
    void generateSuite() {
        AssertSubscriber<? super Integer> sub = Multi
                .createFrom()
                .generator(AtomicInteger::new, (counter, emitter) -> {
                    int n = counter.getAndIncrement();
                    emitter.emit(n);
                    if (n == 5) {
                        emitter.complete();
                    }
                    return counter;
                })
                .subscribe().withSubscriber(AssertSubscriber.create(20));

        sub.assertCompleted();
        sub.assertItems(0, 1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("Generate an infinite sequence and take only the first items")
    void generateInfiniteSuite() {
        AssertSubscriber<? super Integer> sub = Multi
                .createFrom()
                .generator(AtomicInteger::new, (counter, emitter) -> {
                    int n = counter.getAndIncrement();
                    emitter.emit(n);
                    return counter;
                })
                .select().first(6)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        sub.assertCompleted();
        sub.assertItems(0, 1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("Generate some items then cancel the subscription")
    void generateAndCancel() {
        AssertSubscriber<? super Integer> sub = Multi
                .createFrom()
                .generator(AtomicInteger::new, (counter, emitter) -> {
                    int n = counter.getAndIncrement();
                    emitter.emit(n);
                    return counter;
                })
                .select().first(6)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.assertNotTerminated();
        sub.assertHasNotReceivedAnyItem();

        sub.request(3);
        sub.assertNotTerminated();
        sub.assertItems(0, 1, 2);

        sub.request(1);
        sub.assertNotTerminated();
        sub.assertItems(0, 1, 2, 3);

        sub.cancel();
        sub.request(1);
        sub.assertNotTerminated();
        sub.assertItems(0, 1, 2, 3);
    }

    @Test
    @DisplayName("Cancel the subscription upfront")
    void cancelUpfront() {
        AssertSubscriber<? super Integer> sub = Multi
                .createFrom()
                .generator(AtomicInteger::new, (counter, emitter) -> {
                    int n = counter.getAndIncrement();
                    emitter.emit(n);
                    return counter;
                })
                .select().first(6)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.cancel();
        sub.request(10);
        sub.assertNotTerminated();
        sub.assertHasNotReceivedAnyItem();
    }

    @Test
    @DisplayName("Reject zero subscription requests")
    void rejectZeroSubscription() {
        AssertSubscriber<? super Integer> sub = Multi
                .createFrom()
                .generator(AtomicInteger::new, (counter, emitter) -> {
                    int n = counter.getAndIncrement();
                    emitter.emit(n);
                    return counter;
                })
                .select().first(6)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.request(0);
        sub.assertFailedWith(IllegalArgumentException.class, "must be greater than 0");
    }

    @Test
    @DisplayName("Reject negative subscription requests")
    void rejectNegativeSubscription() {
        AssertSubscriber<? super Integer> sub = Multi
                .createFrom()
                .generator(AtomicInteger::new, (counter, emitter) -> {
                    int n = counter.getAndIncrement();
                    emitter.emit(n);
                    return counter;
                })
                .select().first(6)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.request(-10);
        sub.assertFailedWith(IllegalArgumentException.class, "must be greater than 0");
    }

    @Test
    @DisplayName("Allow null state")
    void allowNullState() {
        AtomicInteger counter = new AtomicInteger();
        AssertSubscriber<? super Integer> sub = Multi
                .createFrom()
                .generator(() -> null, (nil, emitter) -> {
                    emitter.emit(counter.incrementAndGet());
                    if (counter.get() == 3) {
                        emitter.complete();
                    }
                    return nil;
                })
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        sub.assertCompleted();
        sub.assertItems(1, 2, 3);
    }

    @Test
    @DisplayName("Propagate emitted failures")
    void propagatesFailures() {
        AtomicInteger counter = new AtomicInteger();
        AssertSubscriber<? super Integer> sub = Multi
                .createFrom()
                .generator(() -> null, (nil, emitter) -> {
                    emitter.emit(counter.incrementAndGet());
                    if (counter.get() == 3) {
                        emitter.fail(new IOException("boom"));
                    }
                    return nil;
                })
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        sub.assertFailedWith(IOException.class, "boom");
        sub.assertItems(1, 2, 3);
    }

    @Test
    @DisplayName("Propagate exceptions thrown by the generator function")
    void propagatesGeneratorExceptions() {
        AtomicInteger counter = new AtomicInteger();
        AssertSubscriber<? super Integer> sub = Multi
                .createFrom()
                .generator(() -> null, (nil, emitter) -> {
                    emitter.emit(counter.incrementAndGet());
                    if (counter.get() == 3) {
                        throw new RuntimeException("boom");
                    }
                    return nil;
                })
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        sub.assertFailedWith(RuntimeException.class, "boom");
        sub.assertItems(1, 2, 3);
    }

    @Test
    @DisplayName("Forbid null initial value suppliers")
    void forbidNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> {
            Multi.createFrom().generator(null, (s, e) -> s);
        });
    }

    @Test
    @DisplayName("Forbid null generator functions")
    void forbidNullGenerator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Multi.createFrom().generator(() -> "", null);
        });
    }

    @Test
    @DisplayName("Propagate failure to create an initial state downstream")
    void handleThrowingInitialStateSupplier() {
        AssertSubscriber<Object> sub = Multi.createFrom().generator(
                () -> {
                    throw new RuntimeException("boom");
                },
                (s, e) -> s)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        sub.assertFailedWith(RuntimeException.class, "boom");
    }

    @Test
    @DisplayName("Emitting a null item is incorrect")
    void emittingNullItemIsBad() {
        AssertSubscriber<? super Integer> sub = Multi
                .createFrom()
                .generator(AtomicInteger::new, (counter, emitter) -> {
                    int n = counter.getAndIncrement();
                    if (n == 5) {
                        emitter.emit(null);
                    } else {
                        emitter.emit(n);
                    }
                    return counter;
                })
                .subscribe().withSubscriber(AssertSubscriber.create(20));

        sub.assertFailedWith(NullPointerException.class, "item");
        sub.assertItems(0, 1, 2, 3, 4);
    }

    @Test
    @DisplayName("Emitting a null failure is incorrect")
    void failingNullItemIsBad() {
        AssertSubscriber<? super Integer> sub = Multi
                .createFrom()
                .generator(AtomicInteger::new, (counter, emitter) -> {
                    int n = counter.getAndIncrement();
                    if (n == 5) {
                        emitter.fail(null);
                    } else {
                        emitter.emit(n);
                    }
                    return counter;
                })
                .subscribe().withSubscriber(AssertSubscriber.create(20));

        sub.assertFailedWith(NullPointerException.class, "The failure is null");
        sub.assertItems(0, 1, 2, 3, 4);
    }
}
