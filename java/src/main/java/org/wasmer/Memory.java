package org.wasmer;

import java.lang.IllegalArgumentException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.BufferUnderflowException;
import java.nio.BufferOverflowException;
import java.nio.ReadOnlyBufferException;

/**
 * `Memory` is a Java class that represents a WebAssembly memory.
 *
 * Example:
 * <pre>{@code
 * Instance instance = new Instance(wasmBytes);
 * Memory memory = instance.memories.get("memory-name");
 * memory.write(0, new byte[]{1, 2, 3, 4, 5});
 * byte[] bytes = memory.read(0, 5);
 * }</pre>
 */
public class Memory {
    private native int nativeMemoryGrow(Memory memory, long memoryPointer, int page);

    /**
     * Represents the actual WebAssembly memory data, borrowed from the runtime (in Rust).
     * The `setInner` method must be used to set this attribute.
     */
    private ByteBuffer inner;
    private long memoryPointer;

    private Memory() {
        // This object is instantiated by Rust.
    }

    /**
     * Set the `ByteBuffer` of this memory. See `Memory.inner` to learn more.
     *
     * In addition, this method correctly sets the endianess of the `ByteBuffer`.
     */
    private void setInner(ByteBuffer inner) {
        this.inner = inner;

        // Ensure the endianess matches WebAssemly specification.
        if (this.inner.order() != ByteOrder.LITTLE_ENDIAN) {
            this.inner.order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    /**
     * Read the set of bytes from the offset.
     *
     * @param offset The offset within the memory data of the first byte to be read.
     * @param length The number of bytes to be read from the memory data.
     * @return The set of bytes.
     */
    public byte[] read(int offset, int length) throws BufferUnderflowException, IllegalArgumentException {
        byte[] result = new byte[length];

        this.inner.position(offset);
        this.inner.get(result);

        return result;
    }

    /**
     * Write the set of bytes from the offset.
     *
     * @param offset The offset within the memory data of the first byte to be read.
     * @param length The number of bytes to be read from the memory data.
     */
    public void write(int offset, byte[] data) throws BufferOverflowException, IllegalArgumentException, ReadOnlyBufferException {
        this.inner.position(offset);
        this.inner.put(data);
    }

    /**
     * Return the size of the memory.
     *
     * @return The size of the memory.
     */
    public int size() {
        return this.inner.limit();
    }

    /**
     * Grow this memory by the specified number of pages.
     *
     * @param page The number of pages to grow. 1 page size is 64KiB.
     * @return The privious number of pages.
     */
    public int grow(int page) {
        return this.nativeMemoryGrow(this, this.memoryPointer, page);
    }
}
