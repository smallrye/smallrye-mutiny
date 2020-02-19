package io.smallrye.mutiny.helpers;

import java.util.function.Predicate;

import io.smallrye.mutiny.operators.UniSerializedSubscriber;

public class Predicates {

    private Predicates() {
        // Avoid direct instantiation
    }

    public static <T> boolean testFailure(Predicate<? super Throwable> predicate,
            UniSerializedSubscriber<? super T> subscriber, Throwable failure) {
        if (predicate != null) {
            boolean pass;
            try {
                pass = predicate.test(failure);
            } catch (Throwable e) {
                subscriber.onFailure(e);
                return false;
            }
            if (!pass) {
                subscriber.onFailure(failure);
                return false;
            } else {
                // We pass!
                return true;
            }
        } else {
            return true;
        }
    }
}
