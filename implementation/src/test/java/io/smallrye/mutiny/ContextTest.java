package io.smallrye.mutiny;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.helpers.BlockingIterable;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniDelegatingSubscriber;

class ContextTest {

    @Nested
    @DisplayName("Context factory methods")
    class ContextFactoryMethods {

        @Test
        void empty() {
            Context context = Context.empty();
            assertThat(context.keys()).isEmpty();
        }

        @Test
        void balancedOf() {
            Context context = Context.of("foo", "bar", "abc", "def");
            assertThat(context.keys())
                    .hasSize(2)
                    .contains("foo", "abc");
        }

        @Test
        void emptyOf() {
            Context context = Context.of();
            assertThat(context.keys()).isEmpty();
        }

        @Test
        void nullOf() {
            assertThatThrownBy(() -> Context.of((Object[]) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("The entries array cannot be null");
        }

        @Test
        void unbalancedOf() {
            assertThatThrownBy(() -> Context.of("foo", "bar", "baz"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Arguments must be balanced to form (key, value) pairs");
        }

        @Test
        void from() {
            HashMap<String, String> map = new HashMap<String, String>() {
                {
                    put("foo", "bar");
                    put("abc", "def");
                }
            };

            Context context = Context.from(map);
            assertThat(context.keys())
                    .hasSize(2)
                    .contains("foo", "abc");

            map.put("123", "456");
            assertThat(map).hasSize(3);
            assertThat(context.keys()).hasSize(2);
        }

        @Test
        void fromNull() {
            assertThatThrownBy(() -> Context.from(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("The entries map cannot be null");
        }

        @Test
        // This will please CodeCov
        void toStringAndEquals() {
            Context ctx1 = Context.of("foo", "bar");
            Context ctx2 = Context.of("foo", "bar");
            Context ctx3 = Context.of("foo", "bar", "abc", "def");
            Context ctx4 = Context.empty();
            Context ctx5 = Context.empty();

            assertThat(ctx1).isEqualTo(ctx2);
            assertThat(ctx2).isNotEqualTo(ctx3);
            assertThat(ctx2).isNotEqualTo(ctx4);
            assertThat(ctx5).isEqualTo(ctx5);

            assertThat(ctx1.toString()).isEqualTo("Context{entries={foo=bar}}");
            assertThat(ctx4.toString()).isEqualTo("Context{entries=null}");
        }
    }

    @Nested
    @DisplayName("Context methods")
    class ContextMethods {

        @Test
        void sanityChecks() {
            Context context = Context.of("foo", "bar", "123", 456);

            assertThat(context.keys())
                    .hasSize(2)
                    .contains("foo", "123");

            assertThat(context.contains("foo")).isTrue();
            assertThat(context.contains("bar")).isFalse();
            assertThat(context.isEmpty()).isFalse();

            assertThat(context.<String> get("foo")).isEqualTo("bar");
            assertThat(context.getOrElse("bar", () -> "666")).isEqualTo("666");

            context.put("bar", 123);
            assertThat(context.keys()).hasSize(3);
            assertThat(context.getOrElse("bar", () -> 666)).isEqualTo(123);

            context.delete("foo").delete("123").delete("bar");
            assertThat(context.contains("foo")).isFalse();
            assertThat(context.isEmpty()).isTrue();
            assertThat(context.keys()).isEmpty();
        }

        @Test
        void containsOnEmptyContext() {
            assertThat(Context.empty().contains("foo")).isFalse();
        }

        @Test
        void getOnEmptyContext() {
            assertThatThrownBy(() -> Context.empty().get("foo"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("The context is empty");

            assertThat(Context.empty().getOrElse("foo", () -> "bar")).isEqualTo("bar");
        }

        @Test
        void getOnMissingKey() {
            assertThatThrownBy(() -> Context.of("foo", "bar").get("yolo"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("The context does not have a value for key yolo");
        }

        @Test
        void keysetIsACopy() {
            Context context = Context.of("foo", "bar", "123", 456);
            Set<String> k1 = context.keys();
            context.put("bar", "baz");
            Set<String> k2 = context.keys();
            assertThat(k1).isNotSameAs(k2);
        }
    }

    @Nested
    @DisplayName("Uni with context")
    class UniAndContext {

        @Test
        void noContextShallBeEmpty() {
            UniAssertSubscriber<String> sub = Uni.createFrom().item(63)
                    .withContext((uni, ctx) -> uni.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            sub.assertCompleted().assertItem("63::yolo");
        }

        @Test
        void transformWithContext() {
            Context context = Context.of("foo", "bar");

            UniAssertSubscriber<String> sub = Uni.createFrom().item(63)
                    .withContext((uni, ctx) -> uni.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().withSubscriber(UniAssertSubscriber.create(context));

            sub.assertCompleted().assertItem("63::bar");
        }

        @Test
        void withContextRejectsNullBuilder() {
            assertThatThrownBy(() -> Uni.createFrom().item(1).withContext(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("`builder` must not be `null`");
        }

        @Test
        void withContextRejectsNullReturningBuilder() {
            UniAssertSubscriber<Object> sub = Uni.createFrom().item(63)
                    .withContext((uni, ctx) -> null)
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            sub.assertFailedWith(NullPointerException.class, "The builder function returned null");
        }

        @Test
        void withContextRejectsThrowingBuilder() {
            UniAssertSubscriber<Object> sub = Uni.createFrom().item(63)
                    .withContext((uni, ctx) -> {
                        throw new RuntimeException("boom");
                    })
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            sub.assertFailedWith(RuntimeException.class, "boom");
        }

        @Test
        void callbacksPropagateContext() {
            Context context = Context.of("foo", "bar");
            AtomicReference<String> result = new AtomicReference<>();

            Uni.createFrom().item(63)
                    .withContext((uni, ctx) -> uni.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().with(context, result::set);

            assertThat(result.get())
                    .isNotNull()
                    .isEqualTo("63::bar");
        }

        @Test
        void asCompletableStagePropagateContext() {
            Context context = Context.of("foo", "bar");

            Uni.createFrom().item(63)
                    .withContext((uni, ctx) -> uni.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().asCompletionStage(context)
                    .whenComplete((str, err) -> {
                        assertThat(err).isNull();
                        assertThat(str).isEqualTo("63::bar");
                    });
        }

        @Test
        void serializedSubscriberDoesPropagateContext() {
            Context context = Context.of("foo", "bar");

            UniAssertSubscriber<String> sub = Uni.createFrom().item(63)
                    .withContext((uni, ctx) -> uni.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().withSerializedSubscriber(UniAssertSubscriber.create(context));

            sub.assertCompleted().assertItem("63::bar");
        }

        @Test
        void callbacksWithoutContextPropagateEmptyContext() {
            AtomicReference<String> result = new AtomicReference<>();

            Uni.createFrom().item(63)
                    .withContext((uni, ctx) -> uni.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().with(result::set);

            assertThat(result.get())
                    .isNotNull()
                    .isEqualTo("63::yolo");
        }

        @Test
        void contextDoesPropagate() {
            Context context = Context.of("foo", "bar");

            UniAssertSubscriber<String> sub = Uni.createFrom().item(63)
                    .withContext((uni, ctx) -> uni.onItem().invoke(() -> ctx.put("counter", ctx.<Integer> get("counter") + 1)))
                    .withContext((uni, ctx) -> uni
                            .onItem().invoke(() -> ctx.put("counter", ctx.<Integer> get("counter") + 1)))
                    .withContext((uni, ctx) -> uni
                            .onItem().invoke(() -> ctx.put("counter", ctx.<Integer> get("counter") + 1)))
                    .withContext((uni, ctx) -> uni
                            .onItem().transform(Object::toString)
                            .onItem().transform(s -> s + "::" + ctx.get("counter")))
                    .withContext((uni, ctx) -> {
                        ctx.put("counter", 0);
                        return uni;
                    })
                    .subscribe().withSubscriber(UniAssertSubscriber.create(context));

            sub.assertCompleted().assertItem("63::3");
            assertThat(context.<Integer> get("counter")).isEqualTo(3);
        }

        @Test
        void uniOperatorSubclassesPropagateContext() {
            Context context = Context.of("foo", "bar");

            UniAssertSubscriber<String> sub = Uni.createFrom().failure(new IOException("boom"))
                    .withContext((uni, ctx) -> uni
                            .onItem().transformToUni(obj -> Uni.createFrom().item("Yolo"))
                            .onFailure().recoverWithItem(ctx.<String> get("foo")))
                    .subscribe().withSubscriber(UniAssertSubscriber.create(context));

            sub.assertCompleted().assertItem("bar");
        }

        @Test
        void joinAllAndAttachContext() {
            Context context = Context.of("foo", "bar", "baz", "baz");

            Uni<Integer> a = Uni.createFrom().item(58)
                    .withContext((uni, ctx) -> uni.onItem().invoke(n -> ctx.put(n.toString(), n)));

            Uni<Integer> b = Uni.createFrom().item(63)
                    .withContext((uni, ctx) -> uni.onItem().invoke(n -> ctx.put(n.toString(), n)));

            Uni<Integer> c = Uni.createFrom().item(69)
                    .withContext((uni, ctx) -> uni.onItem().invoke(n -> ctx.put(n.toString(), n)));

            UniAssertSubscriber<String> sub = Uni.join().all(a, b, c).andFailFast()
                    .attachContext()
                    .onItem().transform(itemsWithContext -> {
                        Context ctx = itemsWithContext.context();
                        return itemsWithContext.get().toString() + "::" + ctx.get("58") + "::" + ctx.get("63") + "::"
                                + ctx.get("69");
                    })
                    .subscribe().withSubscriber(UniAssertSubscriber.create(context));

            sub.assertCompleted().assertItem("[58, 63, 69]::58::63::69");
        }

        @Test
        void joinFirstAndAttachContext() {
            Context context = Context.of("foo", "bar", "baz", "baz");

            Uni<Integer> a = Uni.createFrom().item(58)
                    .withContext((uni, ctx) -> uni.onItem().invoke(n -> ctx.put(n.toString(), n)));

            Uni<Integer> b = Uni.createFrom().item(63)
                    .withContext((uni, ctx) -> uni.onItem().invoke(n -> ctx.put(n.toString(), n)));

            Uni<Integer> c = Uni.createFrom().item(69)
                    .withContext((uni, ctx) -> uni.onItem().invoke(n -> ctx.put(n.toString(), n)));

            UniAssertSubscriber<String> sub = Uni.join().first(a, b, c).withItem()
                    .attachContext()
                    .onItem().transform(item -> {
                        Context ctx = item.context();
                        return item.get().toString() + "::" + ctx.get("58");
                    })
                    .subscribe().withSubscriber(UniAssertSubscriber.create(context));

            sub.assertCompleted().assertItem("58::58");
        }

        @Test
        void combineAllAndAttachContext() {
            Context context = Context.of("foo", "bar", "baz", "baz");

            Uni<Integer> a = Uni.createFrom().item(58)
                    .withContext((uni, ctx) -> uni.onItem().invoke(n -> ctx.put(n.toString(), n)));

            Uni<Integer> b = Uni.createFrom().item(63)
                    .withContext((uni, ctx) -> uni.onItem().invoke(n -> ctx.put(n.toString(), n)));

            Uni<Integer> c = Uni.createFrom().item(69)
                    .withContext((uni, ctx) -> uni.onItem().invoke(n -> ctx.put(n.toString(), n)));

            UniAssertSubscriber<String> sub = Uni.combine().all().unis(a, b, c).asTuple()
                    .attachContext()
                    .onItem().transform(itemsWithContext -> {
                        Context ctx = itemsWithContext.context();
                        return itemsWithContext.get().toString() + "::" + ctx.get("58") + "::" + ctx.get("63") + "::"
                                + ctx.get("69");
                    })
                    .subscribe().withSubscriber(UniAssertSubscriber.create(context));

            sub.assertCompleted().assertItem("Tuple{item1=58,item2=63,item3=69}::58::63::69");
        }

        @Test
        void combineAnyAndAttachContext() {
            Context context = Context.of("foo", "bar", "baz", "baz");

            Uni<Integer> a = Uni.createFrom().item(58)
                    .withContext((uni, ctx) -> uni.onItem().invoke(n -> ctx.put(n.toString(), n)));

            Uni<Integer> b = Uni.createFrom().item(63)
                    .withContext((uni, ctx) -> uni.onItem().invoke(n -> ctx.put(n.toString(), n)));

            Uni<Integer> c = Uni.createFrom().item(69)
                    .withContext((uni, ctx) -> uni.onItem().invoke(n -> ctx.put(n.toString(), n)));

            UniAssertSubscriber<String> sub = Uni.combine().any().of(a, b, c)
                    .attachContext()
                    .onItem().transform(item -> {
                        Context ctx = item.context();
                        return item.get().toString() + "::" + ctx.get("58");
                    })
                    .subscribe().withSubscriber(UniAssertSubscriber.create(context));

            sub.assertCompleted().assertItem("58::58");
        }

        @Test
        void memoization() {
            AtomicBoolean invalidator = new AtomicBoolean(false);

            Context firstContext = Context.of("foo", "bar", "baz", "baz");

            Uni<String> pipeline = Uni.createFrom().item(63)
                    .withContext((uni, ctx) -> {
                        ctx.put("abc", 123);
                        return uni;
                    })
                    .onItem().transform(Object::toString)
                    .memoize().until(invalidator::get);

            UniAssertSubscriber<String> sub = pipeline.subscribe().withSubscriber(UniAssertSubscriber.create(firstContext));

            assertThat(firstContext.contains("abc")).isTrue();
            assertThat(firstContext.contains("foo")).isTrue();
            assertThat(firstContext.<Integer> get("abc")).isEqualTo(123);
            assertThat(firstContext.<String> get("foo")).isEqualTo("bar");
            sub.assertCompleted().assertItem("63");

            Context secondContext = Context.empty();
            sub = pipeline.subscribe().withSubscriber(UniAssertSubscriber.create(secondContext));

            sub.assertCompleted().assertItem("63");
            assertThat(secondContext.isEmpty()).isTrue();

            invalidator.set(true);

            Context thirdContext = Context.empty();
            sub = pipeline.subscribe().withSubscriber(UniAssertSubscriber.create(thirdContext));

            sub.assertCompleted().assertItem("63");
            assertThat(thirdContext.<Integer> get("abc")).isEqualTo(123);
            assertThat(thirdContext.contains("foo")).isFalse();
        }

        @Test
        void numberOfCallsToContextMethod() {
            Context context = Context.of("foo", "bar", "baz", "baz");
            AtomicInteger counter = new AtomicInteger();

            UniAssertSubscriber<Integer> sub = Uni.createFrom().item(1)
                    .withContext((uni, ctx) -> uni
                            .onItem().transform(n -> n + "!")
                            .onItem().transform(s -> "[" + s + "]"))
                    .onItem().transform(String::toUpperCase)
                    .onItem().transform(String::length)
                    .withContext((uni, ctx) -> uni)
                    .subscribe().withSubscriber(new UniAssertSubscriber<Integer>() {
                        @Override
                        public Context context() {
                            counter.incrementAndGet();
                            return context;
                        }
                    });

            sub.assertCompleted().assertItem(4);
            assertThat(counter.get()).isEqualTo(2);
        }

        @Test
        void blockingAwait() {
            Context context = Context.of("foo", "bar", "baz", "baz");

            String res = Uni.createFrom().item(63)
                    .attachContext()
                    .onItem().transform(item -> item.get() + "::" + item.context().get("foo"))
                    .awaitUsing(context).indefinitely();

            assertThat(res).isEqualTo("63::bar");
        }

        @Test
        void blockingAwaitOptional() {
            Context context = Context.of("foo", "bar", "baz", "baz");

            Optional<String> res = Uni.createFrom().item(63)
                    .attachContext()
                    .onItem().transform(item -> item.get() + "::" + item.context().get("foo"))
                    .awaitUsing(context).asOptional().indefinitely();

            assertThat(res)
                    .isPresent()
                    .hasValue("63::bar");
        }

        @Test
        void uniDelegatingSubscriber() {
            Context context = Context.of("foo", "bar");
            AtomicReference<String> box = new AtomicReference<>();

            Uni.createFrom().item(63)
                    .withContext((uni, ctx) -> uni.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .onItem().invoke(box::set)
                    .subscribe().withSubscriber(new UniDelegatingSubscriber<>(UniAssertSubscriber.create(context)));

            assertThat(box.get())
                    .isNotNull()
                    .isEqualTo("63::bar");
        }

        @Test
        void plugAndFlatMap() {
            UniAssertSubscriber<ItemWithContext<String>> sub = Uni.createFrom().item("63")
                    .withContext((uni, ctx) -> {
                        ctx.put("foo", "bar");
                        return uni;
                    })
                    .plug(uni -> uni.flatMap(s -> Uni.createFrom().item("yolo")))
                    .attachContext()
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            sub.assertCompleted();
            ItemWithContext<String> item = sub.getItem();
            assertThat(item.get()).isEqualTo("yolo");
            assertThat(item.context().keys()).hasSize(1).contains("foo");
            assertThat(item.context().<String> get("foo")).isEqualTo("bar");
        }
    }

    @Nested
    @DisplayName("{Uni,Multi} <-> {Multi,Uni} bridges")
    class UniMultiBridges {

        @Test
        void uniToUni() {
            Context context = Context.of("foo", "bar");

            UniAssertSubscriber<String> sub = Uni.createFrom().item("abc")
                    .withContext((uni, ctx) -> uni
                            .onItem().transformToUni(s -> Uni.createFrom().item(s + "::" + ctx.get("foo"))))
                    .onFailure().recoverWithItem(() -> "woops")
                    .subscribe().withSubscriber(UniAssertSubscriber.create(context));

            sub.assertCompleted().assertItem("abc::bar");
        }

        @Test
        void multiToMulti() {
            Context context = Context.of("foo", "bar");

            AssertSubscriber<String> sub = Multi.createFrom().items(1, 2, 3)
                    .withContext((multi, ctx) -> multi
                            .onItem().transformToMultiAndMerge(n -> Multi.createFrom().item(n + "@" + ctx.get("foo"))))
                    .onFailure().recoverWithItem(() -> "woops")
                    .subscribe().withSubscriber(AssertSubscriber.create(context, 10));

            List<String> items = sub.assertCompleted().getItems();
            assertThat(items).hasSize(3)
                    .contains("1@bar", "2@bar", "3@bar");
        }

        @Test
        void uniToMulti() {
            Context context = Context.of("foo", "bar");

            AssertSubscriber<String> sub = Uni.createFrom().item("abc")
                    .withContext((uni, ctx) -> uni.onItem().transform(s -> s + "@" + ctx.get("foo")))
                    .onItem().transformToMulti(s -> Multi.createFrom().item(s))
                    .withContext((multi, ctx) -> multi
                            .onItem().transformToMultiAndConcatenate(s -> Multi.createFrom().items(s, ctx.get("foo"))))
                    .onFailure().recoverWithItem(() -> "woops")
                    .subscribe().withSubscriber(AssertSubscriber.create(context, 10));

            List<String> items = sub.assertCompleted().getItems();
            assertThat(items)
                    .hasSize(2)
                    .contains("abc@bar", "bar");
        }

        @Test
        void multiToUni() {
            Context context = Context.of("foo", "bar");

            UniAssertSubscriber<Object> sub = Multi.createFrom().items(1, 2, 3)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "@" + ctx.get("foo")))
                    .toUni()
                    .withContext((uni, ctx) -> uni
                            .onItem().transformToUni(s -> Uni.createFrom().item(s + "::" + ctx.get("foo"))))
                    .onFailure().recoverWithItem(() -> "woops")
                    .subscribe().withSubscriber(UniAssertSubscriber.create(context));

            sub.assertCompleted().assertItem("1@bar::bar");
        }
    }

    @Nested
    @DisplayName("Uni with context")
    class MultiAndContext {

        @Test
        void noContextShallBeEmpty() {
            AssertSubscriber<String> sub = Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().withSubscriber(AssertSubscriber.create(10L));

            sub.assertCompleted();
            assertThat(sub.getItems())
                    .hasSize(3)
                    .containsExactly("58::yolo", "63::yolo", "69::yolo");
        }

        @Test
        void transformWithContext() {
            Context context = Context.of("foo", "bar");

            AssertSubscriber<String> sub = Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().withSubscriber(AssertSubscriber.create(context, 10L));

            sub.assertCompleted();
            assertThat(sub.getItems())
                    .hasSize(3)
                    .containsExactly("58::bar", "63::bar", "69::bar");
        }

        @Test
        void withContextRejectsNullBuilder() {
            assertThatThrownBy(() -> Multi.createFrom().item(1).withContext(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("`builder` must not be `null`");
        }

        @Test
        void withContextRejectsNullReturningBuilder() {
            AssertSubscriber<Object> sub = Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> null)
                    .subscribe().withSubscriber(AssertSubscriber.create(10L));

            sub.assertFailedWith(NullPointerException.class, "The builder function returned null");
        }

        @Test
        void withContextRejectsThrowingBuilder() {
            AssertSubscriber<Object> sub = Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> {
                        throw new RuntimeException("boom");
                    })
                    .subscribe().withSubscriber(AssertSubscriber.create(10L));

            sub.assertFailedWith(RuntimeException.class, "boom");
        }

        @Test
        void callbacksPropagateContext() {
            Context context = Context.of("foo", "bar");
            ArrayList<String> list = new ArrayList<>();

            Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().with(context, list::add);

            assertThat(list)
                    .hasSize(3)
                    .containsExactly("58::bar", "63::bar", "69::bar");
        }

        @Test
        void callbacksWithoutContextPropagateEmptyContext() {
            ArrayList<String> list = new ArrayList<>();

            Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().with(list::add);

            assertThat(list)
                    .hasSize(3)
                    .containsExactly("58::yolo", "63::yolo", "69::yolo");
        }

        @Test
        void asIterableWithoutContext() {
            BlockingIterable<String> iter = Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().asIterable();

            assertThat(iter).containsExactly("58::yolo", "63::yolo", "69::yolo");
        }

        @Test
        void asIterableWithContext() {
            BlockingIterable<String> iter = Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().asIterable(() -> Context.of("foo", "bar"));

            assertThat(iter).containsExactly("58::bar", "63::bar", "69::bar");
        }

        @Test
        void asStreamWithoutContext() {
            Stream<String> stream = Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().asStream();

            assertThat(stream).containsExactly("58::yolo", "63::yolo", "69::yolo");
        }

        @Test
        void asStreamWithContext() {
            Stream<String> stream = Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().asStream(() -> Context.of("foo", "bar"));

            assertThat(stream).containsExactly("58::bar", "63::bar", "69::bar");
        }

        @Test
        void mergeStreamsAndManipulateContext() {
            Context context = Context.of("foo", "bar", "counter", 0);

            Multi<String> s1 = Multi.createFrom().range(0, 5)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")));

            Multi<String> s2 = Multi.createFrom().range(0, 5)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")));

            AssertSubscriber<String> sub = Multi.createBy().combining().streams(s1, s2).asTuple()
                    .withContext((multi, ctx) -> multi.onItem().transform(t -> t.getItem1() + "|" + t.getItem2()))
                    .withContext(
                            (multi, ctx) -> multi.onItem().invoke(() -> ctx.put("counter", ctx.<Integer> get("counter") + 1)))
                    .withContext((multi, ctx) -> multi.onItem().transformToUniAndConcatenate(s -> Uni
                            .createFrom().item(s + "@" + ctx.get("counter"))
                            .withContext((uni, nestedCtx) -> uni.onItem()
                                    .transform(str -> "[" + ctx.get("counter") + " -> " + str + "]"))))
                    .subscribe().withSubscriber(AssertSubscriber.create(context, Long.MAX_VALUE));

            sub.assertCompleted();
            assertThat(sub.getItems())
                    .hasSize(5)
                    .startsWith("[1 -> 0::bar|0::bar@1]")
                    .endsWith("[5 -> 4::bar|4::bar@5]");
        }

        @Test
        void multiByRepeating() {
            Context context = Context.of("foo", "bar", "counter", 0);

            AssertSubscriber<String> sub = Multi.createBy().repeating().uni(() -> Uni
                    .createFrom().item(63)
                    .withContext((uni, ctx) -> uni.onItem().transform(n -> {
                        ctx.put("counter", ctx.<Integer> get("counter") + 1);
                        return n + "::" + ctx.get("foo") + "@" + ctx.get("counter");
                    })))
                    .atMost(5)
                    .subscribe().withSubscriber(AssertSubscriber.create(context, Long.MAX_VALUE));

            sub.assertCompleted();
            assertThat(sub.getItems())
                    .hasSize(5)
                    .containsExactly("63::bar@1", "63::bar@2", "63::bar@3", "63::bar@4", "63::bar@5");
        }

        @Test
        void multiRetry() {
            Context context = Context.of("foo", "bar", "counter", 0);

            AssertSubscriber<String> sub = Multi.createFrom().item("foo=")
                    .withContext((multi, ctx) -> multi.onItem().transform(s -> {
                        ctx.put("counter", ctx.<Integer> get("counter") + 1);
                        return s + ctx.get("foo") + "@" + ctx.get("counter");
                    }))
                    .onItem().failWith(s -> new IOException(s))
                    .onFailure().retry().atMost(5)
                    .subscribe().withSubscriber(AssertSubscriber.create(context, Long.MAX_VALUE));

            sub.assertFailedWith(IOException.class, "foo=bar@6");
        }

        @Test
        void safeSubscriber() {
            Context context = Context.of("foo", "bar");

            Multi<String> someMulti = Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")));

            AssertSubscriber<String> sub = Multi.createFrom().publisher(Multi.createFrom().publisher(someMulti))
                    .subscribe().withSubscriber(AssertSubscriber.create(context, Long.MAX_VALUE));

            sub.assertCompleted();
            assertThat(sub.getItems())
                    .hasSize(3)
                    .containsExactly("58::bar", "63::bar", "69::bar");
        }

        @Test
        void broadcast() {
            Multi<String> someMulti = Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .broadcast().toAllSubscribers();

            AssertSubscriber<String> sub1 = someMulti.subscribe()
                    .withSubscriber(AssertSubscriber.create(Context.of("foo", "bar")));
            AssertSubscriber<String> sub2 = someMulti.subscribe().withSubscriber(AssertSubscriber.create());

            sub1.request(1L);
            sub2.request(1L);
            sub2.request(1L);
            sub2.request(1L);
            sub1.request(1L);
            sub1.request(1L);
            sub2.request(1L);

            sub1.assertCompleted();
            sub2.assertCompleted();

            assertThat(sub1.getItems())
                    .hasSize(3)
                    .containsExactly("58::bar", "63::bar", "69::bar");

            assertThat(sub2.getItems())
                    .hasSize(3)
                    .containsExactly("58::bar", "63::bar", "69::bar");
        }

        @Test
        void cache() {
            Multi<String> someMulti = Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .cache();

            AssertSubscriber<String> sub1 = someMulti.subscribe()
                    .withSubscriber(AssertSubscriber.create(Context.of("foo", "bar"), Long.MAX_VALUE));
            sub1.assertCompleted();
            assertThat(sub1.getItems())
                    .hasSize(3)
                    .containsExactly("58::bar", "63::bar", "69::bar");

            AssertSubscriber<String> sub2 = someMulti.subscribe()
                    .withSubscriber(AssertSubscriber.create(Context.of("foo", "baz"), Long.MAX_VALUE));
            sub2.assertCompleted();
            assertThat(sub2.getItems())
                    .hasSize(3)
                    .containsExactly("58::bar", "63::bar", "69::bar");
        }

        @Test
        void attachContext() {
            Context context = Context.of("foo", "bar");

            AssertSubscriber<String> sub = Multi.createFrom().items(58, 63, 69)
                    .attachContext()
                    .onItem().transform(n -> n.get() + "::" + n.context().getOrElse("foo", () -> "yolo"))
                    .subscribe().withSubscriber(AssertSubscriber.create(context, Long.MAX_VALUE));

            sub.assertCompleted();
            assertThat(sub.getItems())
                    .hasSize(3)
                    .containsExactly("58::bar", "63::bar", "69::bar");
        }

        @Test
        void emitAndSubscribeOn() {
            Context context = Context.empty();
            ExecutorService pool = Executors.newFixedThreadPool(4);

            AssertSubscriber<String> sub = Multi.createFrom().range(1, 100)
                    .withContext((multi, ctx) -> {
                        ctx.put("foo", "bar");
                        return multi.onItem().transform(n -> {
                            String key = n.toString();
                            context.put(key, n + "::" + ctx.get("foo"));
                            return key;
                        });
                    })
                    .emitOn(pool)
                    .runSubscriptionOn(pool)
                    .subscribe().withSubscriber(AssertSubscriber.create(context, Long.MAX_VALUE));

            sub.awaitCompletion(Duration.ofSeconds(5));

            assertThat(sub.getItems())
                    .hasSize(99)
                    .contains("63", "99", "58", "69");

            assertThat(context.keys()).contains("63", "99", "58", "69");
            assertThat(context.<String> get("63")).isEqualTo("63::bar");

            pool.shutdown();
        }

        @Test
        void collectToList() {
            Context context = Context.of("foo", "bar");

            AssertSubscriber<List<String>> sub = Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .select().where(s -> !s.startsWith("69"))
                    .collect().asList()
                    .toMulti()
                    .subscribe().withSubscriber(AssertSubscriber.create(context, Long.MAX_VALUE));

            assertThat(sub.getItems().get(0))
                    .hasSize(2)
                    .contains("63::bar", "58::bar");
        }

        @Test
        void groupBy() {
            Context context = Context.of("foo", "bar");

            AssertSubscriber<GroupedMulti<String, String>> sub = Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .group().by(str -> str.substring(0, 2))
                    .subscribe().withSubscriber(AssertSubscriber.create(context, Long.MAX_VALUE));

            sub.assertCompleted();
            assertThat(sub.getItems().get(0).key()).isEqualTo("58");
            assertThat(sub.getItems().get(0).collect().asList().await().indefinitely())
                    .hasSize(1)
                    .contains("58::bar");
        }

        @Test
        void plugAndFlatMap() {
            AssertSubscriber<ItemWithContext<String>> sub = Multi.createFrom().items(58, 63, 69)
                    .withContext((multi, ctx) -> multi.onItem().invoke(() -> ctx.put("foo", "bar")))
                    .plug(multi -> multi.flatMap(n -> Multi.createFrom().items(n.toString())))
                    .attachContext()
                    .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

            sub.assertCompleted();
            List<ItemWithContext<String>> items = sub.getItems();
            assertThat(items)
                    .hasSize(3)
                    .anyMatch(item -> item.get().equals("63"))
                    .allMatch(item -> item.context().get("foo").equals("bar"));
        }

        @Test
        void alienSubscriber() {
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<Throwable> error = new AtomicReference<>();
            ArrayList<String> items = new ArrayList<>();

            Multi.createFrom().items(58, 63, 69)
                    .attachContext()
                    .onItem().transform(n -> n.get() + "::" + n.context().getOrElse("foo", () -> "yolo"))
                    .subscribe().withSubscriber(new AlienSubscriber<>(items, error, completed));

            assertThat(completed).isTrue();
            assertThat(error).hasValue(null);
            assertThat(items)
                    .hasSize(3)
                    .containsExactly("58::yolo", "63::yolo", "69::yolo");
        }

        @Test
        void timeGrouping() {
            Context context = Context.of("foo", "bar");

            AssertSubscriber<List<String>> sub = Multi.createFrom().items(58, 63, 69)
                    .attachContext()
                    .onItem().transform(n -> n.get() + "::" + n.context().getOrElse("foo", () -> "yolo"))
                    .group().intoLists().every(Duration.ofSeconds(5L))
                    .subscribe().withSubscriber(AssertSubscriber.create(context, Long.MAX_VALUE));

            sub.awaitCompletion();
            assertThat(sub.getItems()).hasSize(1);
            assertThat(sub.getItems().get(0))
                    .hasSize(3)
                    .containsExactly("58::bar", "63::bar", "69::bar");
        }
    }

    @Nested
    @DisplayName("Uni and Multi context-aware builders")
    class Builders {

        @Test
        void uniEmitter() {
            Context context = Context.of("foo", "bar");

            UniAssertSubscriber<Object> sub = Uni.createFrom().emitter(em -> {
                em.complete(em.context().getOrElse("foo", () -> "yolo"));
            })
                    .subscribe().withSubscriber(UniAssertSubscriber.create(context));

            sub.assertCompleted().assertItem("bar");
        }

        @Test
        void multiEmitter() {
            Context context = Context.of("foo", "bar");

            AssertSubscriber<Object> sub = Multi.createFrom().emitter(em -> {
                em.emit(em.context().getOrElse("foo", () -> "yolo"));
                em.complete();
            })
                    .subscribe().withSubscriber(AssertSubscriber.create(context, 10L));

            sub.assertCompleted().assertItems("bar");
        }

        @Test
        void multiCreateWithContext() {
            Context context = Context.of("foo", "bar");

            AssertSubscriber<String> sub = Multi
                    .createFrom().context(ctx -> Multi.createFrom().item(ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().withSubscriber(AssertSubscriber.create(context, 10L));

            sub.assertCompleted().assertItems("bar");
        }

        @Test
        void multiCreateWithContextAndAlienSubscriber() {
            ArrayList<String> items = new ArrayList<>();
            AtomicReference<Throwable> error = new AtomicReference<>(null);
            AtomicBoolean completed = new AtomicBoolean(false);

            Multi.createFrom().context(ctx -> Multi.createFrom().item(ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().withSubscriber(new AlienSubscriber<>(items, error, completed));

            assertThat(completed).isTrue();
            assertThat(error.get()).isNull();
            assertThat(items)
                    .hasSize(1)
                    .contains("yolo");
        }

        @Test
        void multiCreateWithContextMapperIsNull() {
            assertThatThrownBy(() -> Multi.createFrom().context(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("`mapper` must not be `null`");
        }

        @Test
        void multiCreateWithContextMapperReturnsNull() {
            AssertSubscriber<Object> sub = Multi.createFrom().context(ctx -> null)
                    .subscribe().withSubscriber(AssertSubscriber.create(10L));

            sub.assertFailedWith(NullPointerException.class, "The mapper returned `null`");
        }

        @Test
        void multiCreateWithContextMapperThrows() {
            AssertSubscriber<Object> sub = Multi.createFrom().context(ctx -> {
                throw new RuntimeException("Yolo!");
            })
                    .subscribe().withSubscriber(AssertSubscriber.create(10L));

            sub.assertFailedWith(RuntimeException.class, "Yolo!");
        }

        @Test
        void uniCreateWithContext() {
            Context context = Context.of("foo", "bar");

            UniAssertSubscriber<String> sub = Uni.createFrom()
                    .context(ctx -> Uni.createFrom().item(ctx.getOrElse("foo", () -> "yolo")))
                    .subscribe().withSubscriber(UniAssertSubscriber.create(context));

            sub.assertCompleted().assertItem("bar");
        }

        @Test
        void uniCreateWithContextMapperIsNull() {
            assertThatThrownBy(() -> Uni.createFrom().context(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("`mapper` must not be `null`");
        }

        @Test
        void uniCreateWithContextMapperReturnsNull() {
            UniAssertSubscriber<Object> sub = Uni.createFrom().context(ctx -> null)
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            sub.assertFailedWith(NullPointerException.class, "The mapper returned `null`");
        }

        @Test
        void uniCreateWithContextMapperThrows() {
            UniAssertSubscriber<Object> sub = Uni.createFrom().context(ctx -> {
                throw new RuntimeException("Yolo!");
            })
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            sub.assertFailedWith(RuntimeException.class, "Yolo!");
        }

        @Test
        void multiRepeat() {
            Context context = Context.of("foo", "bar");

            AssertSubscriber<String> sub = Multi.createFrom().context(ctx -> Multi.createBy()
                    .repeating().uni(
                            AtomicInteger::new,
                            counter -> Uni.createFrom()
                                    .item(counter.incrementAndGet() + "::" + ctx.getOrElse("foo", () -> "yolo")))
                    .atMost(5))
                    .subscribe().withSubscriber(AssertSubscriber.create(context, Long.MAX_VALUE));

            sub.assertCompleted();
            assertThat(sub.getItems())
                    .hasSize(5)
                    .contains("1::bar", "5::bar");
        }

        @Test
        void multiCombine() {
            Context context = Context.of("foo", "bar");

            Multi<String> a = Multi.createFrom()
                    .context(ctx -> Multi.createFrom().items("a", ctx.getOrElse("foo", () -> "yolo")));
            Multi<String> b = Multi.createFrom()
                    .context(ctx -> Multi.createFrom().items("b", ctx.getOrElse("foo", () -> "yolo")));
            Multi<String> c = Multi.createFrom()
                    .context(ctx -> Multi.createFrom().items("c", ctx.getOrElse("foo", () -> "yolo")));

            AssertSubscriber<String> sub = Multi.createBy().merging().streams(a, b, c)
                    .subscribe().withSubscriber(AssertSubscriber.create(context, Long.MAX_VALUE));

            sub.assertCompleted();
            assertThat(sub.getItems())
                    .hasSize(6)
                    .containsExactly("a", "bar", "b", "bar", "c", "bar");
        }
    }
}

class AlienSubscriber<T> implements Flow.Subscriber<T> {
    private final ArrayList<T> items;
    private final AtomicReference<Throwable> error;
    private final AtomicBoolean completed;

    public AlienSubscriber(ArrayList<T> items, AtomicReference<Throwable> error, AtomicBoolean completed) {
        this.items = items;
        this.error = error;
        this.completed = completed;
    }

    @Override
    public void onSubscribe(Subscription s) {
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T item) {
        items.add(item);
    }

    @Override
    public void onError(Throwable t) {
        error.set(t);
    }

    @Override
    public void onComplete() {
        completed.set(true);
    }
}
