///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.7.0-RC2
package _01_basics;

import static java.util.concurrent.CompletableFuture.delayedExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.smallrye.mutiny.Uni;

public class _09_Uni_From_CompletionStage {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("⚡️ Uni from CompletionStage");

        var cs = CompletableFuture
                .supplyAsync(() -> "Hello!", delayedExecutor(1, TimeUnit.SECONDS))
                .thenApply(String::toUpperCase);

        Uni.createFrom().completionStage(cs)
                .subscribe().with(System.out::println, failure -> System.out.println(failure.getMessage()));

        Thread.sleep(2000);
    }
}
