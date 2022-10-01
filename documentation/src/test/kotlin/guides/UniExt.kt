import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.replaceWithUnit

fun uniReplaceWithUnit() {
    val uni: Uni<String> = Uni.createFrom().item("")
    // <uniReplaceWithUnit>
    val unitUni : Uni<Unit> = uni.replaceWithUnit()
    assert(unitUni.await().indefinitely() === Unit)
    // </uniReplaceWithUnit>
}
