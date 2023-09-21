///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.5.0-M3
package _03_composition_transformation;

import java.time.Duration;

import io.smallrye.mutiny.Multi;

public class _16_Multi_Temporal_Buckets {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("⚡️ Multi temporal buckets");

        Multi.createFrom()
                .ticks().every(Duration.ofMillis(200))
                .group().intoLists().every(Duration.ofSeconds(2))
                .subscribe().with(System.out::println);
    }
}
