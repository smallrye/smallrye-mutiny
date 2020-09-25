package io.smallrye.mutiny.helpers.spies;

import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.groups.MultiOnFailure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnFailureSpy<T> extends MultiSpyBase<T> {

    private Predicate<? super Throwable> predicate;
    private Class<? extends Throwable> typeOfFailure;

    private volatile Throwable lastFailure;

    public Throwable lastFailure() {
        return lastFailure;
    }

    MultiOnFailureSpy(Multi<? extends T> upstream) {
        super(upstream);
    }

    MultiOnFailureSpy(Multi<? extends T> upstream, Predicate<? super Throwable> predicate) {
        super(upstream);
        this.predicate = predicate;
    }

    MultiOnFailureSpy(Multi<? extends T> upstream, Class<? extends Throwable> typeOfFailure) {
        super(upstream);
        this.typeOfFailure = typeOfFailure;
    }

    @Override
    public void reset() {
        super.reset();
        lastFailure = null;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> dowstream) {
        MultiOnFailure<? extends T> group;
        if (predicate != null) {
            group = upstream.onFailure(predicate);
        } else if (typeOfFailure != null) {
            group = upstream.onFailure(typeOfFailure);
        } else {
            group = upstream.onFailure();
        }
        group.invoke(failure -> {
            incrementInvocationCount();
            lastFailure = failure;
        }).subscribe().withSubscriber(dowstream);
    }

    @Override
    public String toString() {
        return "MultiOnFailureSpy{" +
                "lastFailure=" + lastFailure +
                "} " + super.toString();
    }
}
