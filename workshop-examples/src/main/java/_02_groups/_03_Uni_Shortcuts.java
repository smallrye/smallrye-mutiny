///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.4.0
package _02_groups;

import io.smallrye.mutiny.Uni;

public class _03_Uni_Shortcuts {

    public static void main(String[] args) {
        System.out.println("⚡️ Uni invoke / call shortcuts");

        Uni.createFrom().item(123)
                .invoke(n -> System.out.println("n = " + n))
                .call(n -> Uni.createFrom()
                        .voidItem()
                        .invoke(() -> System.out.println("call(" + n + ")")))
                .eventually(() -> System.out.println("eventually()"))
                .subscribe().with(System.out::println);

    }
}
