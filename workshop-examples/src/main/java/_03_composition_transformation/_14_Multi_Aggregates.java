///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:3.0.3
package _03_composition_transformation;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class _14_Multi_Aggregates {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("⚡️ Multi aggregates");

        // ------------------------------------------------------------------ //

        var persons = Multi.createBy().repeating()
                .supplier(() -> generate()).atMost(100);

        // ------------------------------------------------------------------ //

        System.out.println();

        persons
                .onItem().scan(() -> 0, (count, next) -> count + 1)
                .subscribe().with(count -> System.out.println("We have " + count + " persons"));

        // ------------------------------------------------------------------ //

        System.out.println();

        persons
                .collect().with(Collectors.counting())
                .subscribe().with(count -> System.out.println("We have " + count + " persons"));

        // ------------------------------------------------------------------ //

        System.out.println();

        persons
                .select().where(person -> person.city.equals("Nevers"))
                .collect().asList()
                .subscribe().with(list -> System.out.println("They live in Nevers: " + list));

        // ------------------------------------------------------------------ //

        System.out.println();

        persons
                .select().where(person -> person.city.equals("Nevers"))
                .collect().in(
                        StringBuilder::new,
                        (acc, next) -> acc.append("\n")
                                .append(" -> ")
                                .append(next.identifier))
                .subscribe().with(list -> System.out.println("They live in Nevers: " + list));

        // ------------------------------------------------------------------ //

        System.out.println();

        var agePerCity = persons
                .group().by(person -> person.city)
                .onItem().transformToUni(group -> {
                    String city = group.key();
                    Uni<Double> avg = group.collect().with(Collectors.averagingInt(person -> person.age));
                    return avg.onItem().transform(res -> "Average age in " + city + " is " + res);
                }).merge();

        agePerCity.subscribe().with(System.out::println);
    }

    // ------------------------------------------------------------------ //

    static Person generate() {
        var rand = ThreadLocalRandom.current();
        return new Person(
                UUID.randomUUID().toString(),
                rand.nextInt(18, 50),
                cities.get(rand.nextInt(cities.size())));
    }

    static List<String> cities = Arrays.asList("Lyon", "Tassin La Demi Lune", "Clermont-Ferrand", "Nevers");

    // ------------------------------------------------------------------ //

    private static class Person {
        final String identifier;
        final int age;
        final String city;

        Person(String identifier, int age, String city) {
            this.identifier = identifier;
            this.age = age;
            this.city = city;
        }

        @Override
        public String toString() {
            return "Person{" +
                    "identifier='" + identifier + '\'' +
                    ", age=" + age +
                    ", city='" + city + '\'' +
                    '}';
        }
    }
}
