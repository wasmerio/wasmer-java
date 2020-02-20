<p align="center">
  <a href="https://wasmer.io" target="_blank" rel="noopener">
    <img width="300" src="https://raw.githubusercontent.com/wasmerio/wasmer/master/logo.png" alt="Wasmer logo">
  </a>
</p>

<p align="center">
  <a href="https://spectrum.chat/wasmer">
    <img src="https://withspectrum.github.io/badge/badge.svg" alt="Join the Wasmer Community"></a>
  <a href="https://github.com/wasmerio/wasmer/blob/master/LICENSE">
    <img src="https://img.shields.io/github/license/wasmerio/wasmer.svg" alt="License"></a>
</p>

Wasmer is a Javac library for executing WebAssembly binaries:

 * **Easy to use**: The `wasmer` API mimics the standard WebAssembly API,
 * **Fast**: `wasmer` executes the WebAssembly modules as fast as possible,
 * **Safe**: All calls to WebAssembly will be fast, but more
   importantly, completely safe and sandboxed.

# Install
TODO: Upload this project to [maven central](https://mvnrepository.com/repos/central)?  
TODO: Write how to import this project to your project.

# Example
There is a toy program in `java/src/test/resources/simple.rs`, written in Rust (or any other language that compiles to WebAssembly):

```rust
#[no_mangle]
pub extern fn sum(x: i32, y: i32) -> i32 {
    x + y
}
```

After compilation to WebAssembly, the [`java/src/test/resources/simple.wasm`](https://github.com/wasmerio/java-ext-wasm/blob/master/java/src/test/resources/simple.wasm) binary file is generated. ([Download it](https://github.com/wasmerio/java-ext-wasm/raw/master/java/src/test/resources/simple.wasm)).

Then, we can execute it in Java:

```java
import java.nio.file.Files;

class Example {
    public static void main(String[] args) {
        // Reads the WebAssembly module as bytes.
        byte[] wasmBytes = Files.readAllBytes("simple.wasm");

        // Instantiates the WebAssembly module.
        Instanace = new Instance(wasmBytes);

        // Calls an exported function, and returns an object array.
        Object[] results = instance.exports.get("sum").apply(5, 37);

        System.out.println((Integer) results[0]); // 42

        // Drops an instance object pointer which is stored in Rust.
        instance.close();
    }
}
```

# API of the `wasmer` extension/module
## The `Instance` class
Instantiates a WebAssembly module represented by bytes, and calls exported functions on it:
```java
// Instantiates the WebAssembly module.
Instance instance = new Instance(wasmBytes);

// Calls an exported function.
Object[] results = instance.exports.get("sum").apply(1, 2);

// Casts an object to an integer object because the result is an object array.
int result = (Integer) results[0];

System.out.println(result); // 3

// Drops an instance object pointer, but the garbage collector
// will call this method before an object is removed from the memory.
instance.close();
```

### Exported functions
All exported functions are accessible on the `exports` field in the `Instance` class. Arguments of these functions are automatically casted to WebAssembly values.
```java
Exports exportedFunction = instance.exports;
```

### Exported memories
The `memories` field exposes the `Memories` class representing the set of memories of that particular instance, e.g.:
```java
Memories memories = instance.memories;
```
See the `Memory` class section for more information.

## The `Module` class
Compiles a sequence of bytes into a WebAssembly module. From here, it is possible to instantiate it:
```java
// Checks that given bytes represent a valid WebAssembly module.
boolean isValid = Module.validate(wasmBytes);

// Compiles the bytes into a WebAssembly module.
Module module = new Module(wasmBytes);

// Instantiates the WebAssembly module.
Instance instance = module.instantiate();
```

### Serialization and deserialization
The `Module.serialize` method and its complementary `Module.deserialize` static method help to respectively serialize and deserialize a compiled WebAssembly module, thus saving the compilation time for the next use:
```java
// Compiles the bytes into a WebAssembly module.
Module module1 = new Module(wasmBytes);

// Serializes the module.
byte[] serializedModule = module1.serialize();

// Let's forget about the module for this example.
module1 = null;

// Deserializes the module.
Module module2 = Module.deserialize(serializedModule);

// Instantiates and uses it.
Object[] results = module2.instantiate().exports.get("sum").apply(1, 2);

System.out.println((Integer) results[0]); // 3
```

## The `Memory` class
A WebAssembly instance has a linear memory, represented by the `Memory` class. Let's see how to read it. Consider the following Rust program:
```rust
#[no_mangle]
pub extern fn return_hello() -> *const u8 {
    b"Hello, World!\0".as_ptr()
}
```

The `return_hello` function returns a pointer to a string. This string is stored in the WebAssembly memory. Let's read it.

```java
Instance instance = new Instance(wasmBytes);

// Gets the memory by specifing its exported name.
Memory memory = instance.memories.get("memory");

// Gets the pointer value as an integer.
int pointer = (Integer) instance.exports.get("return_hello").apply()[0];

// Reads the data from the memory.
byte[] stringBytes = memory.read(pointer, 13);

System.out.println(new String(stringBytes)); // Hello, World!

instance.close();
```

### Memory grow
The `Memory.grow` methods allows to grow the memory by a number of pages (of 64KiB each).
```java
// Grows the memory by the specified number of pages, and returns the number of old pages.
int oldPageSize = memory.grow(1);
```

# Development
You need [just](https://github.com/casey/just/) to build the project.

To build Java parts, run the following command:
```sh
$ just build-java
```

To build Rust parts, run the following command:
```sh
$ just build-rust
```

To build the entire project, run the following command:
```sh
$ just build
```

# Testing
Run the following command:
```sh
$ just test
```
Testing automatically build the project.

# JNI overview
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

# What is WebAssembly?
Quoting [the WebAssembly site](https://webassembly.org/):

> WebAssembly (abbreviated Wasm) is a binary instruction format for a
> stack-based virtual machine. Wasm is designed as a portable target
> for compilation of high-level languages like C/C++/Rust, enabling
> deployment on the web for client and server applications.

About speed:

> WebAssembly aims to execute at native speed by taking advantage of
> [common hardware
> capabilities](https://webassembly.org/docs/portability/#assumptions-for-efficient-execution)
> available on a wide range of platforms.

About safety:

> WebAssembly describes a memory-safe, sandboxed [execution
> environment](https://webassembly.org/docs/semantics/#linear-memory) […].

# License
The entire project is under the MIT License. Please read [the
`LICENSE` file][license].

[license]: https://github.com/wasmerio/wasmer/blob/master/LICENSE
