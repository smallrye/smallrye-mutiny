///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.4.0
package _03_composition_transformation;

import io.smallrye.mutiny.Uni;

public class _02_Uni_TransformToUni {

    public static void main(String[] args) {
        System.out.println("⚡️ Uni transform a value with a Uni");

        Uni.createFrom().item(123)
                .onItem().transformToUni(n -> increase(n))
                .onItem().transformToUni((n, emitter) -> emitter.complete("[" + n + "]"))
                .subscribe().with(System.out::println);
    }

    static Uni<Integer> increase(int n) {
        return Uni.createFrom().item(n * 100);
    }
}
