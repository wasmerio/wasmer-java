package org.wasmer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class Memory {
    private ByteBuffer inner;

    private Memory() {
        // Initial size is 114112 bytes (65536 bytes (64 KiB) * 17 pages).
        this.inner = ByteBuffer.allocateDirect(1114112);
        if (this.inner.order() != ByteOrder.LITTLE_ENDIAN) {
            this.inner = this.inner.order(ByteOrder.LITTLE_ENDIAN);
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
