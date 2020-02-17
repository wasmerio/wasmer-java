package org.wasmer;

class Module {
    private native long nativeModuleInstantiate(Module self, byte[] moduleBytes) throws RuntimeException;
    private native void nativeDrop(long modulePointer);
    private native long nativeInstantiate(long modulePointer, Instance instance);
    private static native boolean nativeValidate(byte[] moduleBytes);
    private native byte[] nativeSerialize(long modulePointer);
    private static native long nativeDeserialize(Module module, byte[] serializedBytes);

    private long modulePointer;

    static {
        System.loadLibrary("java_ext_wasm");
    }

    public static boolean validate(byte[] moduleBytes) {
        return Module.nativeValidate(moduleBytes);
    }

    public Module(byte[] moduleBytes) throws RuntimeException {
        long modulePointer = this.nativeModuleInstantiate(this, moduleBytes);
        this.modulePointer = modulePointer;
    }

    private Module() {
    }

    public void close() {
        this.nativeDrop(this.modulePointer);
    }

    public void finalize() {
        this.close();
    }

    public Instance instantiate() {
        Instance instance = new Instance();
        long instancePointer = this.nativeInstantiate(this.modulePointer, instance);
        instance.instancePointer = instancePointer;

        instance.nativeInitializeExportedFunctions(instancePointer);
        instance.nativeInitializeExportedMemories(instancePointer);
        return instance;
    }

    public byte[] serialize() {
        return this.nativeSerialize(this.modulePointer);
    }

    public static Module deserialize(byte[] serializedBytes) {
        Module module = new Module();
        long modulePointer = Module.nativeDeserialize(module, serializedBytes);
        module.modulePointer = modulePointer;
        return module;
    }
}
