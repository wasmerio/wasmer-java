# Compile the Java extension.
build:
	cargo build --release
	javac java/org/wasmer/Instance.java

# Generate the Java C headers.
build-headers:
	javac -h include java/org/wasmer/Instance.java

test:
	cargo test --release

# Local Variables:
# mode: makefile
# End:
