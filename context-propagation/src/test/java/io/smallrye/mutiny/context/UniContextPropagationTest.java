package io.smallrye.mutiny.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.*;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class UniContextPropagationTest {

    private static ExecutorService executor;

    @BeforeEach
    public void initContext() {
        Infrastructure.clearInterceptors();
        Infrastructure.reloadUniInterceptors();
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
    public void testDeferred() {
        MyContext ctx = MyContext.get();
        assertThat(ctx).isNotNull();
        Uni<Integer> uni = Uni.createFrom()
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
            assertThat(ctx).isEqualTo(MyContext.get());
            new Thread(() -> {
                try {
                    emitter.complete(2);
                } catch (Throwable t) {
                    emitter.fail(t);
                }
            }).start();
        }).map(r -> {
            assertThat(ctx).isEqualTo(MyContext.get());
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
            assertThat(ctx).isEqualTo(MyContext.get());
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
