package org.wasmer;

public enum Type {
    I32((byte)1),
    I64((byte)2),
    F32((byte)3),
    F64((byte)4);

    byte i;

    Type(byte i) {
        this.i = i;
    }
}
