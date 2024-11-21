///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.7.0-RC4
package _01_basics;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class _17_Multi_By_Repeating {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("⚡️ Multi by repeating");

        Multi.createBy()
                .repeating()
                .supplier(Service::fetchValue)
                .until(n -> n > 1_000_000L)
                .subscribe().with(System.out::println);

        System.out.println("\n----\n");

        CountDownLatch latch = new CountDownLatch(1);

        Multi.createBy()
                .repeating()
                .uni(Service::asyncFetchValue)
                .atMost(10)
                .subscribe().with(System.out::println, Throwable::printStackTrace, latch::countDown);

        latch.await();

        System.out.println("\n----\n");

        Multi.createBy()
                .repeating()
                .completionStage(Service::queryDb)
                .whilst(n -> n < 1_000_000L)
                .subscribe().with(System.out::println);
    }

    static class Service {

        static long fetchValue() {
            return ThreadLocalRandom.current().nextLong(1_001_000L);
        }

        static Uni<Long> asyncFetchValue() {
            return Uni.createFrom().completionStage(Service::queryDb);
        }

        static CompletionStage<Long> queryDb() {
            return CompletableFuture.supplyAsync(Service::fetchValue);
        }
    }
}
