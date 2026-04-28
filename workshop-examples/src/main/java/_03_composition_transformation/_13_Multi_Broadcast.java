///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:3.2.0
package _03_composition_transformation;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.mutiny.Multi;

public class _13_Multi_Broadcast {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("⚡️ Multi broadcast");

        var counter = new AtomicInteger();
        var executor = Executors.newCachedThreadPool();

        var multi = Multi.createBy()
                .repeating().supplier(counter::getAndIncrement)
                .atMost(10)
                .broadcast().toAllSubscribers();

        executor.submit(() -> multi
                .onItem().transform(n -> "🚀 " + n)
                .subscribe().with(System.out::println));

        executor.submit(() -> multi
                .onItem().transform(n -> "🧪 " + n)
                .subscribe().with(System.out::println));

        executor.submit(() -> multi
                .onItem().transform(n -> "💡 " + n)
                .subscribe().with(System.out::println));

        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
}
