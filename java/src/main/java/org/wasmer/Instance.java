package org.wasmer;

class Instance {
    public native String hello(String input);
    private String exports = "foo";

    static {
        System.loadLibrary("java_ext_wasm");
    }
}
