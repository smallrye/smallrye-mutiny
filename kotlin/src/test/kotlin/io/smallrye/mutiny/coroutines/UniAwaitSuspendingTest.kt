package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.Uni
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class UniAwaitSuspendingTest {

    @Test
    fun `test immediate onItem event`() {
        runBlocking {
            // Given
            val item = UUID.randomUUID()
            val uni = Uni.createFrom().item(item)

            // When
            val eventItem = uni.awaitSuspending()

            // Then
            assertEquals(item, eventItem)
        }
    }

    @Test
    fun `test delayed onItem event`() {
        runBlocking {
            // Given
            val item = UUID.randomUUID()
            val delayed = Duration.ofMillis(250)
            val uni = Uni.createFrom().item(item).onItem().delayIt().by(delayed)

            // When
            val (beforeSubscribe, eventItem, afterOnItem) =
                Triple(Instant.now(), uni.awaitSuspending(), Instant.now())


            // Then
            assertEquals(item, eventItem)
            assertTrue(Duration.between(beforeSubscribe, afterOnItem) >= delayed)
        }
    }

    @Test
    fun `test null item`() {
        runBlocking {
            // Given
            val uni = Uni.createFrom().nullItem<Any>()

            // When
            val eventItem = uni.awaitSuspending()

            // Then
            assertNull(eventItem)
        }
    }

    @Test
    fun `test immediate RuntimeException failure`() {
        // Given
        val uni = Uni.createFrom().failure<Any>(RuntimeException("boom"))

        // When & Then
        assertFailsWith<RuntimeException>("boom") { runBlocking { uni.awaitSuspending() } }
    }

    @Test
    fun `test immediate checked Exception failure`() {
        // Given
        val uni = Uni.createFrom().failure<Any>(Exception("kaboom"))

        // When & Then
        assertFailsWith<Exception>("kaboom") { runBlocking { uni.awaitSuspending() } }
    }

    @Test
    fun `verify that cancelled coroutine unsubscribes and never receive any event`() {
        // Given
        val uni = Uni.createFrom().item("unreachable").onItem().delayIt().by(Duration.ofMillis(250))

        // When
        var retrieveError: Throwable? = null
        runBlocking {
            val job = launch {
                try {
                    uni.awaitSuspending()
                    fail()
                } catch (th: Throwable) {
                    retrieveError = th
                }
            }
            delay(50)
            job.cancel(CancellationException("abort"))
        }
        Thread.sleep(350)

        // Then
        assertThat(retrieveError).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun `test coroutine timeout when nothing happens`() {
        // Given
        val uni = Uni.createFrom().nothing<Any>()

        // When & Then
        assertFailsWith<TimeoutCancellationException> {
            runBlocking {
                withTimeout(50) {
                    uni.awaitSuspending()
                    fail()
                }
            }
        }
    }
}
