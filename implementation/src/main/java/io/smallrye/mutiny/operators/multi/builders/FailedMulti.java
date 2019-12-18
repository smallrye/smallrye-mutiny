package io.smallrye.mutiny.operators.multi.builders;

import java.util.function.Supplier;

import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;

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
