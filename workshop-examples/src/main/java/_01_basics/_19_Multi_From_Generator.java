///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.5.2
package _01_basics;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import io.smallrye.mutiny.Multi;

public class _19_Multi_From_Generator {

    public static void main(String[] args) {
        System.out.println("⚡️ Multi from generator");

        Multi.createFrom()
                .generator(MyState::new, (state, emitter) -> {
                    List<Integer> list = state.produceorNull();
                    if (list != null) {
                        emitter.emit(list);
                    }
                    return state;
                })
                .select().first(10)
                .subscribe().with(System.out::println);
    }

    private static class MyState {
        Random random = new Random();
        LinkedList<Integer> list = new LinkedList<>();

        List<Integer> produceorNull() {
            list.add(random.nextInt(10));
            if (list.size() > 3) {
                list.remove(0);
                return new ArrayList<>(list);
            } else {
                return null;
            }
        }
    }
}
