package org.wasmer;

import java.lang.IllegalArgumentException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.BufferUnderflowException;
import java.nio.BufferOverflowException;
import java.nio.ReadOnlyBufferException;

class Memory {
    private ByteBuffer inner;

    private Memory() {
        // `inner` field is set in Rust.
    }

    private void setInner(ByteBuffer inner) {
        this.inner = inner;
        if (this.inner.order() != ByteOrder.LITTLE_ENDIAN) {
            this.inner.order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    public byte[] read(int offset, int length) throws BufferUnderflowException, IllegalArgumentException {
        byte[] result = new byte[length];
        this.inner.position(offset);
        this.inner.get(result);
        return result;
    }

    public void write(int offset, byte[] data) throws BufferOverflowException, IllegalArgumentException, ReadOnlyBufferException {
        this.inner.position(offset);
        this.inner.put(data);
    }
}
