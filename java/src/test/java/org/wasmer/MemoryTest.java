package org.wasmer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.IllegalArgumentException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.BufferOverflowException;
import java.nio.ReadOnlyBufferException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MemoryTest {
    private byte[] getBytes(String filename) throws IOException,Exception {
        Path modulePath = Paths.get(getClass().getClassLoader().getResource(filename).getPath());
        return Files.readAllBytes(modulePath);
    }

    @Test
    void isMemoryClass() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));

        instance.memories.inner().forEach((name, memory) -> {
            assertTrue(memory instanceof Memory);
        });

        instance.close();
    }

    @Test
    void size() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));
        Memory memory = instance.memories.get("memory");

        assertEquals(1114112, memory.size());

        instance.close();
    }

    @Test
    void initialData() throws IOException, Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));

        byte[] expectedData = "Hello, World!".getBytes();

        /* According to the `wasm-objdump -x tests.wasm`, the data starts from 1048576.
         * Data[1]:
         * - segment[0] memory=0 size=13 - init i32=1048576
         * - 0100000: 4865 6c6c 6f2c 2057 6f72 6c64 21         Hello, World!
         */
        Memory memory = instance.memories.get("memory");
        byte[] readData = memory.read(1048576, expectedData.length);

        assertArrayEquals(expectedData, readData);
    }

    @Test
    void readMemory() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));

        Memory memory = instance.memories.get("memory");
        byte[] readData = memory.read(0, 5);
        assertArrayEquals(new byte[]{0, 0, 0, 0, 0}, readData);

        instance.close();
    }

    @Test
    void writeMemory() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));

        Memory memory = instance.memories.get("memory");
        byte[] writeData = new byte[]{1, 2, 3, 4, 5};
        memory.write(0, writeData);

        byte[] readData = memory.read(0, 5);
        assertArrayEquals(writeData, readData);

        instance.close();
    }

    @Test
    void readInvalidIndex() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));
        Memory memory = instance.memories.get("memory");

        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
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
        Memory memory = instance.memories.get("memory");

        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
            byte[] readData = memory.read(0, 1114113);
        });

        assertTrue(exception instanceof BufferUnderflowException);

        instance.close();
    }

    @Test
    void writeInvalidIndex() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));
        Memory memory = instance.memories.get("memory");

        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
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
        Memory memory = instance.memories.get("memory");

        byte[] writeData = new byte[1114113];
        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
            memory.write(0, writeData);
        });

        assertTrue(exception instanceof BufferOverflowException);

        instance.close();
    }

    @Test
    void noMemory() throws IOException,Exception {
        Instance instance = new Instance(getBytes("no_memory.wasm"));
        Memory memory = instance.memories.get("memory");
        assertNull(memory);
        instance.close();
    }
}
