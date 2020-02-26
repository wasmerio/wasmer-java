<p align="center">
  <a href="https://wasmer.io" target="_blank" rel="noopener">
    <img width="300" src="https://raw.githubusercontent.com/wasmerio/wasmer/master/assets/logo.png" alt="Wasmer logo">
  </a>
</p>

<p align="center">
  <a href="https://spectrum.chat/wasmer">
    <img src="https://withspectrum.github.io/badge/badge.svg" alt="Join the Wasmer Community"></a>
  <a href="https://github.com/wasmerio/wasmer/blob/master/LICENSE">
    <img src="https://img.shields.io/github/license/wasmerio/wasmer.svg" alt="License"></a>
</p>

Wasmer is a Java library for executing WebAssembly binaries:

 * **Easy to use**: The `wasmer` API mimics the standard WebAssembly API,
 * **Fast**: `wasmer` executes the WebAssembly modules as fast as possible,
 * **Safe**: All calls to WebAssembly will be fast, but more
   importantly, completely safe and sandboxed.

# Install

TODO: Upload this project to [maven central](https://mvnrepository.com/repos/central)?
TODO: Write how to import this project to your project.

# Example

There is a toy program in `java/src/test/resources/simple.rs`, written
in Rust (or any other language that compiles to WebAssembly):

```rust
#[no_mangle]
pub extern fn sum(x: i32, y: i32) -> i32 {
    x + y
}
```

After compilation to WebAssembly, the
[`java/src/test/resources/simple.wasm`](https://github.com/wasmerio/java-ext-wasm/blob/master/java/src/test/resources/simple.wasm)
binary file is generated. ([Download
it](https://github.com/wasmerio/java-ext-wasm/raw/master/java/src/test/resources/simple.wasm)).

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

# API of the `wasmer` library

## The `Instance` class

The `Instance` constructor compiles and instantiates a WebAssembly
module. It is built upon bytes.  From here, it is possible to call
exported functions, or exported memories. For example:

```java
// Instantiates the WebAssembly module.
Instance instance = new Instance(wasmBytes);

// Calls an exported function.
Object[] results = instance.exports.get("sum").apply(1, 2);

// Casts an object to an integer object because the result is an object array.
int result = (Integer) results[0];

System.out.println(result); // 3

// Drops an instance object pointer manually. Note that the garbage collector
// will call this method before an object is removed from the memory.
instance.close();
```

### Exported functions

All exported functions are accessible on the `Instance.exports` field
in the `Instance` class. The `get` method allows to get a single
exported function by its name. An exported function is a Java closure,
where all arguments are automatically casted to WebAssembly values if
possible, and all results are of type `Object`, which can be typed to
`Integer` or `Float` for instance.

```java
Exports exportedFunctions = instance.exports;
ExportedFunction sum = exportedFunctions.get("sum");

Object[] results = sum.apply(1, 2);

System.out.println((Integer) results[0]); // 3
```

### Exported memories

The `Instance.memories` field exposes the `Memories` class
representing the set of memories of that particular instance, e.g.:

```java
Memories memories = instance.memories;
```

See the [`Memory`][#memory] class section for more information.

## The `Module` class

The `Module.validate` static method checks whether a sequence of bytes
represents a valid WebAssembly module:

```java
// Checks that given bytes represent a valid WebAssembly module.
boolean isValid = Module.validate(wasmBytes);
```

The `Module` constructor compiles a sequence of bytes into a
WebAssembly module. From here, it is possible to instantiate it:

```java
// Compiles the bytes into a WebAssembly module.
Module module = new Module(wasmBytes);

// Instantiates the WebAssembly module.
Instance instance = module.instantiate();
```

### Serialization and deserialization

The `Module.serialize` method and its complementary
`Module.deserialize` static method help to respectively serialize and
deserialize a _compiled_ WebAssembly module, thus saving the compilation
time for the next use:

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

A WebAssembly instance has a linear memory, represented by the
`Memory` class. Let's see how to read it. Consider the following Rust
program:

```rust
#[no_mangle]
pub extern fn return_hello() -> *const u8 {
    b"Hello, World!\0".as_ptr()
}
```

The `return_hello` function returns a pointer to a string. This string
is stored in the WebAssembly memory. Let's read it.

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
