package org.wasmer;

class Module {
    private native long nativeModuleInstantiate(Module self, byte[] moduleBytes) throws RuntimeException;
    private native void nativeDrop(long modulePointer);
    private static native boolean nativeValidate(byte[] moduleBytes);

    private byte[] moduleBytes;
    private long modulePointer;

    static {
        System.loadLibrary("java_ext_wasm");
    }

    public static boolean validate(byte[] moduleBytes) {
        return Module.nativeValidate(moduleBytes);
    }

    public Module(byte[] moduleBytes) throws RuntimeException {
        long modulePointer = this.nativeModuleInstantiate(this, moduleBytes);
        this.moduleBytes = moduleBytes;
        this.modulePointer = modulePointer;
    }

    public void close() {
        this.nativeDrop(this.modulePointer);
    }

    public Instance instantiate() {
        return new Instance(this.moduleBytes);
    }
}
