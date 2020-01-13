# Wasmer in Java

## Build and test

To build:

```sh
$ just build
```

To test:

```sh
$ just test
```

Testing automatically build the project.

And yes, you need [`just`](https://github.com/casey/just/).

## Workflows

That's early development of this project, but here is how it works for
the moment.

Java allows to write native code, and to expose it through a Java
interface. For example, in `Instance.java`, we can read:

```java
class Instance {
    private native long instantiate(Instance self, byte[] moduleBytes) throws RuntimeException;
    // …
}
```

It is better for the moment to keep all the native implementations
private, and to expose a friendly public method around it, such as:

```java
    public Instance(byte[] moduleBytes) throws RuntimeException {
        long instancePointer = this.instantiate(this, moduleBytes);

        this.instancePointer = instancePointer;
    }
```

The private implementation is written in Rust. First, a C header is
generated (with `just build-headers` but it's automatically done for
most workflows). It is located in `include/`. The generated code for
`instantiate` is the following:

```c
/*
 * Class:     org_wasmer_Instance
 * Method:    instantiate
 * Signature: (Lorg/wasmer/Instance;[B)J
 */
JNIEXPORT jlong JNICALL Java_org_wasmer_Instance_instantiate
  (JNIEnv *, jobject, jobject, jbyteArray);
```

This code isn't particularily useful for us. It is used by the tools
building the project.

Second, on the Rust side, we have to declare a function with the same
naming:

```rust
#[no_mangle]
pub extern "system" fn Java_org_wasmer_Instance_instantiate(
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

### Opaque pointers

Most of the time, we want to handle everything on the Rust side, with
structures for example, and to just pass opaque pointers to Java. This
is how it works for the moment. We have an `Instance` structure,
managed by Rust, and a pointer is passed to Java. For example, the
`instantiate` function returns a `jptr`, which is a type alias for
`jlong` (semantics matters).

There is this special `Pointer` structure that must be used with the
following patterns:

  * `Pointer::new(data).into()` to put the data on the heap, and to
    return its pointer as a `jptr`,
  * `let _: Pointer<Instance> = pointer.into()` to get a typed
    `Pointer` from a `jptr`, but this data is owned! So once `Pointer`
    goes out of scope, the data it holds is dropped,
  * `let _: &mut Instance =
    Into::<Pointer<Instance>>::into(pointer).borrow()` is similar to
    the previous pattern, but this time, the data is borrowed.

Those small patterns aimed at ensuring safety between Java <-> JNI <->
Rust boundaries.

### Panics, errors, and exceptions

The project comes with an `unwrap_or_throw` utility function. It
generates a `RuntimeException` in case of an error. This function
doesn't stop the execution on the Rust side, so it must be used when
the function returns a value.

A typical usage is the following:

```rust
#[no_mangle]
pub extern "system" fn Java_org_wasmer_Instance_instantiate(
    env: JNIEnv,
    _class: JClass,
    this: JObject,
    module_bytes: jbyteArray,
) -> jptr {
    let output = panic::catch_unwind(|| {
        let module_bytes = env.convert_byte_array(module_bytes)?;
        let java_instance = env.new_global_ref(this)?;

        let instance = Instance::new(java_instance, module_bytes);

        Ok(Pointer::new(instance).into())
    });

    unwrap_or_throw(&env, output)
}
```

`output` contains the result of `panic::catch_unwind`. And
`unwrap_or_throw` returns the pointer to an instance
(`Ok(Pointer::new(…).into())`), or a `jptr::default()` value in case
of an exception.

Note: When testing exceptions with `just test`, it runs Maven on the
background. The test plugin (Surefire) is broken with exceptions
thrown on the native side (Rust side), but it works well outside
Surefire.

### Memory management

Java is a garbage collected runtime. To avoid the garbage collector to
collect our data, it's better to keep a `GlobalRef` of an object (see
`Instance.java_instance`). I think it's the way to do. Not sure yet at
100%.

Java has destructors, called `finalize` methods. We must also add
`close` methods, and it's up to the user to call them when the data
must be dropped. This method must call an associated drop function on
the Rust side. See `Java_org_wasmer_Instance_drop`. A `finalize`
method must call a `close` method.


# License

The entire project is under the MIT License. Please read [the
`LICENSE` file][license].


[license]: https://github.com/wasmerio/wasmer/blob/master/LICENSE
