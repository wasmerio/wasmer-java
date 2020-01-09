package org.wasmer;

import java.util.function.Function;
import java.util.HashMap;
import java.util.Map;

@FunctionalInterface
interface WasmFunction<T, R> {
    R apply(T... args);
}

class Instance {
    private native long nativeInstantiate(Instance self, byte[] moduleBytes) throws RuntimeException;
    private native void nativeDrop(long instancePointer);
    private native Object[] nativeCall(long instancePointer, String exportName, Object[] arguments) throws RuntimeException;

    public Map<String, WasmFunction<Object, Object[]>> exports = new HashMap<>();
    private long instancePointer;

    static {
        System.loadLibrary("java_ext_wasm");
    }

    public Instance(byte[] moduleBytes) throws RuntimeException {
        long instancePointer = this.nativeInstantiate(this, moduleBytes);

        this.instancePointer = instancePointer;
    }

    private Function<String, WasmFunction<Object, Object[]>> baseFunction =
        exportName -> arguments -> this.nativeCall(this.instancePointer, exportName, arguments);

    private WasmFunction<Object, Object[]> wasmFunction(String exportName) {
        return baseFunction.apply(exportName);
    }

    private void addExportFunction(String name) {
        this.exports.put(name, this.wasmFunction(name));
    }

    public void close() {
        this.nativeDrop(this.instancePointer);
    }

    public void finalize() {
        this.close();
    }
}
