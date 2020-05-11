package org.wasmer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
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

        assertEquals(1114112, memory.buffer().limit());

        instance.close();
    }

    @Test
    void readStaticallyAllocatedDataInMemory() throws IOException, Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));

        byte[] expectedData = "Hello, World!".getBytes();

        int pointer = (Integer) instance.exports.getFunction("string").apply()[0];
        Memory memory = instance.exports.getMemory("memory");
        ByteBuffer memoryBuffer = memory.buffer();

        byte[] readData = new byte[expectedData.length];
        memoryBuffer.position(pointer);
        memoryBuffer.get(readData);

        assertArrayEquals(expectedData, readData);

        instance.close();
    }

    @Test
    void readMemory() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));

        Memory memory = instance.exports.getMemory("memory");
        ByteBuffer memoryBuffer = memory.buffer();

        byte[] readData = new byte[5];
        memoryBuffer.get(readData);

        assertArrayEquals(new byte[]{0, 0, 0, 0, 0}, readData);

        instance.close();
    }

    @Test
    void writeMemory() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));

        Memory memory = instance.exports.getMemory("memory");
        ByteBuffer memoryBuffer = memory.buffer();

        byte[] writtenData = new byte[]{1, 2, 3, 4, 5};
        memoryBuffer.put(writtenData);

        byte[] readData = new byte[5];
        memoryBuffer.position(0);
        memoryBuffer.get(readData);

        assertArrayEquals(writtenData, readData);

        ByteBuffer memoryBuffer2 = memory.buffer();
        byte[] readData2 = new byte[5];
        memoryBuffer2.get(readData2);

        assertArrayEquals(writtenData, readData2);

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

        {
            Memory memory = instance.exports.getMemory("memory");
            ByteBuffer memoryBuffer = memory.buffer();

            int pointer = (Integer) instance.exports.getFunction("string").apply()[0];
            byte[] data = new byte[13];
            memoryBuffer.position(pointer);
            memoryBuffer.get(data);

            assertEquals("Hello, World!", new String(data));

            memoryBuffer.position(pointer);
            memoryBuffer.put(new byte[]{'A'});
        }

        {
            Memory memory = instance.exports.getMemory("memory");
            ByteBuffer memoryBuffer = memory.buffer();

            int pointer = (Integer) instance.exports.getFunction("string").apply()[0];
            byte[] data = new byte[13];
            memoryBuffer.position(pointer);
            memoryBuffer.get(data);

            assertEquals("Aello, World!", new String(data));
        }

        instance.close();
    }

    @Test
    void memoryGrow() throws IOException,Exception {
        Instance instance = new Instance(getBytes("tests.wasm"));
        Memory memory = instance.exports.getMemory("memory");

        int oldSize = memory.buffer().limit();
        assertEquals(1114112, oldSize);

        memory.grow(1);

        int newSize = memory.buffer().limit();
        assertEquals(1179648, newSize);
        assertEquals(65536, newSize - oldSize);

        instance.close();
    }
}
