package org.wasmer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.RuntimeException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ModuleTest {
    private byte[] getBytes(String filename) throws IOException,Exception {
        Path modulePath = Paths.get(getClass().getClassLoader().getResource(filename).toURI());
        return Files.readAllBytes(modulePath);
    }

    @Test
    void validate() throws IOException,Exception {
        assertTrue(Module.validate(getBytes("tests.wasm")));
    }

    @Test
    void invalidate() throws IOException,Exception {
        assertFalse(Module.validate(getBytes("invalid.wasm")));
    }

    @Test
    void compile() throws IOException,Exception {
        assertTrue(new Module(getBytes("tests.wasm")) instanceof Module);
    }

    @Test
    void failedToCompile() throws IOException,Exception {
        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
            Module module = new Module(getBytes("invalid.wasm"));
        });

        String expected = "Failed to compile the module: Validation error: invalid leading byte in type definition";
        assertTrue(exception.getMessage().startsWith(expected));
    }

    @Test
    void instantiate() throws IOException,Exception {
        Module module = new Module(getBytes("tests.wasm"));

        Instance instance = module.instantiate();
        assertEquals(3, (Integer) instance.exports.getFunction("sum").apply(1, 2)[0]);

        instance.close();
        module.close();
    }

    @Test
    void serialize() throws IOException,Exception {
        Module module = new Module(getBytes("tests.wasm"));
        assertTrue(module.serialize() instanceof byte[]);
        module.close();
    }

    @Test
    void deserialize() throws IOException,Exception {
        Module module = new Module(getBytes("tests.wasm"));

        byte[] serialized = module.serialize();
        module = null;

        Module deserializedModule = Module.deserialize(serialized);
        assertEquals(3, (Integer) deserializedModule.instantiate().exports.getFunction("sum").apply(1, 2)[0]);
    }
}
