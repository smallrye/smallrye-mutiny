import io.smallrye.mutiny.Uni;

public class FirstProgram {

    public static void main(String[] args) {
        Uni.createFrom().item("hello")
           .onItem().transform(item -> item + " mutiny")
           .onItem().transform(String::toUpperCase)
           .subscribe().with(item -> System.out.println(">> " + item));
    }
}