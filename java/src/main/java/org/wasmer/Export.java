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
    Output apply(Input... args);
}

class Export {
    private Map<String, ExportedFunction<Object, Object[]>> functionMap;
    private Instance instance;

    public Export(Instance instance) {
        this.functionMap = new HashMap();
        this.instance = instance;
    }

    /**
     * Lambda expression for currying.
     * This takes a function name and returns the function to call WebAssembly function.
     */
    private Function<String, ExportedFunction<Object, Object[]>> baseFunction =
        functionName -> arguments -> this.instance.nativeCall(this.instance.instancePointer, functionName, arguments);

    private ExportedFunction<Object, Object[]> getWasmFunction(String functionName) {
        return baseFunction.apply(functionName);
    }

    private void addExportedFunction(String name) {
        this.functionMap.put(name, this.getWasmFunction(name));
    }

    private void addExportedFunctions(String[] names) {
        for (String name: names) {
            this.functionMap.put(name, this.getWasmFunction(name));
        }
    }

    public ExportedFunction<Object, Object[]> get(String name) {
        return this.functionMap.get(name);
    }
}
