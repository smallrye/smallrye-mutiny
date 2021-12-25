package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.Uni
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat

class UniAwaitSuspendingTest {

    @Test
    fun `test immediate onItem event`() {
        testBlocking {
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
        testBlocking {
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
        testBlocking {
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
        assertFailsWith<RuntimeException>("boom") { testBlocking { uni.awaitSuspending() } }
    }

    @Test
    fun `test immediate checked Exception failure`() {
        // Given
        val uni = Uni.createFrom().failure<Any>(Exception("kaboom"))

        // When & Then
        assertFailsWith<Exception>("kaboom") { testBlocking { uni.awaitSuspending() } }
    }

    @Test
    fun `verify that cancelled coroutine unsubscribes and never receive any event`() {
        // Given
        val uni = Uni.createFrom().item("unreachable").onItem().delayIt().by(Duration.ofMillis(250))

        // When
        var retrieveError: Throwable? = null
        testBlocking {
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
            testBlocking {
                withTimeout(50) {
                    uni.awaitSuspending()
                    fail()
                }
            }
        }
    }

    @Test
    fun `verify that Uni emission is not blocked by the caller`() {
        // Given
        val uni = Uni.createFrom().item(23)

        // When
        val item = testBlocking(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
            uni.awaitSuspending()
        }

        // Then
        assertThat(item).isEqualTo(23)
    }
}
