package org.wasmer;

import java.util.Arrays;
import java.nio.ByteBuffer;

class Memory {
    private native int nativeSetMemoryData(long instancePointer, ByteBuffer buffer);

    private ByteBuffer inner;
    private long instancePointer;

    public Memory(long instancePointer) {
        this.instancePointer = instancePointer;

        // Initial size is 1114112 bytes (64 KiB (65536 bytes) x 17 pages).
        this.inner = ByteBuffer.allocateDirect(1114112);
        int length = nativeSetMemoryData(instancePointer, inner);
        if (length > 0) {
            this.inner.limit(length);
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
