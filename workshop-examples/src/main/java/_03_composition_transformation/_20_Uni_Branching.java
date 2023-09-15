///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.4.0
package _03_composition_transformation;

import io.smallrye.mutiny.Uni;

import java.util.Random;

public class _20_Uni_Branching {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("⚡️ Uni and branching");

        Random random = new Random();
        Uni.createFrom().item(() -> random.nextInt(100))
                .onItem().transformToUni(n -> {
                    if (n % 2 == 0) {
                        return evenOperation(n);
                    } else {
                        return oddOperation(n);
                    }
                })
                .invoke(str -> {
                    if (str.startsWith("Odd")) {
                        System.out.println("(looks like we have a odd number)");
                    }
                })
                .subscribe().with(System.out::println);
    }

    static Uni<String> evenOperation(int n) {
        return Uni.createFrom().item("Even number: " + n)
                .onItem().invoke(() -> System.out.println("(even branch)"));
    }

    static Uni<String> oddOperation(int n) {
        return Uni.createFrom().item("Odd number: " + n)
                .onItem().invoke(() -> System.out.println("(odd branch)"));
    }
}
