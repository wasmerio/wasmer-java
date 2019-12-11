package org.wasmer;

class Instance {
    private native long instantiate(Instance self, byte[] moduleBytes) throws RuntimeException;
    private native void drop(long instancePointer);
    private native int dynCall(long instancePointer, String exportName, int[] arguments) throws RuntimeException;

    private long instancePointer;

    static {
        System.loadLibrary("java_ext_wasm");
    }

    public Instance(byte[] moduleBytes) throws RuntimeException {
        long instancePointer = this.instantiate(this, moduleBytes);

        this.instancePointer = instancePointer;
    }

    public int call(String exportName, int[] arguments) throws RuntimeException {
        return this.dynCall(this.instancePointer, exportName, arguments);
    }

    public void close() {
        this.drop(this.instancePointer);
    }

    public void finalize() {
        this.close();
    }
}
