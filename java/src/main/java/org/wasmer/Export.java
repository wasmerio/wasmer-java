package org.wasmer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Functional interface for WebAssembly exported functions, i.e. it
 * creates a new type for a closure that mimics a WebAssembly exported
 * function.
 *
 * The apply method takes an arbitrary number of arguments and returns
 * an output.
 */
@FunctionalInterface
interface ExportedFunction<Input, Output> {
    @SuppressWarnings("unchecked")
    Output apply(Input... args);
}

class Export {
    private Map<String, ExportedFunction<Object, Object[]>> inner;
    private Instance instance;

    /**
     * Lambda expression for currying.
     * This takes a function name and returns the function to call WebAssembly function.
     */
    private Function<String, ExportedFunction<Object, Object[]>> exportedFunctionWrapperGenerator =
        functionName -> arguments -> this.instance.nativeCall(this.instance.instancePointer, functionName, arguments);

    public Export(Instance instance) {
        this.inner = new HashMap<String, ExportedFunction<Object, Object[]>>();
        this.instance = instance;
    }

    private ExportedFunction<Object, Object[]> generateExportedFunctionWrapper(String functionName) {
        return this.exportedFunctionWrapperGenerator.apply(functionName);
    }

    private void addExportedFunction(String name) {
        this.inner.put(name, this.generateExportedFunctionWrapper(name));
    }

    public ExportedFunction<Object, Object[]> get(String name) {
        return this.inner.get(name);
    }
}
