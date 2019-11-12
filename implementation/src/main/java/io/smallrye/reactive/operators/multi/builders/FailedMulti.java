package io.smallrye.reactive.operators.multi.builders;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.helpers.Subscriptions;
import io.smallrye.reactive.operators.AbstractMulti;

/**
 * Multi emitting a failures (constant or produced by a supplier) to subscribers.
 *
 * @param <T> the value type
 */
public class FailedMulti<T> extends AbstractMulti<T> {

    private final Supplier<Throwable> supplier;

    public FailedMulti(Throwable failure) {
        ParameterValidation.nonNull(failure, "failure");
        this.supplier = () -> failure;
    }

    public FailedMulti(Supplier<Throwable> supplier) {
        ParameterValidation.nonNull(supplier, "supplier");
        this.supplier = supplier;
    }

    @Override
    protected Publisher<T> publisher() {
        return this;
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        try {
            Throwable throwable = supplier.get();
            if (throwable == null) {
                Subscriptions.fail(actual, new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL));
            } else {
                Subscriptions.fail(actual, throwable);
            }
        } catch (Exception e) {
            Subscriptions.fail(actual, e);
        }

    }

}
