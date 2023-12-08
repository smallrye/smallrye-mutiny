///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.5.2
package _03_composition_transformation;

import io.smallrye.mutiny.Uni;

public class _03_Uni_Shortcuts {

    public static void main(String[] args) {
        System.out.println("⚡️ Uni transform a value with shortcuts");

        Uni.createFrom().item(123)
                .replaceWith(Uni.createFrom().item(456))
                .chain(n -> increase(n)) // or flatMap
                .map(n -> "[" + n + "]")
                .subscribe().with(System.out::println);
    }

    static Uni<Integer> increase(int n) {
        return Uni.createFrom().item(n * 100);
    }
}
