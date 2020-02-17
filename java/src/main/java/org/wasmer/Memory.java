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
        for (int i = 0; i < length; i++) {
            result[i] = this.inner.get(i + offset);
        }
        return result;
    }

    public void write(int offset, int length, byte[] data) {
        for (int i = 0; i < length; i++) {
            this.inner.put(i + offset, data[i]);
        }
    }
}
