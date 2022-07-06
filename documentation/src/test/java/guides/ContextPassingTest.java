package guides;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContextPassingTest {

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

        // Get an entry, use a supplier for a default value if the key is not present
        System.out.println(
                context.getOrElse("X-SPAN-ID", () -> "<no id>"));

        // Add an entry
        context.put("foo", "bar");

        // Remove an entry
        context.delete("foo");
        // </contextManipulation>
    }

    @Test
    void sampleUsage() {
        Multi<Integer> pipeline = Multi.createFrom().range(1, 10);
        String customerId = "1234";

        // <contextSampleUsage>
        Context context = Context.of("X-CUSTOMER-ID", customerId);

        pipeline.withContext((multi, ctx) -> multi.onItem().transformToUniAndMerge(item -> makeRequest(item, ctx.get("X-CUSTOMER-ID"))))
                .subscribe().with(context, item -> handleResponse(item), err -> handleFailure(err));
        // </contextSampleUsage>
    }

    @Test
    void sampleUsageAttachedContext() {
        Multi<Integer> pipeline = Multi.createFrom().range(1, 10);
        String customerId = "1234";

        // <contextAttachedSampleUsage>
        Context context = Context.of("X-CUSTOMER-ID", customerId);

        pipeline.attachContext()
                .onItem().transformToUniAndMerge(item -> makeRequest(item.get(), item.context().get("X-CUSTOMER-ID")))
                .subscribe().with(context, item -> handleResponse(item), err -> handleFailure(err));
        // </contextAttachedSampleUsage>
    }

    @Test
    void builderUsage() {
        Context context = Context.of("X-SPAN-ID", "1234");

        // <builderUsage>
        Uni.createFrom().context(ctx -> makeRequest("db1", ctx.get("X-SPAN-ID")))
                .subscribe().with(context, item -> handleResponse(item), err -> handleFailure(err));
        // </builderUsage>
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
