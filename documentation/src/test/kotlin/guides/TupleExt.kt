package guides

import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.component1
import io.smallrye.mutiny.component2

fun tupleDestructuring() {
    // <tupleDestructuring>
    // import io.smallrye.mutiny.component1
    // import io.smallrye.mutiny.component2
    val uni = Uni.combine().all()
        .unis(
            Uni.createFrom().item("Alice"),
            Uni.createFrom().item(30)
        ).asTuple()

    val (name, age) = uni.await().indefinitely()
    assert(name == "Alice")
    assert(age == 30)
    // </tupleDestructuring>
}
