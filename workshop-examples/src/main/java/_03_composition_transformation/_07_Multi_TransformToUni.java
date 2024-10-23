///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.7.0-RC1
package _03_composition_transformation;

import static java.util.concurrent.CompletableFuture.delayedExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class _07_Multi_TransformToUni {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("⚡️ Multi transformations with Uni");

        CountDownLatch latch = new CountDownLatch(1);

        Multi.createFrom().range(1, 100)
                .select().where(n -> n % 2 == 0)
                .select().last(5)
                .onItem().transformToUniAndMerge(n -> increase(n)) // try transformToUniAndConcatenate
                .onItem().transform(n -> "[" + n + "]")
                .onCompletion().invoke(latch::countDown)
                .subscribe().with(System.out::println);

        latch.await();
    }

    static Uni<Integer> increase(int n) {
        var cs = CompletableFuture.supplyAsync(
                () -> n * 100,
                delayedExecutor(ThreadLocalRandom.current().nextInt(500), TimeUnit.MILLISECONDS));
        return Uni.createFrom().completionStage(cs);
    }
}
