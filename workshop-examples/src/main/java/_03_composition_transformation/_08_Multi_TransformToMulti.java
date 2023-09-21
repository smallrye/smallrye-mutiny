///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.5.0-M2
package _03_composition_transformation;

import static java.util.concurrent.CompletableFuture.delayedExecutor;
import static java.util.concurrent.CompletableFuture.runAsync;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import io.smallrye.mutiny.Multi;

public class _08_Multi_TransformToMulti {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("⚡️ Multi transformations to Multi");

        CountDownLatch latch = new CountDownLatch(1);

        Multi.createFrom().range(1, 100)
                .select().where(n -> n % 2 == 0)
                .select().last(5)
                .onItem().transformToMultiAndMerge(n -> query(n)) // try transformToMultiAndConcatenate
                .onItem().transform(n -> "[" + n + "]")
                .onCompletion().invoke(latch::countDown)
                .subscribe().with(System.out::println);

        latch.await();
    }

    static Multi<Integer> query(int n) {
        return Multi.createFrom().emitter(emitter -> {
            runAsync(
                    () -> {
                        emitter.emit(n);
                        emitter.emit(n * 10);
                        emitter.emit(n * 100);
                        emitter.complete();
                    },
                    delayedExecutor(ThreadLocalRandom.current().nextInt(500), TimeUnit.MILLISECONDS));
        });
    }
}
