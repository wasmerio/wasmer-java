package org.wasmer;

/**
 * `Instance` is a Java class that represents a WebAssembly instance.
 *
 * Example:
 * <pre>{@code
 * Instance instance = new Instance(wasmBytes);
 * }</pre>
 */
public class Instance {
    private native long nativeInstantiate(Instance self, byte[] moduleBytes) throws RuntimeException;
    private native void nativeDrop(long instancePointer);
    protected native Object[] nativeCall(long instancePointer, String exportName, Object[] arguments) throws RuntimeException;
    protected static native void nativeInitializeExportedFunctions(long instancePointer);
    protected static native void nativeInitializeExportedMemories(long instancePointer);

    /**
     * All WebAssembly exported functions.
     */
    public final Exports exports;
    public final Memories memories;
    protected long instancePointer;

    static {
        System.loadLibrary("java_ext_wasm");
    }

    /**
     * The constructor instantiates a new WebAssembly instance based on
     * WebAssembly bytes.
     *
     * @param moduleBytes WebAssembly bytes.
     */
    public Instance(byte[] moduleBytes) throws RuntimeException {
        this.exports = new Exports(this);
        this.memories = new Memories();

        long instancePointer = this.nativeInstantiate(this, moduleBytes);
        this.instancePointer = instancePointer;

        this.nativeInitializeExportedFunctions(instancePointer);
        this.nativeInitializeExportedMemories(instancePointer);
    }

    protected Instance() {
        this.exports = new Exports(this);
        this.memories = new Memories();
    }

    /**
     * Delete an instance object pointer.
     */
    public void close() {
        this.nativeDrop(this.instancePointer);
    }

    /**
     * Delete an instance object pointer, which is called by the garbage collector
     * before an object is removed from the memory.
     */
    public void finalize() {
        this.close();
    }
}
