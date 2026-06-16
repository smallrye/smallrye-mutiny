package guides

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.coroutines.awaitList
import io.smallrye.mutiny.coroutines.awaitFirst
import io.smallrye.mutiny.coroutines.awaitFirstOrThrow
import io.smallrye.mutiny.coroutines.awaitLast
import io.smallrye.mutiny.coroutines.awaitLastOrThrow
import io.smallrye.mutiny.coroutines.awaitEach

suspend fun multiAwaitList() {
    // <multiAwaitList>
    val multi = Multi.createFrom().items("a", "b", "c")
    val list: List<String> = multi.awaitList()
    // </multiAwaitList>
}

suspend fun multiAwaitFirst() {
    // <multiAwaitFirst>
    val multi = Multi.createFrom().items(1, 2, 3)
    val first: Int? = multi.awaitFirst()
    val firstOrThrow: Int = multi.awaitFirstOrThrow()
    // </multiAwaitFirst>
}

suspend fun multiAwaitLast() {
    // <multiAwaitLast>
    val multi = Multi.createFrom().items(1, 2, 3)
    val last: Int? = multi.awaitLast()
    val lastOrThrow: Int = multi.awaitLastOrThrow()
    // </multiAwaitLast>
}

suspend fun multiAwaitEach() {
    // <multiAwaitEach>
    val multi = Multi.createFrom().items(1, 2, 3)
    multi.awaitEach { item ->
        println("Processing $item")
    }
    // </multiAwaitEach>
}
