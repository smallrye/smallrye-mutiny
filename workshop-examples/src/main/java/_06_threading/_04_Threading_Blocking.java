///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.5.8
package _06_threading;

import java.util.stream.Collectors;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.BlockingIterable;

public class _04_Threading_Blocking {

    public static void main(String[] args) {
        System.out.println("⚡️ blocking");

        BlockingIterable<Integer> iterable = Multi.createFrom().range(0, 10)
                .subscribe().asIterable();

        var list = iterable.stream().collect(Collectors.toList());

        System.out.println(list);

        Integer someInt = Uni.createFrom().item(123)
                .await().indefinitely();

        System.out.println(someInt);
    }
}
