package junit5.support;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Make sure Awaitility is configured with "sane" defaults.
 */
public class AwaitilityConfigurationExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        Awaitility.catchUncaughtExceptionsByDefault();
        Awaitility.setDefaultPollInterval(Duration.of(200, ChronoUnit.MILLIS));
        Awaitility.setDefaultPollDelay(Duration.of(50, ChronoUnit.MILLIS));
        Awaitility.pollInSameThread();
    }
}
