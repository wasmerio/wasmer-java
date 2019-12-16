package org.wasmer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class InstanceTests {
    @Test
    void basic() throws IOException,Exception {
        Path modulePath = Paths.get(getClass().getClassLoader().getResource("tests.wasm").getPath());
        byte[] moduleBytes = Files.readAllBytes(modulePath);

        Instance instance = new Instance(moduleBytes);

        Integer[] arguments = {1, 2};
        assertEquals(3, instance.call("sum", arguments));

        instance.close();
    }
}
