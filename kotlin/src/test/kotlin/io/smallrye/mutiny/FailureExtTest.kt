package io.smallrye.mutiny

import io.smallrye.mutiny.helpers.test.AssertSubscriber
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber
import java.io.IOException
import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat

class FailureExtTest {

    @Test
    fun `test Uni reified onFailure recovers from matching exception`() {
        val uni = Uni.createFrom().failure<String>(IOException("disk full"))
        val subscriber = UniAssertSubscriber.create<String>()
        uni.onFailure<String, IOException>().recoverWithItem("recovered")
            .subscribe().withSubscriber(subscriber)
        assertThat(subscriber.awaitItem().item).isEqualTo("recovered")
    }

    @Test
    fun `test Uni reified onFailure does not recover from non-matching exception`() {
        val uni = Uni.createFrom().failure<String>(IllegalStateException("bad state"))
        val subscriber = UniAssertSubscriber.create<String>()
        uni.onFailure<String, IOException>().recoverWithItem("recovered")
            .subscribe().withSubscriber(subscriber)
        assertThat(subscriber.awaitFailure().failure).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `test Uni reified onFailure with no failure passes item through`() {
        val uni = Uni.createFrom().item("hello")
        val subscriber = UniAssertSubscriber.create<String>()
        uni.onFailure<String, IOException>().recoverWithItem("recovered")
            .subscribe().withSubscriber(subscriber)
        assertThat(subscriber.awaitItem().item).isEqualTo("hello")
    }

    @Test
    fun `test Multi reified onFailure recovers from matching exception`() {
        val multi = Multi.createFrom().failure<String>(IOException("disk full"))
        val subscriber = AssertSubscriber.create<String>(10)
        multi.onFailure<String, IOException>().recoverWithItem("recovered")
            .subscribe().withSubscriber(subscriber)
        subscriber.awaitCompletion().assertItems("recovered")
    }

    @Test
    fun `test Multi reified onFailure does not recover from non-matching exception`() {
        val multi = Multi.createFrom().failure<String>(IllegalStateException("bad state"))
        val subscriber = AssertSubscriber.create<String>(10)
        multi.onFailure<String, IOException>().recoverWithItem("recovered")
            .subscribe().withSubscriber(subscriber)
        subscriber.awaitFailure().assertFailedWith(IllegalStateException::class.java, "bad state")
    }

    @Test
    fun `test Multi reified onFailure with no failure passes items through`() {
        val multi = Multi.createFrom().items("a", "b", "c")
        val subscriber = AssertSubscriber.create<String>(10)
        multi.onFailure<String, IOException>().recoverWithItem("recovered")
            .subscribe().withSubscriber(subscriber)
        subscriber.awaitCompletion().assertItems("a", "b", "c")
    }
}
