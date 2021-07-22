package io.smallrye.mutiny.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.eclipse.microprofile.context.ThreadContext;
import org.junit.jupiter.api.*;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.UniEmitter;

public class UniContextPropagationTest {

    private static ExecutorService executor;

    @BeforeEach
    public void initContext() {
        Infrastructure.reloadCallbackDecorators();
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
    public void testDeferred() {
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();
        Uni<Integer> uni = Uni.createFrom()
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
                int result = uni.await().indefinitely();
                emitter.complete(result);
            } catch (Throwable t) {
                emitter.fail(t);
            }
        }).start());

        int result = latch.await().indefinitely();
        assertThat(result).isEqualTo(2);
    }

    @Test
    public void testGenerator() {
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();
        Uni<Integer> uni = Uni.createFrom().<Integer> emitter(emitter -> {
            assertThat(ctx).isSameAs(MyContext.get());
            new Thread(() -> {
                try {
                    emitter.complete(2);
                } catch (Throwable t) {
                    emitter.fail(t);
                }
            }).start();
        }).map(r -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return r;
        });

        int result = uni.await().indefinitely();
        assertThat(result).isEqualTo(2);
    }

    @Test
    public void testCompletionStage() throws InterruptedException, ExecutionException {
        CountDownLatch fire = new CountDownLatch(1);
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();
        Uni<Integer> uni = Uni.createFrom().emitter(emitter -> {
            assertThat(ctx).isSameAs(MyContext.get());
            new Thread(() -> {
                try {
                    assertThat(ctx).isNotNull();
                    fire.await();
                    emitter.complete(2);
                } catch (Throwable t) {
                    emitter.fail(t);
                }
            }).start();
        });

        CompletableFuture<Integer> cs = uni.subscribeAsCompletionStage();

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
    public void testZip3() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        MyContext ctx = MyContext.get();
        Executor executor = Executors.newFixedThreadPool(4);

        Uni<String> uni1 = Uni.createFrom().item(() -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return "a";
        }).runSubscriptionOn(executor);

        Uni<String> uni2 = Uni.createFrom().item(() -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return "b";
        }).runSubscriptionOn(executor);

        Uni<String> uni3 = Uni.createFrom().item(() -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return "c";
        }).runSubscriptionOn(executor);

        Uni.combine().all().unis(uni1, uni2, uni3)
                .combinedWith((item1, item2, item3) -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return item1 + item2 + item3;
                })
                .emitOn(executor)
                .subscribe().with(
                        s -> {
                            assertThat(ctx).isSameAs(MyContext.get());
                            assertThat(s).isEqualTo("abc");
                            latch.countDown();
                        });

        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void testZip2() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        MyContext ctx = MyContext.get();
        Executor executor = Executors.newFixedThreadPool(4);

        Uni<String> uni1 = Uni.createFrom().item(() -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return "a";
        }).runSubscriptionOn(executor);

        Uni<String> uni2 = Uni.createFrom().item(() -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return "b";
        }).runSubscriptionOn(executor);

        Uni.combine().all().unis(uni1, uni2)
                .asTuple()
                .emitOn(executor)
                .onItem().transformToUni(tuple -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return Uni.createFrom().item(tuple.getItem1() + tuple.getItem2()).onItem().delayIt()
                            .by(Duration.ofMillis(1));
                })
                .onItem().transform(s -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return s.toUpperCase();
                })
                .subscribe().with(
                        s -> {
                            assertThat(ctx).isSameAs(MyContext.get());
                            assertThat(s).isEqualTo("AB");
                            latch.countDown();
                        });

        // Same but with biFunction
        Uni.combine().all().unis(uni1, uni2)
                .combinedWith((a, b) -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return a + b;
                })
                .emitOn(executor)
                .onItem().transform(s -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return s.toUpperCase();
                })
                .subscribe().with(
                        s -> {
                            assertThat(ctx).isSameAs(MyContext.get());
                            assertThat(s).isEqualTo("AB");
                            latch.countDown();
                        });

        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void testZip() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(6);
        MyContext ctx = MyContext.get();
        Executor executor = Executors.newFixedThreadPool(4);

        Uni<String> uni1 = Uni.createFrom().item(() -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return "a";
        }).runSubscriptionOn(executor);

        Uni<String> uni2 = Uni.createFrom().item(() -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return "b";
        }).runSubscriptionOn(executor);

        Uni<String> uni3 = Uni.createFrom().item(() -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return "c";
        }).runSubscriptionOn(executor);

        Uni<String> uni4 = Uni.createFrom().item(() -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return "d";
        }).runSubscriptionOn(executor);

        Uni<String> uni5 = Uni.createFrom().item(() -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return "e";
        }).runSubscriptionOn(executor);

        Uni<String> uni6 = Uni.createFrom().item(() -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return "f";
        }).runSubscriptionOn(executor);

        Uni<String> uni7 = Uni.createFrom().item(() -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return "g";
        }).runSubscriptionOn(executor);

        Uni<String> uni8 = Uni.createFrom().item(() -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return "h";
        }).runSubscriptionOn(executor);

        Uni<String> uni9 = Uni.createFrom().item(() -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return "i";
        }).runSubscriptionOn(executor);

        Uni.combine().all().unis(uni1, uni2, uni3, uni4)
                .combinedWith((item1, item2, item3, item4) -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return item1 + item2 + item3 + item4;
                })
                .emitOn(executor)
                .subscribe().with(
                        s -> {
                            assertThat(ctx).isSameAs(MyContext.get());
                            assertThat(s).isEqualTo("abcd");
                            latch.countDown();
                        });

        Uni.combine().all().unis(uni1, uni2, uni3, uni4, uni5)
                .combinedWith((item1, item2, item3, item4, item5) -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return item1 + item2 + item3 + item4 + item5;
                })
                .emitOn(executor)
                .subscribe().with(
                        s -> {
                            assertThat(ctx).isSameAs(MyContext.get());
                            assertThat(s).isEqualTo("abcde");
                            latch.countDown();
                        });

        Uni.combine().all().unis(uni1, uni2, uni3, uni4, uni5, uni6)
                .combinedWith((item1, item2, item3, item4, item5, item6) -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return item1 + item2 + item3 + item4 + item5 + item6;
                })
                .emitOn(executor)
                .subscribe().with(
                        s -> {
                            assertThat(ctx).isSameAs(MyContext.get());
                            assertThat(s).isEqualTo("abcdef");
                            latch.countDown();
                        });

        Uni.combine().all().unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7)
                .combinedWith((item1, item2, item3, item4, item5, item6, item7) -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return item1 + item2 + item3 + item4 + item5 + item6 + item7;
                })
                .emitOn(executor)
                .subscribe().with(
                        s -> {
                            assertThat(ctx).isSameAs(MyContext.get());
                            assertThat(s).isEqualTo("abcdefg");
                            latch.countDown();
                        });

        Uni.combine().all().unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7, uni8)
                .combinedWith((item1, item2, item3, item4, item5, item6, item7, item8) -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return item1 + item2 + item3 + item4 + item5 + item6 + item7 + item8;
                })
                .emitOn(executor)
                .subscribe().with(
                        s -> {
                            assertThat(ctx).isSameAs(MyContext.get());
                            assertThat(s).isEqualTo("abcdefgh");
                            latch.countDown();
                        });

        Uni.combine().all().unis(uni1, uni2, uni3, uni4, uni5, uni6, uni7, uni8, uni9)
                .combinedWith((item1, item2, item3, item4, item5, item6, item7, item8, item9) -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return item1 + item2 + item3 + item4 + item5 + item6 + item7 + item8 + item9;
                })
                .emitOn(executor)
                .subscribe().with(
                        s -> {
                            assertThat(ctx).isSameAs(MyContext.get());
                            assertThat(s).isEqualTo("abcdefghi");
                            latch.countDown();
                        });

        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void testOnTermination() throws InterruptedException {
        MyContext ctx = MyContext.get();
        AtomicInteger count = new AtomicInteger();
        AtomicReference<UniEmitter<? super Integer>> reference = new AtomicReference<>();
        Uni<Integer> uni = Uni.createFrom().emitter((Consumer<UniEmitter<? super Integer>>) reference::set)
                .onTermination()
                .invoke((value, failure, cancellation) -> {
                    count.incrementAndGet();
                    assertThat(ctx).isSameAs(MyContext.get());
                });

        // Test completion
        CountDownLatch latch1 = new CountDownLatch(1);
        uni.subscribe().with(x -> {
            latch1.countDown();
        });

        new Thread(() -> {
            reference.get().complete(23);
        }).start();

        latch1.await(10, TimeUnit.MILLISECONDS);
        assertThat(count).hasValue(1);

        // Test failure
        CountDownLatch latch2 = new CountDownLatch(1);
        uni.subscribe().with(x -> {

        }, fail -> {
            latch2.countDown();
        });

        new Thread(() -> {
            reference.get().fail(new Exception("boom"));
        }).start();

        latch2.await(10, TimeUnit.MILLISECONDS);
        assertThat(count).hasValue(2);

        // Test cancellation
        Cancellable cancellable = uni.subscribe().with(x -> {

        }, fail -> {

        });

        new Thread(cancellable::cancel).start();
        await().until(() -> count.get() == 3);
    }

    @Test
    public void testCache() {
        AtomicInteger sub = new AtomicInteger();
        AtomicInteger count = new AtomicInteger();

        MyContext ctx = MyContext.get();
        Uni<Integer> uni = Uni.createFrom().item(() -> {
            assertThat(ctx).isSameAs(MyContext.get());
            return count.incrementAndGet();
        })
                .emitOn(ForkJoinPool.commonPool())
                .runSubscriptionOn(ForkJoinPool.commonPool())
                .memoize().until(() -> {
                    assertThat(ctx).isSameAs(MyContext.get());
                    return sub.incrementAndGet() > 2;
                });

        assertThat(uni.await().atMost(Duration.ofMillis(100))).isEqualTo(1);
        assertThat(uni.await().atMost(Duration.ofMillis(100))).isEqualTo(1);
        assertThat(uni.await().atMost(Duration.ofMillis(100))).isEqualTo(2);
        assertThat(uni.await().atMost(Duration.ofMillis(100))).isEqualTo(3);
        assertThat(uni.await().atMost(Duration.ofMillis(100))).isEqualTo(4);
    }

    @Test
    public void testContextOverride() {
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();
        SmallRyeThreadContext emptyContext = SmallRyeThreadContext.builder().cleared(ThreadContext.ALL_REMAINING)
                .propagated(ThreadContext.NONE).build();
        Uni<Integer> uni;
        // remove context propagation in this scope
        try (CleanAutoCloseable ac = SmallRyeThreadContext.withThreadContext(emptyContext)) {
            uni = Uni.createFrom()
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

        Uni<Integer> latch = Uni.createFrom().emitter(emitter -> new Thread(() -> {
            try {
                int result = uni.await().indefinitely();
                emitter.complete(result);
            } catch (Throwable t) {
                emitter.fail(t);
            }
        }).start());

        int result = latch.await().indefinitely();
        assertThat(result).isEqualTo(2);
    }
}
