package org.wasmer;

/**
 * Functional interface for WebAssembly exported functions, i.e. it
 * creates a new type for a closure that mimics a WebAssembly exported
 * function.
 *
 * The apply method takes an arbitrary number of arguments and returns
 * an output.
 */
@FunctionalInterface
public interface ExportedFunction<Input, Output> {
    @SuppressWarnings("unchecked")
    public Output apply(Input... args);
}
