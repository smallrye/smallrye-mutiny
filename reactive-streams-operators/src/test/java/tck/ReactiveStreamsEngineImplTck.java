package tck;

import org.eclipse.microprofile.reactive.streams.operators.tck.ReactiveStreamsTck;
import org.reactivestreams.tck.TestEnvironment;

import io.smallrye.mutiny.streams.Engine;

/**
 * Executes the TCK again the implementation.
 */
public class ReactiveStreamsEngineImplTck extends ReactiveStreamsTck<Engine> {

    static TestEnvironment ENV = new TestEnvironment();

    public ReactiveStreamsEngineImplTck() {
        super(ENV);
    }

    @Override
    protected Engine createEngine() {
        return new Engine();
    }

}
