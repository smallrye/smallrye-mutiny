package io.smallrye.mutiny.helpers.spies;

import java.util.function.Predicate;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniOnFailure;
import io.smallrye.mutiny.operators.UniSerializedSubscriber;

public class UniOnFailureSpy<T> extends UniSpyBase<T> {

    private Predicate<? super Throwable> predicate;
    private Class<? extends Throwable> typeOfFailure;

    private volatile Throwable lastFailure;

    public Throwable lastFailure() {
        return lastFailure;
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
    protected void subscribing(UniSerializedSubscriber<? super T> downstream) {
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
}
