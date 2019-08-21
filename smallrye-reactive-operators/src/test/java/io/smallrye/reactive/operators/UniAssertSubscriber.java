package io.smallrye.reactive.operators;

import io.smallrye.reactive.subscription.UniSubscriber;
import io.smallrye.reactive.subscription.UniSubscription;

import java.util.concurrent.CompletableFuture;

public class UniAssertSubscriber<T> implements UniSubscriber<T> {
    private final boolean cancelImmediatelyOnSubscription;
    private UniSubscription subscription;
    private boolean gotSignal;
    private T item;
    private Throwable failure;
    private CompletableFuture<T> future = new CompletableFuture<>();
    private String onResultThreadName;
    private String onErrorThreadName;
    private String onSubscribeThreadName;

    public UniAssertSubscriber(boolean cancelled) {
        this.cancelImmediatelyOnSubscription = cancelled;
    }

    public UniAssertSubscriber() {
        this(false);
    }

    public static <T> UniAssertSubscriber<T> create() {
        return new UniAssertSubscriber<>();
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
    public synchronized void onItem(T item) {
        this.gotSignal = true;
        if (this.future == null) {
            throw new IllegalStateException("No subscription");
        }
        this.item = item;
        this.onResultThreadName = Thread.currentThread().getName();
        this.future.complete(item);
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

    public UniAssertSubscriber<T> await() {
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

    public synchronized UniAssertSubscriber<T> assertCompletedSuccessfully() {
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

    public synchronized UniAssertSubscriber<T> assertCompletedWithFailure() {
        if (this.future == null) {
            throw new IllegalStateException("No subscription");
        }
        if (!this.future.isDone()) {
            throw new IllegalStateException("Not done yet");
        }

        if (! future.isCompletedExceptionally()) {
            throw new AssertionError("The uni completed successfully: " + item);
        }
        if (future.isCancelled()) {
            throw new AssertionError("The uni didn't completed successfully, it was cancelled");
        }
        return this;
    }

    public synchronized T getItem() {
        if (this.future == null) {
            throw new IllegalStateException("No subscription");
        }
        if (!this.future.isDone()) {
            throw new IllegalStateException("Not done yet");
        }
        return item;
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

    public UniAssertSubscriber<T> assertItem(T expected) {
        T item = getItem();
        if (item == null && expected != null) {
            throw new AssertionError("Expected: " + expected + " but was `null`");
        }
        if (item != null && !item.equals(expected)) {
            throw new AssertionError("Expected: " + expected + " but was " + item);
        }
        return this;
    }

    public UniAssertSubscriber<T> assertFailure(Class<? extends Throwable> exceptionClass, String message) {
        Throwable failure = getFailure();

        if (failure == null) {
            throw new AssertionError("Expected a failure, but the Uni completed with an item");
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

    public UniAssertSubscriber<T> assertNotCompleted() {
        if (this.future == null) {
            throw new IllegalStateException("No subscription");
        }
        if (! gotSignal) {
            return this;
        } else {
            throw new AssertionError("The uni completed");
        }
    }

    public UniAssertSubscriber<T> assertNoResult() {
        if(gotSignal  && failure == null) {
            throw new AssertionError("The Uni got a signal");
        }
        return this;
    }

    public UniAssertSubscriber<T> assertNoFailure() {
        if (failure != null) {
            throw new AssertionError("Expected to not have an error, but found " + failure);
        }
        return this;
    }


    public UniAssertSubscriber<T> assertSubscribed() {
        if (subscription == null) {
            throw new AssertionError(("Expected to have a subscription"));
        }
        return this;
    }

    public UniAssertSubscriber<T> assertNoSignals() {
        if (gotSignal) {
            throw new AssertionError("The Uni got a signal");
        }
        return this;
    }
}
