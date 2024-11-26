///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.7.0
package _01_basics;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.smallrye.mutiny.Multi;

public class _13_Multi {

    public static void main(String[] args) {
        System.out.println("⚡️ Hello world");

        // -------------------------------------------------------------------------------------------------- //

        Multi.createFrom().items(1, 2, 3)
                .subscribe().with(
                        subscription -> {
                            System.out.println("Subscription: " + subscription);
                            subscription.request(10);
                        },
                        item -> System.out.println("Item: " + item),
                        failure -> System.out.println("Failure: " + failure.getMessage()),
                        () -> System.out.println("Completed"));

        // -------------------------------------------------------------------------------------------------- //

        System.out.println("----");

        Multi.createFrom().range(10, 15)
                .subscribe().with(System.out::println);

        var randomNumbers = Stream
                .generate(ThreadLocalRandom.current()::nextInt)
                .limit(5)
                .collect(Collectors.toList());

        // -------------------------------------------------------------------------------------------------- //

        System.out.println("----");

        Multi.createFrom().iterable(randomNumbers)
                .subscribe().with(System.out::println);
    }
}
