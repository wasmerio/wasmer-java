package org.wasmer;

import java.util.Arrays;

class Memory {
    private byte[] inner;

    public Memory(byte[] bytes) {
        this.inner = bytes;
    }

    public byte[] read(int offset, int length) {
        return Arrays.copyOfRange(this.inner, offset, length);
    }

    public void write(int offset, int length, byte[] data) {
        for (int i = offset; i < length; i++) {
	    this.inner[i] = data[i];
	}
    }
}
