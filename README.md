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

 * **Easy to use**: The API mimics the standard WebAssembly API,
 * **Fast**: The Wasmer Java library executes the WebAssembly modules
   as fast as possible,
 * **Safe**: All calls to WebAssembly will be fast, but more
   importantly, completely safe and sandboxed.

# Install

Since the Wasmer Java library includes the [Wasmer
runtime](https://github.com/wasmerio/wasmer), written in Rust,
pre-compiled as a shared library, we produce one JAR per platform and
architecture. For the moment, the following are supported:

- `x86_64-darwin` (macOS, `x86` 64bits),
- `x86_64-linux` (Linux, `x86` 64bits),
- `x86_64-windows` (Windows, `x86` 64bits).

More architectures and more platforms will be added in a close
future. It is possible to produce your own JAR for your own platform
and architecture, see [the Development Section](#development) to learn
more.

Thus, the JAR files are named as follows:
`wasmer-jni-$(architecture)-$(os)-$(version).jar`. According the
[Gradle Jar
API](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Jar.html#org.gradle.api.tasks.bundling.Jar:appendix),
the `$(architecture)-$(os)` part is called the _archive prefix_. To
infer the JAR appendix automatically to configure your dependencies,
you can use the following `inferWasmerJarAppendix` function:

```gradle
String inferWasmerJarAppendix() {
    def nativePlatform = new org.gradle.nativeplatform.platform.internal.DefaultNativePlatform("current")
    def arch = nativePlatform.architecture
    def os = nativePlatform.operatingSystem

    def arch_name

    switch (arch.getName()) {
        case ["x86_64", "x64", "x86-64"]:
            arch_name = "x86_64"
            break;

        default:
            throw new RuntimeException("`wasmer-jni` has no pre-compiled archive for the architecture " + arch.getName())
    }

    def os_name

    if (os.isMacOsX()) {
        os_name = "darwin"
    } else if (os.isLinux()) {
        os_name = "linux"
    } else if (os.isWindows()) {
        os_name = "windows"
    } else {
        throw new RuntimeException("`wasmer-jni` has no pre-compiled archive for the platform " + os.getName())
    }

    return arch_name + "-" + os_name
}
```

Finally, you can configure your dependencies such as:

```gradle
dependencies {
    implementation "org.wasmer:wasmer-jni-" + inferWasmerJarAppendix() + ":0.1.0"
}
```

You can also download the Java JAR file from the
[Github releases page](https://github.com/wasmerio/java-ext-wasm/releases)!

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
class Example {
    public static void main(String[] args) {
        // simple.wasm is located at `tests/resources/`.
        Path wasmPath = Paths.get(new Example().getClass().getClassLoader().getResource("simple.wasm").getPath());

        // Reads the WebAssembly module as bytes.
        byte[] wasmBytes = Files.readAllBytes(wasmPath);

        // Instantiates the WebAssembly module.
        Instance = new Instance(wasmBytes);

        // Calls an exported function, and returns an object array.
        Object[] results = instance.exports.getFunction("sum").apply(5, 37);

        System.out.println((Integer) results[0]); // 42

        // Drops an instance object pointer which is stored in Rust.
        instance.close();
    }
}
```

For more information, you can check [this example
project](https://github.com/d0iasm/example-java-ext-wasm).

# API of the `wasmer` library

The root namespace is `org.wasmer`.

## The `Instance` class

The `Instance` constructor compiles and instantiates a WebAssembly
module. It is built upon bytes. From here, it is possible to call
exported functions, or exported memories. For example:

```java
// Instantiates the WebAssembly module.
Instance instance = new Instance(wasmBytes);

// Calls an exported function.
Object[] results = instance.exports.getFunction("sum").apply(1, 2);

// Casts an object to an integer object because the result is an object array.
int result = (Integer) results[0];

System.out.println(result); // 3

// Drops an instance object pointer manually. Note that the garbage collector
// will call this method before an object is removed from the memory.
instance.close();
```

### Exports

All exports, like functions or memories, are accessible on the
`Instance.exports` field, which is of kind `Exports` (a read-only
wrapper around a map of kind `Map<String, exports.Export>`). The
`Exports.get` method returns an object of type `Export`. To
downcast it to an exported function or to an exported memory, you can
use the respective `getFunction` or `getMemory` methods. The following
sections describe each exports in details.

#### Exported functions

An exported function is a native Java closure (represented by the
`exports.Function` class), where all arguments are automatically
casted to WebAssembly values if possible, and all results are of type
`Object`, which can be typed to `Integer` or `Float` for instance.

```java
Function sum = instance.exports.getFunction("sum");
Object[] results = sum.apply(1, 2);

System.out.println((Integer) results[0]); // 3
```

#### Exported memories

An exported memory is a regular `Memory` class.

```java
Memory memory = instance.exports.getMemory("memory_1");
```

See the [`Memory`](#the-memory-class) class section for more information.

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
Object[] results = module2.instantiate().exports.getFunction("sum").apply(1, 2);

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

// Gets the memory by specifying its exported name.
Memory memory = instance.exports.getMemory("memory");

// Gets the pointer value as an integer.
int pointer = (Integer) instance.exports.getFunction("return_hello").apply()[0];

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

## Development

If you want to build the extension you will need the following tools:

- [Gradle](https://gradle.org/), a package management tool,
- [Rust](https://rust-lang.org/), the Rust programming language,
- [Java](https://www.java.com/), because it's a Java project ;-).

```sh
$ git clone https://github.com/wasmerio/java-ext-wasm/
$ cd java-ext-wasm
```

To build the entire project, run the following command:

```sh
$ make build
```

To build the JAR package:

```sh
$ make package
```

This will generate the file `build/libs/wasmer-jni-0.1.0.jar`.

### Testing

Run the following command:

```sh
$ make test
```

Note: Testing automatically builds the project.

### Documentation

Run the following command:

```sh
$ make java-doc
```

Then open `build/docs/javadoc/index.html`.

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
