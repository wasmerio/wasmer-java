package org.wasmer;

class Instance {
    private native long nativeInstantiate(Instance self, byte[] moduleBytes) throws RuntimeException;
    private native void nativeDrop(long instancePointer);
    private native Object nativeCall(long instancePointer, String exportName, Object[] arguments) throws RuntimeException;

    private long instancePointer;

    static {
        System.loadLibrary("java_ext_wasm");
    }

    public Instance(byte[] moduleBytes) throws RuntimeException {
        long instancePointer = this.nativeInstantiate(this, moduleBytes);

        this.instancePointer = instancePointer;
    }

    public Object call(String exportName, Object[] arguments) throws RuntimeException {
        return this.nativeCall(this.instancePointer, exportName, arguments);
    }

    public void close() {
        this.nativeDrop(this.instancePointer);
    }

    public void finalize() {
        this.close();
    }
}
