package org.wasmer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

    public byte[] read(int offset, int length) {
        byte[] result = new byte[length];
        this.inner.position(offset);
        this.inner.get(result);
        return result;
    }

    public void write(int offset, byte[] data) {
        this.inner.position(offset);
        this.inner.put(data);
    }
}
