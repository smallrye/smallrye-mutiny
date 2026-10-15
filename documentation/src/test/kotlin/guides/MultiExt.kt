package guides

import io.smallrye.mutiny.multi
import io.smallrye.mutiny.subscription.BackPressureStrategy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import io.smallrye.mutiny.coroutines.multi as multiSuspend

fun regularMultiBuilder() {
    // tag::multiBuilder[]
    // import io.smallrye.mutiny.multi
    val multi = multi<Int> {
        emit(1)
        emit(2)
        emit(3)
    }
    // end::multiBuilder[]
}

fun multiBuilderWithBackPressure() {
    // tag::multiBuilderBackPressure[]
    // import io.smallrye.mutiny.multi
    val multi = multi<Int>(backPressure = BackPressureStrategy.DROP) {
        emit(1)
        emit(2)
        emit(3)
    }
    // end::multiBuilderBackPressure[]
}

@ExperimentalCoroutinesApi
suspend fun suspendMultiBuilder() {
    // tag::multiBuilderSuspend[]
    // import io.smallrye.mutiny.coroutines.multi
    val queryResult = fetchResults()
    coroutineScope {
        val multi = multiSuspend(context = this) {
            queryResult.forEach { result -> emit(result.download()) }
        }
    }
    // end::multiBuilderSuspend[]
}

interface ResultHandle {
    suspend fun download(): String
}

suspend fun fetchResults(): Iterable<ResultHandle> {
    TODO("Not yet implemented")
}
