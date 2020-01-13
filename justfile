# Compile everything!
build: build-headers build-rust build-java

# Compile the Rust part.
build-rust:
	cargo build --release

# Compile the Java part.
build-java:
	cd java && mvn compile

# Generate the Java C headers.
build-headers:
	javac -h include -sourcepath java/src/main/java/org/wasmer/*.java java/src/main/java/org/wasmer/Instance.java

# Run the tests.
test: build-headers test-rust test-java

# Run the Rust tests.
test-rust:
	cargo test --release

# Run the Java tests.
test-java: build-rust
	cd java && mvn test

# Local Variables:
# mode: makefile
# End:
