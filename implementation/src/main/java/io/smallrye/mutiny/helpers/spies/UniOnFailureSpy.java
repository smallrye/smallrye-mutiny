package io.smallrye.mutiny.helpers.spies;

import java.util.function.Predicate;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniOnFailure;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniOnFailureSpy<T> extends UniSpyBase<T> {

    private Predicate<? super Throwable> predicate;
    private Class<? extends Throwable> typeOfFailure;

    private volatile Throwable lastFailure;

    public Throwable lastFailure() {
        return lastFailure;
    }

    @Override
    public void reset() {
        super.reset();
        lastFailure = null;
    }

    UniOnFailureSpy(Uni<T> upstream) {
        super(upstream);
    }

    UniOnFailureSpy(Uni<T> upstream, Predicate<? super Throwable> predicate) {
        super(upstream);
        this.predicate = predicate;
    }

    UniOnFailureSpy(Uni<T> upstream, Class<? extends Throwable> typeOfFailure) {
        super(upstream);
        this.typeOfFailure = typeOfFailure;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> downstream) {
        UniOnFailure<? extends T> group;
        if (predicate != null) {
            group = upstream().onFailure(predicate);
        } else if (typeOfFailure != null) {
            group = upstream().onFailure(typeOfFailure);
        } else {
            group = upstream().onFailure();
        }
        group.invoke(failure -> {
            incrementInvocationCount();
            lastFailure = failure;
        }).subscribe().withSubscriber(downstream);
    }

    @Override
    public String toString() {
        return "UniOnFailureSpy{" +
                "lastFailure=" + lastFailure +
                "} " + super.toString();
    }
}
