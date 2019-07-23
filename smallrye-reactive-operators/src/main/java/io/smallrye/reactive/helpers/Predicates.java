package io.smallrye.reactive.helpers;

import io.smallrye.reactive.operators.UniSerializedSubscriber;

import java.util.function.Predicate;

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
            } catch (Exception e) {
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
