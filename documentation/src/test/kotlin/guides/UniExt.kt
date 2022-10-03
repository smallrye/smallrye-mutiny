import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.replaceWithUnit
import io.smallrye.mutiny.uni

fun uniReplaceWithUnit() {
    val uni: Uni<String> = Uni.createFrom().item("")
    // <uniReplaceWithUnit>
    // import io.smallrye.mutiny.replaceWithUnit
    val unitUni : Uni<Unit> = uni.replaceWithUnit()
    assert(unitUni.await().indefinitely() === Unit)
    // </uniReplaceWithUnit>
}

fun regularUniBuilder() {
    // <uniBuilder>
    // import io.smallrye.mutiny.uni
    val uni: Uni<String> = uni { "λ 🚧" }
    // </uniBuilder>
}
