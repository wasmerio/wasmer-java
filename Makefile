UNAME_S := $(shell uname -s)

ifeq ($(UNAME_S),Darwin)
	build_os := darwin
endif
ifeq ($(OS),Windows_NT)
	build_os := windows
endif
ifeq ($(UNAME_S),Linux)
	build_os := linux
endif
ARCH := $(shell uname -m)
ifeq ($(ARCH),x86_64)
	build_arch = x86_64
else
	$(error Architecture not supported yet)
endif

# Compile everything!
build: build-headers build-rust build-java

# Compile the Rust part (only for one target).
# We relay this command to the others, to make sure that
# artifacts are set properly.
build-rust: build-rust-$(build_arch)-$(build_os)

# Compile the Rust part.
build-rust-all-targets: build-rust-x86_64-darwin build-rust-x86_64-linux build-rust-x86_64-windows

build-rust-x86_64-darwin:
	rustup target add x86_64-apple-darwin
	cargo build --release --target=x86_64-apple-darwin
	mkdir -p artifacts/darwin-x86_64
	cp target/x86_64-apple-darwin/release/libwasmer_jni.dylib artifacts/darwin-x86_64
	install_name_tool -id "@rpath/libwasmer_jni.dylib" ./artifacts/darwin-x86_64/libwasmer_jni.dylib
	test -h target/current || ln -s x86_64-apple-darwin/release target/current

build-rust-x86_64-linux:
	rustup target add x86_64-unknown-linux-gnu
	cargo build --release --target=x86_64-unknown-linux-gnu
	mkdir -p artifacts/linux-x86_64
	cp target/x86_64-unknown-linux-gnu/release/libwasmer_jni.so artifacts/linux-x86_64/
	test -h target/current || ln -s x86_64-unknown-linux-gnu/release target/current

build-rust-x86_64-windows:
	rustup target add x86_64-pc-windows-msvc
	cargo build --release --target=x86_64-pc-windows-msvc
	mkdir -p artifacts/windows-x86_64
	cp target/x86_64-pc-windows-msvc/release/wasmer_jni.dll artifacts/windows-x86_64/
	test -h target/current || ln -s x86_64-pc-windows-msvc/release target/current

# Compile the Java part (incl. `build-test`, see `gradlew`).
build-java:
	"./gradlew" build --info

# Generate the Java C headers.
build-headers:
	"./gradlew" generateJniHeaders

# Run the tests.
test: build-headers build-rust test-rust build-java

# Run the Rust tests.
test-rust: test-rust-$(build_arch)-$(build_os)

test-rust-x86_64-darwin:
	cargo test --release --target=x86_64-apple-darwin

test-rust-x86_64-linux:
	cargo test --release --target=x86_64-unknown-linux-gnu

test-rust-x86_64-windows:
	cargo test --release --target=x86_64-pc-windows-msvc

# Run the Java tests.
test-java:
	"./gradlew" test --info

# Make a JAR-file.
package:
	"./gradlew" jar

# Publish the package artifact to a public repository
publish:
	"./gradlew" publish

# Clean
clean:
	cargo clean
	rm -rf build
	rm -rf artifacts
