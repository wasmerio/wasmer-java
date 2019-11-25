package org.wasmer;

class Instance {
    private native String hello(String input);
    private String exports = "foo";

    static {
        System.loadLibrary("java_ext_wasm");
    }

    public static void main(String[] args) {
        Instance instance = new Instance();
        String output = instance.hello("Ivan");
        System.out.println(output);
        System.out.println(instance.exports);
    }
}
