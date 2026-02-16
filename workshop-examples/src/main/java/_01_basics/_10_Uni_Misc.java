///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:3.1.1
package _01_basics;

import java.util.Optional;

import io.smallrye.mutiny.Uni;

public class _10_Uni_Misc {

    public static void main(String[] args) {
        System.out.println("⚡️ Misc");

        Uni.createFrom().nothing()
                .subscribe().with(System.out::println, failure -> System.out.println(failure.getMessage()));

        Uni.createFrom().voidItem()
                .subscribe().with(System.out::println, failure -> System.out.println(failure.getMessage()));

        Uni.createFrom().nullItem()
                .subscribe().with(System.out::println, failure -> System.out.println(failure.getMessage()));

        Uni.createFrom().optional(Optional.of("Hello"))
                .subscribe().with(System.out::println, failure -> System.out.println(failure.getMessage()));

        Uni.createFrom().converter(i -> Uni.createFrom().item("[" + i + "]"), 10)
                .subscribe().with(System.out::println, failure -> System.out.println(failure.getMessage()));

    }
}
