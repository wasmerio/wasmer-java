package org.wasmer;

/**
 * `Module` is a Java class that represents a WebAssembly module.
 *
 * # Examples
 *
 * ```java
 * boolean isValid = Module.validate(wasmBytes);
 *
 * Module module = new Module(wasmBytes);
 * Instance instance = module.instantiate();
 * ```
 */
class Module {
    private native long nativeModuleInstantiate(Module self, byte[] moduleBytes) throws RuntimeException;
    private native void nativeDrop(long modulePointer);
    private native long nativeInstantiate(long modulePointer, Instance instance);
    private static native boolean nativeValidate(byte[] moduleBytes);

    private long modulePointer;

    static {
        System.loadLibrary("java_ext_wasm");
    }

    /**
     * Check that given bytes represent a valid WebAssembly module.
     *
     * @param moduleBytes WebAssembly bytes.
     * @return true if, and only if, given bytes are valid as a WebAssembly module.
     */
    public static boolean validate(byte[] moduleBytes) {
        return Module.nativeValidate(moduleBytes);
    }

    /**
     * The constructor instantiates a new WebAssembly module based on
     * WebAssembly bytes.
     *
     * @param moduleBytes WebAssembly bytes.
     */
    public Module(byte[] moduleBytes) throws RuntimeException {
        long modulePointer = this.nativeModuleInstantiate(this, moduleBytes);
        this.modulePointer = modulePointer;
    }

    /**
     * Delete a module object pointer.
     */
    public void close() {
        this.nativeDrop(this.modulePointer);
    }

    /**
     * Delete a module object pointer, which is called by the garbage collector
     * before an object is removed from the memory.
     */
    public void finalize() {
        this.close();
    }

    /**
     * Create an instance object based on a module object.
     *
     * @return Instance object.
     */
    public Instance instantiate() {
        Instance instance = new Instance();
        long instancePointer = this.nativeInstantiate(this.modulePointer, instance);
        instance.instancePointer = instancePointer;
        instance.nativeInitializeExportedFunctions(instancePointer);
        return instance;
    }
}
