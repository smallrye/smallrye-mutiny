package io.smallrye.reactive.operators;

import io.smallrye.reactive.subscription.UniSubscriber;
import io.smallrye.reactive.subscription.UniSubscription;

import java.util.concurrent.CompletableFuture;

public class AssertSubscriber<T> implements UniSubscriber<T> {
    private final boolean cancelImmediatelyOnSubscription;
    private UniSubscription subscription;
    private boolean gotSignal;
    private T result;
    private Throwable failure;
    private CompletableFuture<T> future = new CompletableFuture<>();
    private String onResultThreadName;
    private String onErrorThreadName;
    private String onSubscribeThreadName;

    public AssertSubscriber(boolean cancelled) {
        this.cancelImmediatelyOnSubscription = cancelled;
    }

    public AssertSubscriber() {
        this(false);
    }

    public static <T> AssertSubscriber<T> create() {
        return new AssertSubscriber<>();
    }


    @Override
    public synchronized void onSubscribe(UniSubscription subscription) {
        onSubscribeThreadName = Thread.currentThread().getName();
        if (this.cancelImmediatelyOnSubscription) {
            this.subscription = subscription;
            subscription.cancel();
            future.cancel(false);
            return;
        }
        this.subscription = subscription;
    }

    @Override
    public synchronized void onResult(T result) {
        this.gotSignal = true;
        if (this.future == null) {
            throw new IllegalStateException("No subscription");
        }
        this.result = result;
        this.onResultThreadName = Thread.currentThread().getName();
        this.future.complete(result);
    }

    @Override
    public synchronized void onFailure(Throwable failure) {
        this.gotSignal = true;
        if (this.future == null) {
            throw new IllegalStateException("No subscription");
        }
        this.failure = failure;
        this.onErrorThreadName = Thread.currentThread().getName();
        this.future.completeExceptionally(failure);
    }

    public AssertSubscriber<T> await() {
        CompletableFuture<T> fut;
        synchronized (this) {
            if (this.future == null) {
                throw new IllegalStateException("No subscription");
            }
            fut = this.future;
        }
        try {
            fut.join();
        } catch (Exception e) {
            // Error already caught.
        }
        return this;
    }

    public synchronized AssertSubscriber<T> assertCompletedSuccessfully() {
        if (this.future == null) {
            throw new IllegalStateException("No subscription");
        }
        if (!this.future.isDone()) {
            throw new IllegalStateException("Not done yet");
        }

        if (future.isCompletedExceptionally()) {
            throw new AssertionError("The uni didn't completed successfully: " + failure);
        }
        if (future.isCancelled()) {
            throw new AssertionError("The uni didn't completed successfully, it was cancelled");
        }
        return this;
    }

    public synchronized AssertSubscriber<T> assertCompletedWithFailure() {
        if (this.future == null) {
            throw new IllegalStateException("No subscription");
        }
        if (!this.future.isDone()) {
            throw new IllegalStateException("Not done yet");
        }

        if (! future.isCompletedExceptionally()) {
            throw new AssertionError("The uni completed successfully: " + result);
        }
        if (future.isCancelled()) {
            throw new AssertionError("The uni didn't completed successfully, it was cancelled");
        }
        return this;
    }

    public synchronized T getResult() {
        if (this.future == null) {
            throw new IllegalStateException("No subscription");
        }
        if (!this.future.isDone()) {
            throw new IllegalStateException("Not done yet");
        }
        return result;
    }

    public synchronized Throwable getFailure() {
        if (this.future == null) {
            throw new IllegalStateException("No subscription");
        }
        if (!this.future.isDone()) {
            throw new IllegalStateException("Not done yet");
        }
        return failure;
    }

    public AssertSubscriber<T> assertResult(T expected) {
        T result = getResult();
        if (result == null && expected != null) {
            throw new AssertionError("Expected: " + expected + " but was `null`");
        }
        if (result != null && !result.equals(expected)) {
            throw new AssertionError("Expected: " + expected + " but was " + result);
        }
        return this;
    }

    public AssertSubscriber<T> assertFailure(Class<? extends Throwable> exceptionClass, String message) {
        Throwable failure = getFailure();

        if (failure == null) {
            throw new AssertionError("Expected a failure, but the Uni completed with a result");
        }
        if (!exceptionClass.isInstance(failure)) {
            throw new AssertionError("Expected a failure of type " + exceptionClass + ", but it was a " + failure.getClass());
        }
        if (!failure.getMessage().contains(message)) {
            throw new AssertionError("Expected a failure with a message containing '" + message + "', but it was '" + failure.getMessage() + "'");
        }
        return this;
    }

    public String getOnResultThreadName() {
        return onResultThreadName;
    }

    public String getOnFailureThreadName() {
        return onErrorThreadName;
    }

    public String getOnSubscribeThreadName() {
        return onSubscribeThreadName;
    }

    public void cancel() {
        this.subscription.cancel();
    }

    public AssertSubscriber<T> assertNotCompleted() {
        if (this.future == null) {
            throw new IllegalStateException("No subscription");
        }
        if (! gotSignal) {
            return this;
        } else {
            throw new AssertionError("The uni completed");
        }
    }

    public AssertSubscriber<T> assertNoResult() {
        if(gotSignal  && failure == null) {
            throw new AssertionError("The Uni got a signal");
        }
        return this;
    }

    public AssertSubscriber<T> assertNoFailure() {
        if (failure != null) {
            throw new AssertionError("Expected to not have an error, but found " + failure);
        }
        return this;
    }


    public AssertSubscriber<T> assertSubscribed() {
        if (subscription == null) {
            throw new AssertionError(("Expected to have a subscription"));
        }
        return this;
    }

    public AssertSubscriber<T> assertNoSignals() {
        if (gotSignal) {
            throw new AssertionError("The Uni got a signal");
        }
        return this;
    }
}
