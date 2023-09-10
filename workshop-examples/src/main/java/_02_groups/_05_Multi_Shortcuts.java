///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.4.0
package _02_groups;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class _05_Multi_Shortcuts {

    public static void main(String[] args) {
        System.out.println("⚡️ Multi invoke / call shortcuts");

        Multi.createFrom().range(1, 10)
                .invoke(n -> System.out.println("n = " + n))
                .call(n -> Uni.createFrom()
                        .voidItem().invoke(() -> System.out.println("call(" + n + ")")))
                .subscribe().with(System.out::println);
    }
}
