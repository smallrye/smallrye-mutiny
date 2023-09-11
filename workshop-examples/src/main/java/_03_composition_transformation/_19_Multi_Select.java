///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.4.0
package _03_composition_transformation;

import io.smallrye.mutiny.Multi;

public class _19_Multi_Select {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("⚡️ Multi select");

        Multi.createFrom().range(1, 100)
                .skip().first(10)
                .select().where(n -> n % 2 == 0)
                .select().last(10)
                .subscribe().with(System.out::println);
    }
}
