package org.wasmer;

class Instance {
    private native long nativeInstantiate(Instance self, byte[] moduleBytes) throws RuntimeException;
    private native void nativeDrop(long instancePointer);
    protected native Object[] nativeCall(long instancePointer, String exportName, Object[] arguments) throws RuntimeException;
    private static native void nativeInitializeExportedFunctions(long instancePointer);
    private static native void nativeInitializeExportedMemories(long instancePointer);

    public final Exports exports;
    public final Memories memories;
    protected long instancePointer;

    static {
        System.loadLibrary("java_ext_wasm");
    }

    public Instance(byte[] moduleBytes) throws RuntimeException {
        this.exports = new Exports(this);
        this.memories = new Memories();

        long instancePointer = this.nativeInstantiate(this, moduleBytes);
        this.instancePointer = instancePointer;

        this.nativeInitializeExportedFunctions(instancePointer);
        this.nativeInitializeExportedMemories(instancePointer);
    }

    public void close() {
        this.nativeDrop(this.instancePointer);
    }

    public void finalize() {
        this.close();
    }
}
