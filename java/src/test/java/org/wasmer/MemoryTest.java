package org.wasmer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class MemoryTest {
    private byte[] getBytes() throws IOException,Exception {
        Path modulePath = Paths.get(getClass().getClassLoader().getResource("tests.wasm").getPath());
        return Files.readAllBytes(modulePath);
    }

    @Test
    void is_memory_class() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        instance.memories.inner().forEach((name, memory) -> {
            assertTrue(memory instanceof Memory);
        });

        instance.close();
    }

    @Test
    void initial_data() throws IOException, Exception {
        Instance instance = new Instance(getBytes());

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
    void read_memory() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        Memory memory = instance.memories.get("memory");
        byte[] readData = memory.read(0, 5);
        for (int i = 0; i < readData.length; i++) {
            assertEquals(0, readData[i]);
        }

        instance.close();
    }

    @Test
    void write_memory() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        Memory memory = instance.memories.get("memory");
        byte[] writeData = new byte[] {1, 2, 3, 4, 5};
        memory.write(0, writeData);

        byte[] readData = memory.read(0, 5);
        assertArrayEquals(writeData, readData);

        instance.close();
    }
}
