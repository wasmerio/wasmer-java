# Compile everything!
build: build-headers build-rust build-java

# Compile the Rust part.
build-rust: 
	cargo build --release

# Compile the Rust part.
build-rust-all-targets: build-rust-x86_64-darwin build-rust-x86_64-linux build-rust-x86_64-windows

build-rust-x86_64-darwin:
	rustup target add x86_64-apple-darwin
	cargo build --release --target=x86_64-apple-darwin
	mkdir -p artifacts/x86_64/darwin
	cp target/release/libjava_ext_wasm.dylib artifacts/x86_64/darwin
	install_name_tool -id "@rpath/libjava_ext_wasm.dylib" ./artifacts/x86_64/darwin/libjava_ext_wasm.dylib


build-rust-x86_64-linux:
	rustup target add x86_64-unknown-linux-gnu
	cargo build --release --target=x86_64-unknown-linux-gnu
	mkdir -p artifacts/x86_64/linux
	cp target/release/libjava_ext_wasm.so artifacts/x86_64/linux

build-rust-x86_64-windows:
	rustup target add x86_64-pc-windows-msvc
	cargo build --release --target=x86_64-pc-windows-msvc
	mkdir -p artifacts/x86_64/windows
	cp target/release/libjava_ext_wasm.so artifacts/x86_64/windows

# Compile the Java part.
build-java:
	./gradlew build

# Generate the Java C headers.
build-headers: build-java

# Run the tests.
test: build-headers test-rust test-java

# Run the Rust tests.
test-rust:
	cargo test --release

# Run the Java tests.
test-java:
	./gradlew test

# Make a JAR-file.
package:
	./gradlew jar

# Clean
clean:
	cargo clean
	cd java && mvn clean

# Local Variables:
# mode: makefile
# End:
