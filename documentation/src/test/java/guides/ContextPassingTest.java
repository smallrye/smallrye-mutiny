package guides;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class ContextPassingTest {

    void contextCreation() {

        // <contextCreation>
        // Create a context using key / value pairs
        Context context = Context.of(
                "X-CUSTOMER-ID", "1234",
                "X-SPAN-ID", "foo-bar-baz"
        );

        // Create an empty context
        Context.empty();

        // Create a context from a Map
        Map<String, String> map = Map.of(
                "X-CUSTOMER-ID", "1234",
                "X-SPAN-ID", "foo-bar-baz");
        Context.from(map);
        // </contextCreation>
    }

    void contextManipulation() {

        // <contextManipulation>
        // Create a context using key / value pairs
        Context context = Context.of(
                "X-CUSTOMER-ID", "1234",
                "X-SPAN-ID", "foo-bar-baz"
        );

        // Get an entry
        System.out.println(
                context.<String>get("X-SPAN-ID"));

        // Get an entry, use a supplier for a default value
        // if the key is not present
        System.out.println(
                context.getOrElse("X-SPAN-ID", () -> "<no id>"));

        // Add an entry
        context.put("foo", "bar");

        // Remove an entry
        context.delete("foo");
        // </contextManipulation>
    }

    @Test
    void contextAttach() {
        Uni<String> uni = Uni.createFrom().item("foo")
                .withContext((uni1, ctx) -> uni1.replaceWith((String) ctx.get("X-CUSTOMER-ID")));
        String customerId = "1234";

        // <contextAttach>
        Context context = Context.of("X-CUSTOMER-ID", customerId);

        // This is how you can attach a context to your pipeline
        uni.subscribe()
           .with(context,
                 item -> handleResponse(item),
                 err -> handleFailure(err));
        // </contextAttach>
    }

    @Test
    void contextModify() {
        Uni<String> uni = Uni.createFrom().item("foo")
                .withContext((uni1, ctx) -> uni1.replaceWith((String) ctx.get("X-CUSTOMER-ID")));
        String customerId = "1234";

        // <contextModify>
        Context newContext = Context.of("X-CUSTOMER-ID", customerId);

        // Let's add a context to our original Uni
        // by creating an intermediate subscription
        Uni<String> uniWithContext = Uni.createFrom().emitter(emitter -> {
            uni.subscribe().with(newContext, emitter::complete, emitter::fail);
        });

        // Now it does not matter what the initial context is
        // (empty in this case since we are not providing it)
        uniWithContext.subscribe()
                .with(item -> handleResponse(item),
                      err -> handleFailure(err));
        // </contextModify>
    }

    @Test
    void sampleUsage() {
        Uni<String> uni = Uni.createFrom().item("foo");
        String customerId = "1234";

        // <contextSampleUsage>
        Context context = Context.of("X-CUSTOMER-ID", customerId);

        uni.withContext(
            (originalUni, ctx)
              -> originalUni.chain(item -> makeRequest(item,
                                                       ctx.get("X-CUSTOMER-ID"))))
        // </contextSampleUsage>
                .subscribe().with(context, item -> handleResponse(item), err -> handleFailure(err));
    }

    @Test
    void sampleUsageAttachedContext() {
        Uni<String> uni = Uni.createFrom().item("foo");
        String customerId = "1234";

        // <contextAttachedSampleUsage>
        Context context = Context.of("X-CUSTOMER-ID", customerId);

        uni.attachContext()
                .chain(item -> makeRequest(item.get(),
                                           item.context().get("X-CUSTOMER-ID")))
        // </contextAttachedSampleUsage>
                .subscribe().with(context, item -> handleResponse(item), err -> handleFailure(err));
    }

    @Test
    void builderUsage() {
        Context context = Context.of("X-SPAN-ID", "1234");

        // <builderUsage>
        Uni.createFrom().context(ctx -> makeRequest("db1", ctx.get("X-SPAN-ID")))
        // </builderUsage>
                .subscribe().with(context, item -> handleResponse(item), err -> handleFailure(err));
    }

    @Test
    void emitterUsage() {
        Context context = Context.of("X-SPAN-ID", "1234");

        // <emitterUsage>
        Multi.createFrom().emitter(emitter -> {
            String customerId = emitter.context().get("X-SPAN-ID");
            for (int i = 0; i < 10; i++) {
                emitter.emit("@" + i + " [" + customerId + "]");
            }
            emitter.complete();
        });
        // </emitterUsage>
    }

    private void handleFailure(Throwable err) {
        Assertions.fail(err);
    }

    private void handleResponse(String item) {
        Assertions.assertTrue(item.endsWith("::1234"));
    }

    private Uni<String> makeRequest(Object item, Object o) {
        return Uni.createFrom().item(item + "::" + o);
    }
}
