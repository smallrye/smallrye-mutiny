package io.smallrye.reactive.infrastructure;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.context.MyContext;
import io.smallrye.reactive.infrastructure.Infrastructure;

public class ContextPropagationTest {

    @Before
    public void initContext() {
        Infrastructure.clearUniInterceptors();
        Infrastructure.reloadUniInterceptors();
        MyContext.init();
    }

    @After
    public void clearContext() {
        Infrastructure.clearUniInterceptors();
        MyContext.clear();
    }

    @Test
    public void testDeferred() {
        MyContext ctx = MyContext.get();
        Assert.assertNotNull(ctx);
        log("creating uni");
        Uni<Integer> uni = Uni.createFrom().deferredItem(() -> {
            log("creating deferred");
            Assert.assertEquals(ctx, MyContext.get());
            return 2;
        }).map(r -> {
            Assert.assertEquals(ctx, MyContext.get());
            log("mapping");
            return r;
        });

        log("creating latch");
        Uni<Integer> latch = Uni.createFrom().emitter(emitter -> {
            log("starting thread");
            new Thread() {

                public void run() {
                    try {
                        log("in thread waiting");
                        int result = uni.await().indefinitely();
                        log("in thread completing");
                        emitter.complete(result);
                    } catch (Throwable t) {
                        emitter.fail(t);
                    }
                }
            }.start();
        });

        log("await");
        int result = latch.await().indefinitely();
        log("await done");
        Assert.assertEquals(2, result);
    }

    @Test
    public void testGenerator() {
        MyContext ctx = MyContext.get();
        Assert.assertNotNull(ctx);
        log("creating uni");
        Uni<Integer> uni = Uni.createFrom().<Integer> emitter(emitter -> {
            log("creating emitter");
            Assert.assertEquals(ctx, MyContext.get());
            new Thread() {

                public void run() {
                    try {
                        log("in thread completing");
                        emitter.complete(2);
                    } catch (Throwable t) {
                        emitter.fail(t);
                    }
                }
            }.start();
        }).map(r -> {
            Assert.assertEquals(ctx, MyContext.get());
            log("mapping");
            return r;
        });

        log("await");
        int result = uni.await().indefinitely();
        log("await done");
        Assert.assertEquals(2, result);
    }

    @Test
    public void testCompletionStage() throws InterruptedException, ExecutionException {
        CountDownLatch fire = new CountDownLatch(1);
        MyContext ctx = MyContext.get();
        Assert.assertNotNull(ctx);
        log("creating uni");
        Uni<Integer> uni = Uni.createFrom().<Integer> emitter(emitter -> {
            log("creating emitter");
            Assert.assertEquals(ctx, MyContext.get());
            new Thread() {

                public void run() {
                    try {
                        log("in thread completing (wait for fire)");
                        Assert.assertNull(MyContext.get());
                        fire.await();
                        log("in thread completing");
                        emitter.complete(2);
                    } catch (Throwable t) {
                        emitter.fail(t);
                    }
                }
            }.start();
        });

        CompletableFuture<Integer> cs = uni.subscribeAsCompletionStage();
        
        MyContext ctx2 = new MyContext();
        MyContext.set(ctx2);
        log("changing context");
        CompletableFuture<Integer> cs2 = cs.thenApply(r -> {
            Assert.assertEquals(ctx2, MyContext.get());
            log("mapping");
            return r;
        });

        log("creating latch");
        CompletableFuture<Integer> cf = new CompletableFuture<>();
        log("starting thread");
        new Thread() {
            public void run() {
                try {
                    log("in thread waiting");
                    Assert.assertNull(MyContext.get());
                    int result = cs2.get();
                    log("in thread completing");
                    cf.complete(result);
                } catch (Throwable t) {
                    cf.completeExceptionally(t);
                }
            }
        }.start();

        log("fire");
        fire.countDown();
        log("await");
        int result = cf.get();
        log("await done");
        Assert.assertEquals(2, result);
    }

    private void log(String string) {
        System.err.println(Thread.currentThread() + ": " + string);
    }
}
