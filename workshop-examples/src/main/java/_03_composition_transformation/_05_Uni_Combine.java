///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.4.0
package _03_composition_transformation;

import io.smallrye.mutiny.Uni;

public class _05_Uni_Combine {

    public static void main(String[] args) {
        System.out.println("⚡️ Uni combine");

        var first = Uni.createFrom().item(1);
        var second = Uni.createFrom().item(2);
        var third = Uni.createFrom().item(3);

        Uni.combine()
                .all().unis(first, second, third)
                .asTuple()
                .subscribe().with(System.out::println);

        Uni.combine()
                .all().unis(first, second, third)
                .combinedWith((a, b, c) -> a + b + c)
                .subscribe().with(System.out::println);

        Uni.combine()
                .any().of(first, second, third)
                .subscribe().with(System.out::println);
    }
}
