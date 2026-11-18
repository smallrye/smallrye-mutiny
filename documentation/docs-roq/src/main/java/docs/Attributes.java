package docs;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkiverse.roq.data.runtime.annotations.DataMapping;

@DataMapping(value = "attributes")
public record Attributes(@JsonProperty("project-version") String projectVersion, Versions versions) {

    public record Versions(String mutiny, @JsonProperty("vertx_bindings") String vertxBindings) {
    }
}
