package io.smallrye.mutiny.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.*;

import org.eclipse.microprofile.context.ThreadContext;
import org.junit.jupiter.api.*;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class MultiContextPropagationTest {

    private static ExecutorService executor;

    @BeforeEach
    public void initContext() {
        Infrastructure.reload();
        MyContext.init();
    }

    @AfterEach
    public void clearContext() {
        Infrastructure.reload();
        MyContext.clear();
    }

    @BeforeAll
    public static void init() {
        executor = Executors.newFixedThreadPool(4);
    }

    @AfterAll
    public static void shutdown() {
        executor.shutdown();
    }

    @Test
    public void testDeferredWithSingleItem() {
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();
        Multi<Integer> multi = Multi.createFrom()
                .item(() -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return 2;
                })
                .runSubscriptionOn(executor)
                .map(r -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return r;
                });

        Uni<Integer> latch = Multi.createFrom().<Integer> emitter(emitter -> new Thread(() -> {
            try {
                int result = multi.toUni().await().indefinitely();
                emitter.emit(result);
                emitter.complete();
            } catch (Throwable t) {
                emitter.fail(t);
            }
        }).start()).toUni();

        int result = latch.await().indefinitely();
        assertThat(result).isEqualTo(2);
    }

    @Test
    public void testMultiToUni() {
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();
        Multi<Integer> multi = Multi.createFrom()
                .item(() -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return 2;
                })
                .runSubscriptionOn(executor)
                .map(r -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return r;
                });

        Uni<Integer> latch = Uni.createFrom().emitter(emitter -> new Thread(() -> {
            try {
                int result = multi.toUni().await().indefinitely();
                emitter.complete(result);
            } catch (Throwable t) {
                emitter.fail(t);
            }
        }).start());

        int result = latch.await().indefinitely();
        assertThat(result).isEqualTo(2);
    }

    @Test
    public void testDeferredWithMultipleItems() {
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();
        Multi<Integer> multi = Multi.createFrom()
                .item(() -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return 2;
                })
                .runSubscriptionOn(executor)
                .map(r -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return r;
                });

        Uni<List<Integer>> latch = Multi.createFrom().<Integer> emitter(emitter -> new Thread(() -> {
            try {
                int result = multi.toUni().await().indefinitely();
                emitter.emit(result);
                emitter.emit(result);
                emitter.emit(result);
                emitter.complete();
            } catch (Throwable t) {
                emitter.fail(t);
            }
        }).start()).collect().asList();

        List<Integer> result = latch.await().indefinitely();
        assertThat(result).hasSize(3).allSatisfy(i -> assertThat(i).isEqualTo(2));
    }

    @Test
    public void testGenerator() {
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();
        Multi<Integer> multi = Multi.createFrom().<Integer> emitter(emitter -> {
            assertThat(ctx).isSameAs(MyContext.get());
            new Thread(() -> {
                try {
                    emitter.emit(2);
                    emitter.emit(3);
                    emitter.complete();
                } catch (Throwable t) {
                    emitter.fail(t);
                }
            }).start();
        }).map(r -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return r;
        });

        int result = multi.collect().first().await().indefinitely();
        assertThat(result).isEqualTo(2);
    }

    @RepeatedTest(100)
    public void testBroadcast() {
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();
        Multi<Integer> multi = Multi.createFrom().<Integer> emitter(emitter -> {
            assertThat(ctx).isSameAs(MyContext.get());
            new Thread(() -> {
                try {
                    emitter.emit(2);
                    emitter.emit(3);
                    emitter.complete();
                } catch (Throwable t) {
                    emitter.fail(t);
                }
            }).start();
        }).map(r -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return r;
        }).broadcast().toAtLeast(2);

        AssertSubscriber<Integer> sub1 = multi
                .map(i -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return i;
                })
                .subscribe().withSubscriber(AssertSubscriber.create(10));
        AssertSubscriber<Integer> sub2 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        sub1.awaitCompletion(Duration.of(5, ChronoUnit.SECONDS)).assertCompleted().assertItems(2, 3);
        sub2.awaitCompletion(Duration.of(5, ChronoUnit.SECONDS)).assertCompleted().assertItems(2, 3);
    }

    @Test
    public void testCompletionStage() throws InterruptedException, ExecutionException {
        CountDownLatch fire = new CountDownLatch(1);
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();
        Multi<Integer> multi = Multi.createFrom().emitter(emitter -> {
            assertThat(ctx).isSameAs(MyContext.get());
            new Thread(() -> {
                try {
                    assertThat(ctx).isNotNull();
                    fire.await();
                    emitter.emit(2);
                    emitter.complete();
                } catch (Throwable t) {
                    emitter.fail(t);
                }
            }).start();
        });

        CompletableFuture<Integer> cs = multi.toUni().subscribeAsCompletionStage();

        MyContext ctx2 = new MyContext();
        MyContext.set(ctx2);
        CompletableFuture<Integer> cs2 = cs.thenApply(r -> {
            assertThat(ctx2).isSameAs(MyContext.get());
            return r;
        });

        CompletableFuture<Integer> cf = new CompletableFuture<>();
        new Thread(() -> {
            try {
                assertNull(MyContext.get());
                int result = cs2.get();
                cf.complete(result);
            } catch (Throwable t) {
                cf.completeExceptionally(t);
            }
        }).start();

        fire.countDown();
        int result = cf.get();
        assertThat(result).isEqualTo(2);
    }

    @Test
    public void testOnSubscribe() {
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();

        AssertSubscriber<Long> subscriber = Multi.createFrom().ticks().every(Duration.ofMillis(1))
                .select().first(5)
                .onSubscription().invoke(() -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    MyContext.get().set("test");
                })
                .onRequest().invoke(l -> {
                    assertThat(l).isGreaterThan(0);
                    assertThat(ctx).isSameAs(MyContext.get());
                    assertThat(MyContext.get().getReqId()).isEqualTo("test");
                })
                .onItem().invoke(() -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    assertThat(MyContext.get().getReqId()).isEqualTo("test");
                })
                .subscribe().withSubscriber(AssertSubscriber.create(20));

        subscriber.awaitCompletion()
                .assertItems(0L, 1L, 2L, 3L, 4L);

    }

    @Test
    public void testSelectWhere() {
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();

        AssertSubscriber<Long> subscriber = Multi.createFrom().ticks().every(Duration.ofMillis(1))
                .select().first(5)
                .select().where(l -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return l % 2 == 0;
                })
                .subscribe().withSubscriber(AssertSubscriber.create(20));

        subscriber.awaitCompletion()
                .assertItems(0L, 2L, 4L);

    }

    @Test
    public void testScan() {
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();

        AssertSubscriber<Integer> subscriber = Multi.createFrom().ticks().every(Duration.ofMillis(1))
                .onItem().scan((acc, it) -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return acc + it;
                })
                .onItem().transform(Long::intValue)
                .select().first(10)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.awaitCompletion()
                .assertItems(0, 1, 3, 6, 10, 15, 21, 28, 36, 45);

    }

    @Test
    public void testContextOverride() {
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();
        SmallRyeThreadContext emptyContext = SmallRyeThreadContext.builder().cleared(ThreadContext.ALL_REMAINING)
                .propagated(ThreadContext.NONE).build();
        Multi<Integer> multi;
        // remove context propagation in this scope
        try (CleanAutoCloseable ac = SmallRyeThreadContext.withThreadContext(emptyContext)) {
            multi = Multi.createFrom()
                    .item(() -> {
                        assertThat(MyContext.get()).isNull();
                        return 2;
                    })
                    .runSubscriptionOn(executor)
                    .map(r -> {
                        assertThat(MyContext.get()).isNull();
                        return r;
                    });
        }

        Uni<Integer> latch = Multi.createFrom().<Integer> emitter(emitter -> new Thread(() -> {
            try {
                int result = multi.toUni().await().indefinitely();
                emitter.emit(result);
                emitter.complete();
            } catch (Throwable t) {
                emitter.fail(t);
            }
        }).start()).toUni();

        int result = latch.await().indefinitely();
        assertThat(result).isEqualTo(2);
    }
}
