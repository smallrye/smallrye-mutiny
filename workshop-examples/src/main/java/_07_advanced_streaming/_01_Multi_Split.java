///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.5.4
package _07_advanced_streaming;

import static _07_advanced_streaming._01_Multi_Split.Country.*;

import java.util.List;

import io.smallrye.mutiny.Multi;

public class _01_Multi_Split {

    static class TemperatureRecord {
        final Country country;
        final String city;
        final long timestamp;
        final double value;

        TemperatureRecord(Country country, String city, long timestamp, double value) {
            this.country = country;
            this.city = city;
            this.timestamp = timestamp;
            this.value = value;
        }

        @Override
        public String toString() {
            return "TemperatureRecord{" +
                    "country=" + country +
                    ", city='" + city + '\'' +
                    ", timestamp=" + timestamp +
                    ", value=" + value +
                    '}';
        }
    }

    enum Country {
        FRANCE,
        UK,
        AUSTRALIA
    }

    public static void main(String[] args) {
        System.out.println("⚡️ Multi split operator");

        var data = List.of(
                new TemperatureRecord(FRANCE, "Tassin-La-Demi-Lune", System.nanoTime(), 28.0),
                new TemperatureRecord(FRANCE, "Clermont-Ferrand", System.nanoTime(), 27.0),
                new TemperatureRecord(FRANCE, "Nevers", System.nanoTime(), 27.0),
                new TemperatureRecord(FRANCE, "Aubière", System.nanoTime(), 28.0),
                new TemperatureRecord(AUSTRALIA, "Sydney", System.nanoTime(), 16.0),
                new TemperatureRecord(FRANCE, "Lyon", System.nanoTime(), 29.0),
                new TemperatureRecord(AUSTRALIA, "Kensington", System.nanoTime(), 16.0),
                new TemperatureRecord(UK, "Newcastle", System.nanoTime(), 13.0),
                new TemperatureRecord(AUSTRALIA, "Coogee", System.nanoTime(), 16.0),
                new TemperatureRecord(UK, "Bexhill", System.nanoTime(), 22.0));

        var splitter = Multi.createFrom().iterable(data)
                .split(Country.class, record -> record.country);

        splitter.get(FRANCE)
                .subscribe().with(
                        record -> System.out.println("🇫🇷 => " + record),
                        Throwable::printStackTrace,
                        () -> System.out.println("✅ Done with France"));

        splitter.get(AUSTRALIA)
                .subscribe().with(
                        record -> System.out.println("🇦🇺🦘 => " + record),
                        Throwable::printStackTrace,
                        () -> System.out.println("✅ Done with Australia"));

        splitter.get(UK)
                .subscribe().with(
                        record -> System.out.println("🇬🇧 => " + record),
                        Throwable::printStackTrace,
                        () -> System.out.println("✅ Done with the UK"));

    }
}
