///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.4.0
package _03_composition_transformation;

import io.smallrye.mutiny.Multi;

public class _06_Multi_Transform {

    public static void main(String[] args) {
        System.out.println("⚡️ Multi transformations");

        Multi.createFrom().range(1, 100)
                .select().where(n -> n % 2 == 0)
                .select().last(5)
                .onItem().transform(n -> "[" + n + "]")
                .subscribe().with(System.out::println);
    }
}
