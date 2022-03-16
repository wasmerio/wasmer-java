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
    /**
     * Native bindings.
     */
    static {
        if (!Native.LOADED_EMBEDDED_LIBRARY) {
            System.loadLibrary("wasmer_jni");
        }
    }
    private native long nativeInstantiate(Instance self, byte[] moduleBytes) throws RuntimeException;
    private native void nativeDrop(long instancePointer);
    protected native Object[] nativeCallExportedFunction(long instancePointer, String exportName, Object[] arguments) throws RuntimeException;
    protected static native void nativeInitializeExportedFunctions(long instancePointer);
    protected static native void nativeInitializeExportedMemories(long instancePointer);

    /**
     * All WebAssembly exports.
     */
    public final Exports exports;

    /**
       The instance pointer.
     */
    protected long instancePointer;

    /**
     * The constructor instantiates a new WebAssembly instance based on
     * WebAssembly bytes.
     *
     * @param moduleBytes WebAssembly bytes.
     */
    public Instance(byte[] moduleBytes) throws RuntimeException {
        this.exports = new Exports(this);

        long instancePointer = this.nativeInstantiate(this, moduleBytes);
        this.instancePointer = instancePointer;

        this.nativeInitializeExportedFunctions(instancePointer);
        this.nativeInitializeExportedMemories(instancePointer);
    }

    protected Instance() {
        this.exports = new Exports(this);
    }

    /**
     * Delete an instance object pointer.
     */
    public void close() {
        // To avoid duplicate native dropping
        if(this.instancePointer != 0l) {
            this.nativeDrop(this.instancePointer);
            this.instancePointer = 0l;
        }
    }

    /**
     * Delete an instance object pointer, which is called by the garbage collector
     * before an object is removed from the memory.
     */
    public void finalize() {
        this.close();
    }
}
