///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.4.0
package _07_misc;

import io.smallrye.mutiny.Multi;

public class _02_Logging {

    public static void main(String[] args) {
        System.out.println("⚡️ Logging");

        Multi<String> multi = Multi.createFrom().range(1, 3)
                .log("source")
                .onItem().transform(i -> ">>> " + i)
                .log("transformed");

        System.out.println();
        System.out.println("🚀 First subscriber");
        multi.subscribe().with(System.out::println);

        System.out.println();
        System.out.println("🚀 Second subscriber");
        multi.subscribe().with(System.out::println);
    }
}
