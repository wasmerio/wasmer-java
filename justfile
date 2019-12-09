# Compile everything!
build: build-rust build-java

# Compile the Rust part.
build-rust:
	cargo build --release

# Compile the Java part.
build-java:
	cd java && mvn compile

# Generate the Java C headers.
build-headers:
	javac -h include java/org/wasmer/Instance.java

# Run the tests.
test: test-rust test-java

# Run the Rust tests.
test-rust:
	cargo test --release

# Run the Java tests.
test-java:
	cd java && mvn test

# Local Variables:
# mode: makefile
# End:
