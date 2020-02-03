package org.wasmer;

import java.nio.ByteBuffer;

class Memory {
    private ByteBuffer inner;

    private Memory() {
        this.inner = ByteBuffer.allocateDirect(1114112);
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
