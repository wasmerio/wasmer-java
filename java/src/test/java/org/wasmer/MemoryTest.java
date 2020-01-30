package org.wasmer;

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

        assertTrue(instance.memory instanceof Memory);

        instance.close();
    }

    @Test
    void read_memory() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        Memory memory = instance.memory;
        byte[] readData = memory.read(0, 5);
        for (int i = 0; i < 5; i++) {
            assertEquals(0, readData[i]);
        }

        instance.close();
    }

    @Test
    void write_memory() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        Memory memory = instance.memory;
        byte[] writeData = new byte[] {1, 2, 3, 4, 5};
        memory.write(0, 5, writeData);

        byte[] readData = memory.read(0, 5);
        for (int i = 0; i < 5; i++) {
            assertEquals(i+1, readData[i]);
        }

        instance.close();
    }
}
