///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.4.0
package _03_composition_transformation;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class _18_Multi_To_Uni_And_Back {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("⚡️ Multi <-> Uni");

        Multi.createFrom().range(1, 10)
                .toUni()
                .subscribe().with(System.out::println);

        Uni.createFrom().item(123)
                .toMulti()
                .subscribe().with(System.out::println);
    }
}