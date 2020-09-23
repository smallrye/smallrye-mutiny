package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Uni;

public interface SpyUni {

    static <T> UniOnSubscribeSpy<T> onSubscribe(Uni<T> upstream) {
        return (UniOnSubscribeSpy<T>) upstream.plug(uni -> new UniOnSubscribeSpy<>(upstream));
    }

}
