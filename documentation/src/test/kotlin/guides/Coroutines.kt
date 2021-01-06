import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
// tag::importStatements[]
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.asMulti
import io.smallrye.mutiny.coroutines.asUni
import io.smallrye.mutiny.coroutines.awaitSuspending
// end::importStatements[]
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

suspend fun uniAwaitSuspending() {
    // tag::uniAwaitSuspending[]
    val uni: Uni<String> = Uni.createFrom().item("Mutiny ❤ Kotlin")
    try {
        // Available within suspend function and CoroutineScope
        val item: String = uni.awaitSuspending()
    } catch (failure: Throwable) {
        // onFailure event happened
    }
    // end::uniAwaitSuspending[]
}

@ExperimentalCoroutinesApi
suspend fun deferredAsUni() {
    // tag::deferredAsUni[]
    val deferred: Deferred<String> = GlobalScope.async { "Kotlin ❤ Mutiny" }
    val uni: Uni<String> = deferred.asUni()
    // end::deferredAsUni[]
}

@ExperimentalCoroutinesApi
suspend fun multiAsFlow() {
    // tag::multiAsFlow[]
    val multi: Multi<String> = Multi.createFrom().items("Mutiny", "❤", "Kotlin")
    val flow: Flow<String> = multi.asFlow()
    // end::multiAsFlow[]
}

suspend fun flowAsMulti() {
    // tag::flowAsMulti[]
    val flow: Flow<String> = flowOf("Kotlin", "❤", "Mutiny")
    val multi: Multi<String> = flow.asMulti()
    // end::flowAsMulti[]
}