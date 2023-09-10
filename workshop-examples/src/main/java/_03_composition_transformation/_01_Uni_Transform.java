///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.4.0
package _03_composition_transformation;

import io.smallrye.mutiny.Uni;

public class _01_Uni_Transform {

    public static void main(String[] args) {
        System.out.println("⚡️ Uni transform a value with a function");

        Uni.createFrom().item(123)
                .onItem().transform(n -> n * 100)
                .onItem().transform(Object::toString)
                .subscribe().with(System.out::println);
    }
}
