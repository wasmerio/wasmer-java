# How Java and Rust communicate

Java allows to write native code, and to expose it through a Java
interface. For example, in `Instance.java`, we can read:

```java
class Instance {
    private native long nativeInstantiate(Instance self, byte[] moduleBytes) throws RuntimeException;
    // …
}
```

We define a public method to call a native method, such as:
```java
    public Instance(byte[] moduleBytes) throws RuntimeException {
        long instancePointer = this.instantiate(this, moduleBytes);

        this.instancePointer = instancePointer;
    }
```

The native implementation is written in Rust. First, a C header is generated
with `just build-headers` which is located in `include/`. The generated code for
`nativeInstantiate` is the following:

```c
/*
 * Class:     org_wasmer_Instance
 * Method:    nativeInstantiate
 * Signature: (Lorg/wasmer/Instance;[B)J
 */
JNIEXPORT jlong JNICALL Java_org_wasmer_Instance_nativeInstantiate
  (JNIEnv *, jobject, jobject, jbyteArray);
```

Second, on the Rust side, we have to declare a function with the same naming:
```rust
#[no_mangle]
pub extern "system" fn Java_org_wasmer_Instance_nativeInstantiate(
    env: JNIEnv,
    _class: JClass,
    this: JObject,
    module_bytes: jbyteArray,
) -> jptr {
    // …
}
```

And the dynamic linking does the rest (it's done with the
`java.library.path` configuration on the Java side). It uses a shared
library (`.dylib` on macOS, `.so` on Linux, `.dll` on Windows).

Then, we have to convert “Java data” to “Rust data”. [`jni-rs`'s
documentation](https://docs.rs/jni/0.14.0/jni/index.html) is our best
friend here.
