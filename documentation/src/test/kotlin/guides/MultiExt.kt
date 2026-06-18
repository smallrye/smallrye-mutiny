package guides

import io.smallrye.mutiny.multi
import io.smallrye.mutiny.subscription.BackPressureStrategy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import io.smallrye.mutiny.coroutines.multi as multiSuspend

fun regularMultiBuilder() {
    // <multiBuilder>
    // import io.smallrye.mutiny.multi
    val multi = multi<Int> {
        emit(1)
        emit(2)
        emit(3)
    }
    // </multiBuilder>
}

fun multiBuilderWithBackPressure() {
    // <multiBuilderBackPressure>
    // import io.smallrye.mutiny.multi
    val multi = multi<Int>(backPressure = BackPressureStrategy.DROP) {
        emit(1)
        emit(2)
        emit(3)
    }
    // </multiBuilderBackPressure>
}

@ExperimentalCoroutinesApi
suspend fun suspendMultiBuilder() {
    // <multiBuilderSuspend>
    // import io.smallrye.mutiny.coroutines.multi
    val queryResult = fetchResults()
    coroutineScope {
        val multi = multiSuspend(context = this) {
            queryResult.forEach { result -> emit(result.download()) }
        }
    }
    // </multiBuilderSuspend>
}

interface ResultHandle {
    suspend fun download(): String
}

suspend fun fetchResults(): Iterable<ResultHandle> {
    TODO("Not yet implemented")
}
