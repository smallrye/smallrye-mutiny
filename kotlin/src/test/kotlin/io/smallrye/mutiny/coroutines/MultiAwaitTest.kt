package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.MultiEmitter
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat

class MultiAwaitTest {

    @Test
    fun `test awaitList with items`() {
        testBlocking {
            val multi = Multi.createFrom().items(1, 2, 3)
            val list = multi.awaitList()
            assertThat(list).containsExactly(1, 2, 3)
        }
    }

    @Test
    fun `test awaitList with empty multi`() {
        testBlocking {
            val multi = Multi.createFrom().empty<String>()
            val list = multi.awaitList()
            assertThat(list).isEmpty()
        }
    }

    @Test
    fun `test awaitList with failure`() {
        assertFailsWith<RuntimeException>("boom") {
            testBlocking {
                val multi = Multi.createFrom().failure<String>(RuntimeException("boom"))
                multi.awaitList()
            }
        }
    }

    @Test
    fun `test awaitFirst with items`() {
        testBlocking {
            val multi = Multi.createFrom().items("a", "b", "c")
            val first = multi.awaitFirst()
            assertThat(first).isEqualTo("a")
        }
    }

    @Test
    fun `test awaitFirst with empty multi returns null`() {
        testBlocking {
            val multi = Multi.createFrom().empty<String>()
            val first = multi.awaitFirst()
            assertThat(first).isNull()
        }
    }

    @Test
    fun `test awaitFirstOrThrow with items`() {
        testBlocking {
            val multi = Multi.createFrom().items("a", "b", "c")
            val first = multi.awaitFirstOrThrow()
            assertThat(first).isEqualTo("a")
        }
    }

    @Test
    fun `test awaitFirstOrThrow with empty multi throws`() {
        assertFailsWith<NoSuchElementException> {
            testBlocking {
                val multi = Multi.createFrom().empty<String>()
                multi.awaitFirstOrThrow()
            }
        }
    }

    @Test
    fun `test awaitLast with items`() {
        testBlocking {
            val multi = Multi.createFrom().items("a", "b", "c")
            val last = multi.awaitLast()
            assertThat(last).isEqualTo("c")
        }
    }

    @Test
    fun `test awaitLast with empty multi returns null`() {
        testBlocking {
            val multi = Multi.createFrom().empty<String>()
            val last = multi.awaitLast()
            assertThat(last).isNull()
        }
    }

    @Test
    fun `test awaitLastOrThrow with items`() {
        testBlocking {
            val multi = Multi.createFrom().items("a", "b", "c")
            val last = multi.awaitLastOrThrow()
            assertThat(last).isEqualTo("c")
        }
    }

    @Test
    fun `test awaitLastOrThrow with empty multi throws`() {
        assertFailsWith<NoSuchElementException> {
            testBlocking {
                val multi = Multi.createFrom().empty<String>()
                multi.awaitLastOrThrow()
            }
        }
    }

    @Test
    fun `test awaitEach processes all items`() {
        testBlocking {
            val multi = Multi.createFrom().items(1, 2, 3)
            val collected = mutableListOf<Int>()
            multi.awaitEach { collected.add(it) }
            assertThat(collected).containsExactly(1, 2, 3)
        }
    }

    @Test
    fun `test awaitEach with empty multi`() {
        testBlocking {
            val multi = Multi.createFrom().empty<String>()
            val collected = mutableListOf<String>()
            multi.awaitEach { collected.add(it) }
            assertThat(collected).isEmpty()
        }
    }

    @Test
    fun `test awaitEach with failure`() {
        assertFailsWith<RuntimeException>("boom") {
            testBlocking {
                val multi = Multi.createFrom().emitter { em: MultiEmitter<in Int> ->
                    em.emit(1)
                    em.fail(RuntimeException("boom"))
                }
                multi.awaitEach { }
            }
        }
    }

    @Test
    fun `test awaitLast with failure after items`() {
        assertFailsWith<RuntimeException>("boom") {
            testBlocking {
                val multi = Multi.createFrom().emitter { em: MultiEmitter<in String> ->
                    em.emit("a")
                    em.emit("b")
                    em.fail(RuntimeException("boom"))
                }
                multi.awaitLast()
            }
        }
    }

    @Test
    fun `test awaitLastOrThrow with failure`() {
        assertFailsWith<RuntimeException>("boom") {
            testBlocking {
                val multi = Multi.createFrom().failure<String>(RuntimeException("boom"))
                multi.awaitLastOrThrow()
            }
        }
    }

    @Test
    fun `test awaitFirstOrThrow with failure`() {
        assertFailsWith<RuntimeException>("boom") {
            testBlocking {
                val multi = Multi.createFrom().failure<String>(RuntimeException("boom"))
                multi.awaitFirstOrThrow()
            }
        }
    }

    @Test
    fun `test awaitEach with failure after items delivers items then throws`() {
        val collected = mutableListOf<String>()
        assertFailsWith<RuntimeException>("boom") {
            testBlocking {
                val multi = Multi.createFrom().emitter { em: MultiEmitter<in String> ->
                    em.emit("a")
                    em.emit("b")
                    em.fail(RuntimeException("boom"))
                }
                multi.awaitEach { collected.add(it) }
            }
        }
        assertThat(collected).isNotEmpty
    }

    @Test
    fun `test awaitEach supports suspend action`() {
        testBlocking {
            val multi = Multi.createFrom().items(1, 2, 3)
            val collected = mutableListOf<Int>()
            multi.awaitEach { item ->
                delay(1.milliseconds)
                collected.add(item)
            }
            assertThat(collected).containsExactly(1, 2, 3)
        }
    }

    @Test
    fun `test awaitList respects coroutine cancellation`() {
        assertFailsWith<TimeoutCancellationException> {
            testBlocking {
                val multi = Multi.createFrom().nothing<String>()
                withTimeout(50.milliseconds) {
                    multi.awaitList()
                }
            }
        }
    }

    @Test
    fun `test awaitFirst with delayed items`() {
        testBlocking {
            val multi = Multi.createFrom().emitter { em: MultiEmitter<in String> ->
                embeddedScope().launch {
                    delay(50.milliseconds)
                    em.emit("delayed")
                    em.complete()
                }
            }
            val first = multi.awaitFirst()
            assertThat(first).isEqualTo("delayed")
        }
    }
}
