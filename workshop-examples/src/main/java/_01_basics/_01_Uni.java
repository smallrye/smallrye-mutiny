///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.9.0-RC1
package _01_basics;

import io.smallrye.mutiny.Uni;

public class _01_Uni {

    public static void main(String[] args) {
        System.out.println("⚡️ Hello world");

        Uni<String> uni = Uni.createFrom().item("Hello, world!");

        uni.subscribe().with(System.out::println);
    }
}
