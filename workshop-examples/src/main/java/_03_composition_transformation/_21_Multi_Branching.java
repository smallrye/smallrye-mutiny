///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.5.0-M3
package _03_composition_transformation;

import java.util.Random;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class _21_Multi_Branching {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("⚡️ Uni and branching");

        Random random = new Random();
        Multi.createBy().repeating().supplier(random::nextInt).atMost(20)
                .onItem().transformToUniAndMerge(n -> {
                    System.out.println("----");
                    if (n < 0) {
                        return drop();
                    } else if (n % 2 == 0) {
                        return evenOperation(n);
                    } else {
                        return oddOperation(n);
                    }
                })
                .subscribe().with(str -> System.out.println("=> " + str));
    }

    static Uni<String> drop() {
        return Uni.createFrom().<String> nullItem()
                .onItem().invoke(() -> System.out.println("(dropping negative value)"));
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
