package io.smallrye.mutiny.coroutines

import kotlinx.coroutines.CancellationException

/**
 * Process [block] but suppress [CancellationException] completely.
 *
 */
internal inline fun suppressCancellationException(block: () -> Unit) =
    try {
        block()
    } catch (e: CancellationException) {
        // CancellationExceptions are likely to happen if an emitter processes during cancellation/unsubscription.
    }