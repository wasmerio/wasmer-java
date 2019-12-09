package org.wasmer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InstanceTests {
    @Test
    void basic() {
        Instance instance = new Instance();
        String output = instance.hello("Ivan");

        assertEquals("Hello, Ivan!", output);
    }
}
