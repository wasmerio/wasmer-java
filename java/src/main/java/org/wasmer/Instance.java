package org.wasmer;

class Instance {
    private native long nativeInstantiate(Instance self, byte[] moduleBytes) throws RuntimeException;
    private native void nativeDrop(long instancePointer);
    protected native Object[] nativeCall(long instancePointer, String exportName, Object[] arguments) throws RuntimeException;
    private static native void nativeInitializeExportedMemories(long instancePointer);

    public final Export exports;
    public Memories memories;
    protected long instancePointer;

    static {
        System.loadLibrary("java_ext_wasm");
    }

    public Instance(byte[] moduleBytes) throws RuntimeException {
        // Should make an export object and set it up to exports field
        // before pass an instance object to Rust. Otherwise, the exports field is null.
        this.exports = new Export(this);
        this.memories = new Memories();

        long instancePointer = this.nativeInstantiate(this, moduleBytes);
        this.instancePointer = instancePointer;

        this.nativeInitializeExportedMemories(instancePointer);
    }

    public void close() {
        this.nativeDrop(this.instancePointer);
    }

    public void finalize() {
        this.close();
    }
}
