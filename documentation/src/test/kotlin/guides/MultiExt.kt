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
    coroutineScope {
        val multi = multiSuspend<String>(context = this) {
            emit("hello")
            emit("world")
        }
    }
    // </multiBuilderSuspend>
}
