///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.5.6
package _02_groups;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import io.smallrye.mutiny.Uni;

public class _02_Uni_Call {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("⚡️ Uni call()");

        CountDownLatch latch = new CountDownLatch(1);

        Uni.createFrom().item(123)
                .onItem().call(n -> asyncLog(">>> ", n))
                .onTermination().call(() -> asyncLog("--- ", ""))
                .subscribe().with(x -> {
                    System.out.println(x);
                    latch.countDown();
                });

        latch.await();
    }

    static Uni<Void> asyncLog(String prefix, Object value) {
        var cs = CompletableFuture
                .runAsync(() -> System.out.println(Thread.currentThread() + " :: " + prefix + value));
        return Uni.createFrom().completionStage(cs);
    }
}
