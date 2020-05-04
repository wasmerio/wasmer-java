package org.wasmer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ReadOnlyBufferException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

class MemoryTest {
    private byte[] getBytes(String filename) throws IOException,Exception {
        Path modulePath = Paths.get(getClass().getClassLoader().getResource(filename).toURI());
        return Files.readAllBytes(modulePath);
    }

    @Test
    void size() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));
        Memory memory = instance.exports.getMemory("memory");

        assertEquals(1114112, memory.size());

        instance.close();
    }

    @Test
    void readStaticallyAllocatedDataInMemory() throws IOException, Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));

        byte[] expectedData = "Hello, World!".getBytes();

        int pointer = (Integer) instance.exports.getFunction("string").apply()[0];
        Memory memory = instance.exports.getMemory("memory");
        byte[] readData = memory.read(pointer, expectedData.length);

        assertArrayEquals(expectedData, readData);

        instance.close();
    }

    @Test
    void readMemory() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));

        Memory memory = instance.exports.getMemory("memory");
        byte[] readData = memory.read(0, 5);
        assertArrayEquals(new byte[]{0, 0, 0, 0, 0}, readData);

        instance.close();
    }

    @Test
    void writeMemory() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));

        Memory memory = instance.exports.getMemory("memory");
        byte[] data = new byte[]{1, 2, 3, 4, 5};
        memory.write(0, data);

        byte[] readData = memory.read(0, 5);
        assertArrayEquals(data, readData);

        instance.close();
    }

    @Test
    void readInvalidIndex() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));
        Memory memory = instance.exports.getMemory("memory");

        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            byte[] readData = memory.read(-1, 5);
        });
        String expected = "newPosition < 0: (-1 < 0)";

        assertTrue(exception instanceof IllegalArgumentException);
        assertEquals(expected, exception.getMessage());

        instance.close();
    }

    @Test
    void readOverLimit() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));
        Memory memory = instance.exports.getMemory("memory");

        Exception exception = Assertions.assertThrows(BufferUnderflowException.class, () -> {
            byte[] readData = memory.read(0, 1114113);
        });

        assertTrue(exception instanceof BufferUnderflowException);

        instance.close();
    }

    @Test
    void writeInvalidIndex() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));
        Memory memory = instance.exports.getMemory("memory");

        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            memory.write(-1, new byte[]{1, 2, 3, 4, 5});
        });
        String expected = "newPosition < 0: (-1 < 0)";

        assertTrue(exception instanceof IllegalArgumentException);
        assertEquals(expected, exception.getMessage());

        instance.close();
    }

    @Test
    void writeOverLimit() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));
        Memory memory = instance.exports.getMemory("memory");

        byte[] writeData = new byte[1114113];
        Exception exception = Assertions.assertThrows(BufferOverflowException.class, () -> {
            memory.write(0, writeData);
        });

        assertTrue(exception instanceof BufferOverflowException);

        instance.close();
    }

    @Test
    void noMemory() throws IOException,Exception {
        Instance instance = new Instance(getBytes("no_memory.wasm"));
        Memory memory = instance.exports.getMemory("memory");

        assertNull(memory);

        instance.close();
    }

    @Test
    void javaBorrowsRustMemory() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));
        Memory memory = instance.exports.getMemory("memory");

        int pointer = (Integer) instance.exports.getFunction("string").apply()[0];

        assertEquals("Hello, World!", new String(memory.read(pointer, 13)));

        memory.write(pointer, new byte[]{'A'});

        assertEquals("Aello, World!", new String(instance.exports.getMemory("memory").read(pointer, 13)));

        instance.close();
    }

    @Test
    void memoryGrow() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));
        Memory memory = instance.exports.getMemory("memory");

        int oldSize = memory.size();
        assertEquals(1114112, oldSize);
        memory.grow(1);
        int newSize = memory.size();
        assertEquals(1179648, newSize);
        assertEquals(65536, newSize - oldSize);

        instance.close();
    }

    @Test
    void writeMemoryAfterGrow() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));
        Memory memory = instance.exports.getMemory("memory");

        int overIndex = memory.size() + 1;
        byte[] writeData = new byte[]{1, 2, 3, 4, 5};
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            memory.write(overIndex, writeData);
        });
        String expected = "newPosition > limit: (1114113 > 1114112)";

        assertTrue(exception instanceof IllegalArgumentException);
        assertEquals(expected, exception.getMessage());

        memory.grow(1);

        memory.write(overIndex, writeData);
        byte[] readData = memory.read(overIndex, writeData.length);

        assertArrayEquals(writeData, readData);

        instance.close();
    }
}
