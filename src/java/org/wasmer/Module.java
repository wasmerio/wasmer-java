package org.wasmer;

import java.util.Collections;

/**
 * `Module` is a Java class that represents a WebAssembly module.
 *
 * Example:
 * <pre>{@code
 * boolean isValid = Module.validate(wasmBytes);
 *
 * Module module = new Module(wasmBytes);
 * Instance instance = module.instantiate();
 * }</pre>
 */
public class Module {
    /**
     * Native bindings.
     */
    static {
        if (!Native.LOADED_EMBEDDED_LIBRARY) {
            System.loadLibrary("wasmer_jni");
        }
    }
    private native long nativeModuleInstantiate(Module self, byte[] moduleBytes) throws RuntimeException;
    private native void nativeDrop(long modulePointer);
    private native long nativeInstantiate(long modulePointer, Instance instance, long importsPointer);
    private static native boolean nativeValidate(byte[] moduleBytes);
    private native byte[] nativeSerialize(long modulePointer);
    private static native long nativeDeserialize(Module module, byte[] serializedBytes);

    protected long modulePointer;


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

    private Module() {}

    /**
     * Delete a module object pointer.
     */
    public void close() {
        // To avoid duplicate native dropping
        if (this.modulePointer != 0l) {
            this.nativeDrop(this.modulePointer);
            this.modulePointer = 0l;
        }
    }

    /**
     * Delete a module object pointer, which is called by the garbage collector
     * before an object is removed from the memory.
     */
    public void finalize() {
        this.close();
    }

    public Instance instantiate() {
        return instantiate(Imports.from(Collections.emptyList(), this));
    }
    /**
     * Create an instance object based on a module object.
     *
     * @return Instance object.
     */
    public Instance instantiate(Imports imports) {
        Instance instance = new Instance();

        long instancePointer = this.nativeInstantiate(this.modulePointer, instance, imports.importsPointer);
        instance.instancePointer = instancePointer;

        instance.nativeInitializeExportedFunctions(instancePointer);
        instance.nativeInitializeExportedMemories(instancePointer);
        return instance;
    }

    /**
     * Create a serialized byte array from a WebAssembly module.
     *
     * @return Serialized bytes.
     */
    public byte[] serialize() {
        return this.nativeSerialize(this.modulePointer);
    }

    /**
     * Create an original Module object from a byte array.
     *
     * @return Module object.
     */
    public static Module deserialize(byte[] serializedBytes) {
        Module module = new Module();
        long modulePointer = Module.nativeDeserialize(module, serializedBytes);
        module.modulePointer = modulePointer;
        return module;
    }
}
