package io.smallrye.mutiny.helpers;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
/**
 * Marker for methods whose return values shall not be ignored under common API usage patterns.
 */
public @interface CheckReturnValue {
}
