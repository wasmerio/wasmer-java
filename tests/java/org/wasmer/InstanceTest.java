package org.wasmer;

import org.wasmer.exports.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class InstanceTest {
    private byte[] getBytes() throws IOException,Exception {
        URL url = getClass().getClassLoader().getResource("tests.wasm");
        Path modulePath = Paths.get(url.toURI());
        return Files.readAllBytes(modulePath);
    }

    @Test
    void sum() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertEquals(3, instance.exports.getFunction("sum").apply(1, 2)[0]);

        instance.close();
    }

    @Test
    void arity_0() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertEquals(42, (Integer) instance.exports.getFunction("arity_0").apply()[0]);

        instance.close();
    }

    @Test
    void i32_i32() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertEquals(42, (Integer) instance.exports.getFunction("i32_i32").apply(42)[0]);

        instance.close();
    }

    @Test
    void i64_i64() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertEquals(42l, (Long) instance.exports.getFunction("i64_i64").apply(42l)[0]);

        instance.close();
    }

    @Test
    void f32_f32() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertEquals(42.0f, (Float) instance.exports.getFunction("f32_f32").apply(42.0f)[0]);

        instance.close();
    }

    @Test
    void f64_f64() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertEquals(42.0d, (Double) instance.exports.getFunction("f64_f64").apply(42.0d)[0]);

        instance.close();
    }

    @Test
    void i32_i64_f32_f64_f64() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertEquals(10.0d, (Double) instance.exports.getFunction("i32_i64_f32_f64_f64").apply(1, 2l, 3.0f, 4.0d)[0]);

        instance.close();
    }

    @Test
    void bool_casted_to_i32() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertTrue((Integer) instance.exports.getFunction("bool_casted_to_i32").apply()[0] == 1);

        instance.close();
    }

    @Test
    void string() throws IOException,Exception {
        Instance instance = new Instance(getBytes());
        Memory memory = instance.exports.getMemory("memory");

        int pointer = (Integer) instance.exports.getFunction("string").apply()[0];
        byte[] stringBytes = memory.read(pointer, 13);

        String expected = "Hello, World!";
        assertEquals(expected, new String(stringBytes));

        instance.close();
    }

    @Test
    void nothing() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertNull(instance.exports.getFunction("void").apply());

        instance.close();
    }
}
