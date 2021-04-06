package mutiny.zero;

public enum BackpressureStrategy {
    BUFFER,
    UNBOUNDED_BUFFER,
    DROP,
    ERROR,
    IGNORE,
    LATEST
}
