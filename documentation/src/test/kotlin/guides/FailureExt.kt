package guides

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.onFailure
import java.io.IOException

fun uniReifiedOnFailure() {
    // <uniReifiedOnFailure>
    // import io.smallrye.mutiny.onFailure
    val uni = Uni.createFrom().failure<String>(IOException("disk full"))
    val recovered: Uni<String> = uni.onFailure<String, IOException>()
        .recoverWithItem("recovered")
    // </uniReifiedOnFailure>
}

fun multiReifiedOnFailure() {
    // <multiReifiedOnFailure>
    // import io.smallrye.mutiny.onFailure
    val multi = Multi.createFrom().failure<String>(IOException("disk full"))
    val recovered: Multi<String> = multi.onFailure<String, IOException>()
        .recoverWithItem("recovered")
    // </multiReifiedOnFailure>
}
