package io.smallrye.mutiny.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.*;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class MultiContextPropagationTest {

    private static ExecutorService executor;

    @BeforeEach
    public void initContext() {
        Infrastructure.clearInterceptors();
        Infrastructure.reloadUniInterceptors();
        Infrastructure.reloadMultiInterceptors();
        MyContext.init();
    }

    @AfterEach
    public void clearContext() {
        Infrastructure.clearInterceptors();
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
                    assertThat(ctx).isEqualTo(MyContext.get());
                    return 2;
                })
                .runSubscriptionOn(executor)
                .map(r -> {
                    assertThat(ctx).isEqualTo(MyContext.get());
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
                    assertThat(ctx).isEqualTo(MyContext.get());
                    return 2;
                })
                .runSubscriptionOn(executor)
                .map(r -> {
                    assertThat(ctx).isEqualTo(MyContext.get());
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
                    assertThat(ctx).isEqualTo(MyContext.get());
                    return 2;
                })
                .runSubscriptionOn(executor)
                .map(r -> {
                    assertThat(ctx).isEqualTo(MyContext.get());
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
        }).start()).collectItems().asList();

        List<Integer> result = latch.await().indefinitely();
        assertThat(result).hasSize(3).allSatisfy(i -> assertThat(i).isEqualTo(2));
    }

    @Test
    public void testGenerator() {
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();
        Multi<Integer> multi = Multi.createFrom().<Integer> emitter(emitter -> {
            assertThat(ctx).isEqualTo(MyContext.get());
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
            assertThat(ctx).isEqualTo(MyContext.get());
            return r;
        });

        int result = multi.collectItems().first().await().indefinitely();
        assertThat(result).isEqualTo(2);
    }

    @RepeatedTest(100)
    public void testBroadcast() {
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();
        Multi<Integer> multi = Multi.createFrom().<Integer> emitter(emitter -> {
            assertThat(ctx).isEqualTo(MyContext.get());
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
            assertThat(ctx).isEqualTo(MyContext.get());
            return r;
        }).broadcast().toAtLeast(2);

        AssertSubscriber<Integer> sub1 = multi
                .map(i -> {
                    assertThat(ctx).isEqualTo(MyContext.get());
                    return i;
                })
                .subscribe().withSubscriber(AssertSubscriber.create(10));
        AssertSubscriber<Integer> sub2 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        sub1.await(Duration.of(5, ChronoUnit.SECONDS)).assertCompleted().assertItems(2, 3);
        sub2.await(Duration.of(5, ChronoUnit.SECONDS)).assertCompleted().assertItems(2, 3);
    }

    @Test
    public void testCompletionStage() throws InterruptedException, ExecutionException {
        CountDownLatch fire = new CountDownLatch(1);
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();
        Multi<Integer> multi = Multi.createFrom().emitter(emitter -> {
            assertThat(ctx).isEqualTo(MyContext.get());
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
            assertThat(ctx2).isEqualTo(MyContext.get());
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
}
