package io.smallrye.mutiny.helpers;

import java.util.function.Predicate;

import io.smallrye.mutiny.Uni;

/**
 * An utility class that creates a predicate on a throwable. If the tested throwable is a runtime exception that has a cause, it unwraps the cause and checks if it is assignable to the cause class
 * provided on predicate creation. It is a companion to the unchecked utilities which wrap checked exceptions into unchecked Runtime exceptions. If the tested throwable has cause, the assignment check
 * will occur on the throwable itself. This class is not meant to be sub-classed so it is declared as final.
 */
public final class Caused implements Predicate<Throwable> {

  private Class<? extends Throwable> cause;

  private Caused(Class<? extends Throwable> cause) {
    this.cause = cause;
  }

  /**
   * Main entry point used to create that predicate
   * 
   * @param cause
   *                the throwable type want to check
   * @return a predicate which can be used in {@link Uni#onFailure()}
   */
  public static Caused by(Class<? extends Throwable> cause) {
    if (cause == null) throw new IllegalArgumentException("You must provide a cause class");
    return new Caused(cause);
  }

  @Override
  public boolean test(Throwable t) {
    if (t.getCause() == null) return cause.isAssignableFrom(t.getClass());
    return cause.isAssignableFrom(t.getCause().getClass());
  }

}
