package org.wasmer;

import org.wasmer.exports.Function;
import org.wasmer.exports.Export;
import java.util.HashMap;
import java.util.Map;

/**
 * `Exports` is a Java class that represents the set of WebAssembly exported function.
 * It's basically a set of `ExportedFunction`.
 *
 * Example:
 * <pre>{@code
 * Instance instance = new Instance(wasmBytes);
 * Object[] result = instance.exports.get("sum").apply(1, 2);
 * }</pre>
 */
public class Exports {
    private Map<String, Export> inner;
    private Instance instance;

    /**
     * Lambda expression for currying.
     * This takes a function name and returns the function to call WebAssembly function.
     */
    private java.util.function.Function<String, Function<Object, Object[]>> functionWrapperGenerator =
        functionName -> arguments -> this.instance.nativeCall(this.instance.instancePointer, functionName, arguments);

    /**
     * The constructor instantiates new exported functions.
     *
     * @param instance Instance object which holds the exports object.
     */
    protected Exports(Instance instance) {
        this.inner = new HashMap<String, Export>();
        this.instance = instance;
    }

    /**
     * Return the exported function at the specified name.
     *
     * @param name Name of the function to return.
     */
    public Export get(String name) {
        return this.inner.get(name);
    }

    public Function<Object, Object[]> getFunction(String name) {
        return (Function<Object, Object[]>) this.inner.get(name);
    }

    public Memory getMemory(String name) {
        return (Memory) this.inner.get(name);
    }

    private Function<Object, Object[]> generateFunctionWrapper(String functionName) {
        return this.functionWrapperGenerator.apply(functionName);
    }

    private void addFunction(String name) {
        this.inner.put(name, this.generateFunctionWrapper(name));
    }

    private void addMemory(String name, Memory memory) {
        this.inner.put(name, memory);
    }
}
