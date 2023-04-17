package io.smallrye.mutiny.groups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.operators.MultiOnFailureRetryTest;
import io.smallrye.mutiny.unchecked.Unchecked;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class UniOnFailureRetryTest {
    @Test
    public void testFailureWithPredicateException() {
        AtomicLong counter = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Uni.createFrom().failure(new Throwable("Cause failure"))
                .onFailure(new ThrowablePredicate()).retry().atMost(2)
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);

        await().until(() -> counter.intValue() == 0);
        assertThat(failure.get()).isNotNull();
    }

    @Test
    public void testFailureWithPredicateFailure() {
        AtomicLong counter = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Uni.createFrom().failure(new Throwable("Cause failure"))
                .onFailure((t) -> false).retry().atMost(2)
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);

        await().until(() -> counter.intValue() == 0);
        assertThat(failure.get()).isNotNull();
    }

    @Test
    public void testRetryWhenWithNoFailureInTriggerStream() {
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        AtomicInteger count = new AtomicInteger();
        String value = Uni.createFrom().<String> emitter(e -> {
            int attempt = count.getAndIncrement();
            if (attempt == 0) {
                e.fail(new Exception("boom"));
            } else if (attempt == 1) {
                e.fail(new IOException("another-boom"));
            } else {
                e.complete("done");
            }
        })
                .onFailure().retry().when(stream -> stream.onItem().invoke(failures::add)
                        .onItem()
                        .transformToUni(f -> Uni.createFrom().item("tick").onItem().delayIt().by(Duration.ofMillis(10)))
                        .concatenate())
                .await().atMost(Duration.ofSeconds(5));

        assertThat(value).isEqualTo("done");
        assertThat(failures).hasSize(2)
                .anySatisfy(t -> assertThat(t).hasMessage("boom"))
                .anySatisfy(t -> assertThat(t).isInstanceOf(IOException.class).hasMessage("another-boom"));
    }

    @Test
    public void testRetryWhenWithFailureInTriggerStream() {
        assertThrows(IllegalStateException.class, () -> {
            AtomicInteger count = new AtomicInteger();
            Uni.createFrom().<String> emitter(e -> {
                int attempt = count.getAndIncrement();
                if (attempt == 0) {
                    e.fail(new Exception("boom"));
                } else if (attempt == 1) {
                    e.fail(new IOException("another-boom"));
                } else {
                    e.complete("done");
                }
            })
                    .onFailure().retry().when(stream -> stream
                            .onItem().transformToUni(f -> Uni.createFrom().failure(new IllegalStateException("damned!")))
                            .concatenate())
                    .await().atMost(Duration.ofSeconds(5));
        });
    }

    @Test
    public void testRetryWhenWithCompletionInTriggerStream() {
        AtomicInteger count = new AtomicInteger();
        String value = Uni.createFrom().<String> emitter(e -> {
            int attempt = count.getAndIncrement();
            if (attempt == 0) {
                e.fail(new Exception("boom"));
            } else if (attempt == 1) {
                e.fail(new IOException("another-boom"));
            } else {
                e.complete("done");
            }
        })
                .onFailure().retry().when(stream -> stream.select().first())
                .await().atMost(Duration.ofSeconds(5));
        assertThat(value).isNull();
    }

    @Test
    public void testRetryWithBackOff() {
        AtomicInteger count = new AtomicInteger();
        String value = Uni.createFrom().<String> emitter(e -> {
            int attempt = count.getAndIncrement();
            if (attempt == 0) {
                e.fail(new Exception("boom"));
            } else if (attempt == 1) {
                e.fail(new IOException("another-boom"));
            } else {
                e.complete("done");
            }
        })
                .onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofSeconds(1)).withJitter(1.0)
                .atMost(20)
                .await().atMost(Duration.ofSeconds(5));

        assertThat(value).isEqualTo("done");
    }

    @Test
    public void testExpireInRetryWithBackOff() {
        AtomicInteger count = new AtomicInteger();
        String value = Uni.createFrom().<String> emitter(e -> {
            int attempt = count.getAndIncrement();
            if (attempt == 0) {
                e.fail(new Exception("boom"));
            } else if (attempt == 1) {
                e.fail(new IOException("another-boom"));
            } else {
                e.complete("done");
            }
        })
                .onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofSeconds(1)).withJitter(1.0)
                .expireIn(10_000L)
                .await().atMost(Duration.ofSeconds(5));

        assertThat(value).isEqualTo("done");
    }

    @Test
    public void testExpireAtRetryWithBackOff() {
        AtomicInteger count = new AtomicInteger();
        String value = Uni.createFrom().<String> emitter(e -> {
            int attempt = count.getAndIncrement();
            if (attempt == 0) {
                e.fail(new Exception("boom"));
            } else if (attempt == 1) {
                e.fail(new IOException("another-boom"));
            } else {
                e.complete("done");
            }
        })
                .onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofSeconds(1)).withJitter(1.0)
                .expireAt(System.currentTimeMillis() + 10_000L)
                .await().atMost(Duration.ofSeconds(5));

        assertThat(value).isEqualTo("done");
    }

    @Test
    public void testRetryWithBackOffReachingMaxAttempt() {
        assertThatThrownBy(() -> {
            AtomicInteger count = new AtomicInteger();
            Uni.createFrom().<String> emitter(e -> e.fail(new Exception("boom " + count.getAndIncrement())))
                    .onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofSeconds(20)).withJitter(1.0)
                    .atMost(4)
                    .await().atMost(Duration.ofSeconds(5));
        }).getCause() // Expected exception is wrapped in a java.util.concurrent.CompletionException
                .hasMessageContaining("boom")
                .hasSuppressedException(new IllegalStateException("Retries exhausted: 4/4"));
    }

    @Test
    public void testRetryWithBackOffReachingExpiresIn() {
        assertThrows(IllegalStateException.class, () -> {
            AtomicInteger count = new AtomicInteger();
            Uni.createFrom().<String> emitter(e -> e.fail(new Exception("boom " + count.getAndIncrement())))
                    .onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofSeconds(20)).withJitter(1.0)
                    .expireIn(90L)
                    .await().atMost(Duration.ofSeconds(5));
        });
    }

    @Test
    public void testRetryWithBackOffReachingExpiresAt() {
        assertThrows(IllegalStateException.class, () -> {
            AtomicInteger count = new AtomicInteger();
            Uni.createFrom().<String> emitter(e -> e.fail(new Exception("boom " + count.getAndIncrement())))
                    .onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofSeconds(20)).withJitter(1.0)
                    .expireAt(System.currentTimeMillis() + 90L)
                    .await().atMost(Duration.ofSeconds(5));
        });
    }

    @Test
    public void testRetryWithBackOffAndPredicateExpiresAt() {
        AtomicInteger numberOfRetries = new AtomicInteger();

        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().<String> emitter(e -> {
            int attempt = numberOfRetries.getAndIncrement();
            if (attempt == 0) {
                e.fail(new IOException("will-retry"));
            } else {
                e.fail(new IllegalArgumentException("boom"));
            }

        }).onFailure(IOException.class).retry().withBackOff(Duration.ofMillis(10)).expireIn(10_000L)
                .await().atMost(Duration.ofSeconds(5)));

        assertThat(numberOfRetries.get()).isEqualTo(2);
    }

    @Test
    public void testRetryWithBackOffAndPredicateAtMost() {
        AtomicInteger numberOfRetries = new AtomicInteger();

        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().<String> emitter(e -> {
            int attempt = numberOfRetries.getAndIncrement();
            if (attempt == 0) {
                e.fail(new IOException("will-retry"));
            } else {
                e.fail(new IllegalArgumentException("boom"));
            }

        }).onFailure(IOException.class).retry().withBackOff(Duration.ofMillis(10)).atMost(2)
                .await().atMost(Duration.ofSeconds(5)));

        assertThat(numberOfRetries.get()).isEqualTo(2);
    }

    @Test
    public void testThatYouCannotUseWhenIfBackoffIsConfigured() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item("hello")
                .onFailure().retry().withBackOff(Duration.ofSeconds(1)).when(t -> Multi.createFrom().item(t)));
    }

    @Test
    public void testExpireAtThatYouCannotUseWhenIfBackoffIsNotConfigured() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item("hello")
                .onFailure().retry().expireAt(1L));
    }

    @Test
    public void testExpireInThatYouCannotUseWhenIfBackoffIsNotConfigured() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item("hello")
                .onFailure().retry().expireIn(1L));
    }

    static class ThrowablePredicate implements Predicate<Throwable> {
        @Override
        public boolean test(Throwable throwable) {
            throw new RuntimeException();
        }
    }

    @Test
    public void checkThatItDoesOnlyRetryOnMatchingExceptionWithRetryAtMost() {
        AtomicInteger count = new AtomicInteger();

        Uni.createFrom().item(1)
                .onItem().invoke(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyRuntimeException("boom");
                    }
                })
                .onFailure(MultiOnFailureRetryTest.MyRuntimeException.class).retry().atMost(2)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertItem(1);

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().contains("boom")).retry().atMost(2)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(1);

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(ArithmeticException.class).retry().atMost(2)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().equalsIgnoreCase("wrong")).retry().atMost(2)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(t -> {
                    throw new RuntimeException("expected");
                }).retry().atMost(2)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(CompositeException.class, "expected");
    }

    @Test
    public void checkThatItDoesOnlyRetryOnMatchingExceptionWithRetryAtMostWithBackoff() {
        AtomicInteger count = new AtomicInteger();

        Uni.createFrom().item(1)
                .onItem().invoke(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyRuntimeException("boom");
                    }
                })
                .onFailure(MultiOnFailureRetryTest.MyRuntimeException.class).retry()
                .withBackOff(Duration.ofMillis(2)).atMost(2)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertItem(1);

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().contains("boom")).retry()
                .withBackOff(Duration.ofMillis(2)).atMost(2)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(1);

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(ArithmeticException.class).retry()
                .withBackOff(Duration.ofMillis(2)).atMost(2)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().equalsIgnoreCase("wrong")).retry()
                .withBackOff(Duration.ofMillis(2)).atMost(2)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(t -> {
                    throw new RuntimeException("expected");
                }).retry()
                .withBackOff(Duration.ofMillis(2)).atMost(2)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(CompositeException.class, "expected");
    }

    @Test
    public void checkThatItDoesOnlyRetryOnMatchingExceptionWithRetryAtMostWithExpireAt() {
        AtomicInteger count = new AtomicInteger();

        Uni.createFrom().item(1)
                .onItem().invoke(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyRuntimeException("boom");
                    }
                })
                .onFailure(MultiOnFailureRetryTest.MyRuntimeException.class).retry()
                .withBackOff(Duration.ofMillis(2)).expireAt(System.currentTimeMillis() + 10000)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertItem(1);

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().contains("boom")).retry()
                .withBackOff(Duration.ofMillis(2)).expireAt(System.currentTimeMillis() + 10000)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(1);

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(ArithmeticException.class).retry()
                .withBackOff(Duration.ofMillis(2)).expireAt(System.currentTimeMillis() + 10000)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().equalsIgnoreCase("wrong")).retry()
                .withBackOff(Duration.ofMillis(2)).expireAt(System.currentTimeMillis() + 10000)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(t -> {
                    throw new RuntimeException("expected");
                }).retry()
                .withBackOff(Duration.ofMillis(2)).expireAt(System.currentTimeMillis() + 10000)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(CompositeException.class, "expected");
    }

    @Test
    public void checkThatItDoesOnlyRetryOnMatchingExceptionWithRetryWhen() {
        AtomicInteger count = new AtomicInteger();

        Uni.createFrom().item(1)
                .onItem().invoke(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyRuntimeException("boom");
                    }
                })
                .onFailure(MultiOnFailureRetryTest.MyRuntimeException.class).retry()
                .when(t -> Multi.createFrom().items(1, 1, 1, 1))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertItem(1);

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().contains("boom")).retry().when(t -> Multi.createFrom().items(1, 1, 1, 1))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(1);

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(ArithmeticException.class).retry().when(t -> Multi.createFrom().items(1, 1, 1, 1))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().equalsIgnoreCase("wrong")).retry()
                .when(t -> Multi.createFrom().items(1, 1, 1, 1))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Uni.createFrom().item(1)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() == 0) {
                        throw new MultiOnFailureRetryTest.MyException("boom");
                    }
                }))
                .onFailure(t -> {
                    throw new RuntimeException("expected");
                }).retry().when(t -> Multi.createFrom().items(1, 1, 1, 1))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(CompositeException.class, "expected");
    }

    @Test
    public void rejectNullExecutors() {
        assertThatThrownBy(() -> Uni.createFrom().item(123)
                .onFailure().retry().withExecutor(null).withBackOff(Duration.ofMillis(100)).atMost(5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("`executor` must not be `null`");
    }
}
