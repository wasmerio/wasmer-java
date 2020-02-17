package org.wasmer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * `Exports` is a Java class that represents the set of WebAssembly exported function.
 * It's basically a set of `ExportedFunction`.
 *
 * Examples:
 * <pre>{@code
 * Instance instance = new Instance(wasmBytes);
 * Object[] result = instance.exports.get("sum").apply(1, 2);
 * }</pre>
 */
class Exports {
    private Map<String, ExportedFunction<Object, Object[]>> inner;
    private Instance instance;

    /**
     * Lambda expression for currying.
     * This takes a function name and returns the function to call WebAssembly function.
     */
    private Function<String, ExportedFunction<Object, Object[]>> exportedFunctionWrapperGenerator =
        functionName -> arguments -> this.instance.nativeCall(this.instance.instancePointer, functionName, arguments);

    /**
     * The constructor instantiates new exported functions.
     *
     * @param instance Instance object which holds the exports object.
     */
    public Exports(Instance instance) {
        this.inner = new HashMap<String, ExportedFunction<Object, Object[]>>();
        this.instance = instance;
    }

    /**
     * Return the exported function at the specified name.
     *
     * @param name Name of the function to return.
     */
    public ExportedFunction<Object, Object[]> get(String name) {
        return this.inner.get(name);
    }

    private ExportedFunction<Object, Object[]> generateExportedFunctionWrapper(String functionName) {
        return this.exportedFunctionWrapperGenerator.apply(functionName);
    }

    private void addExportedFunction(String name) {
        this.inner.put(name, this.generateExportedFunctionWrapper(name));
    }
}
