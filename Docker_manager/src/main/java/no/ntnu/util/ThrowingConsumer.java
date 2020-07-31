package no.ntnu.util;


/**
 * Represents an operation that accepts a single input argument and returns no
 * result. capable of throwing exceptions
 * @param <T> the type of the input to the operation
 * @param <E> the type of the checked exceptions to catch
 */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception>{
    void accept(T t) throws E;
}
