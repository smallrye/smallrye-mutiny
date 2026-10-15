package guides

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.coroutines.awaitList
import io.smallrye.mutiny.coroutines.awaitFirst
import io.smallrye.mutiny.coroutines.awaitFirstOrThrow
import io.smallrye.mutiny.coroutines.awaitLast
import io.smallrye.mutiny.coroutines.awaitLastOrThrow
import io.smallrye.mutiny.coroutines.awaitEach

suspend fun multiAwaitList() {
    // tag::multiAwaitList[]
    val multi = Multi.createFrom().items("a", "b", "c")
    val list: List<String> = multi.awaitList()
    // end::multiAwaitList[]
}

suspend fun multiAwaitFirst() {
    // tag::multiAwaitFirst[]
    val multi = Multi.createFrom().items(1, 2, 3)
    val first: Int? = multi.awaitFirst()
    val firstOrThrow: Int = multi.awaitFirstOrThrow()
    // end::multiAwaitFirst[]
}

suspend fun multiAwaitLast() {
    // tag::multiAwaitLast[]
    val multi = Multi.createFrom().items(1, 2, 3)
    val last: Int? = multi.awaitLast()
    val lastOrThrow: Int = multi.awaitLastOrThrow()
    // end::multiAwaitLast[]
}

suspend fun multiAwaitEach() {
    // tag::multiAwaitEach[]
    val multi = Multi.createFrom().items(1, 2, 3)
    multi.awaitEach { item ->
        println("Processing $item")
    }
    // end::multiAwaitEach[]
}
