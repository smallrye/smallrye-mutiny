import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
// <importStatements>
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.asMulti
import io.smallrye.mutiny.coroutines.asUni
import io.smallrye.mutiny.coroutines.awaitSuspending
// </importStatements>
import io.smallrye.mutiny.coroutines.uni
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

suspend fun uniAwaitSuspending() {
    // <uniAwaitSuspending>
    val uni: Uni<String> = Uni.createFrom().item("Mutiny ❤ Kotlin")
    try {
        // Available within suspend function and CoroutineScope
        val item: String = uni.awaitSuspending()
    } catch (failure: Throwable) {
        // onFailure event happened
    }
    // </uniAwaitSuspending>
}

@ExperimentalCoroutinesApi
suspend fun deferredAsUni() {
    // <deferredAsUni>
    val deferred: Deferred<String> = GlobalScope.async { "Kotlin ❤ Mutiny" }
    val uni: Uni<String> = deferred.asUni()
    // </deferredAsUni>
}

@ExperimentalCoroutinesApi
suspend fun multiAsFlow() {
    // <multiAsFlow>
    val multi: Multi<String> = Multi.createFrom().items("Mutiny", "❤", "Kotlin")
    val flow: Flow<String> = multi.asFlow()
    // </multiAsFlow>
}

suspend fun flowAsMulti() {
    // <flowAsMulti>
    val flow: Flow<String> = flowOf("Kotlin", "❤", "Mutiny")
    val multi: Multi<String> = flow.asMulti()
    // </flowAsMulti>
}

suspend fun uniBuilder() {
    // <uniBuilder>
    // import io.smallrye.mutiny.coroutines.uni
    coroutineScope {
        val uni: Uni<String> = uni { "λ 🚧" }
    }
    // </uniBuilder>
}
