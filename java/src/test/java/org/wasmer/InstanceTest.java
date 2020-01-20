package org.wasmer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class InstanceTest {
    private byte[] getBytes() throws IOException,Exception {
        Path modulePath = Paths.get(getClass().getClassLoader().getResource("tests.wasm").getPath());
        return Files.readAllBytes(modulePath);
    }

    @Test
    void sum() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertEquals(3, (Integer) instance.exports.get("sum").apply(1, 2)[0]);

        instance.close();
    }

    @Test
    void arity_0() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertEquals(42, (Integer) instance.exports.get("arity_0").apply()[0]);

        instance.close();
    }

    @Test
    void i32_i32() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertEquals(42, (Integer) instance.exports.get("i32_i32").apply(42)[0]);

        instance.close();
    }

    @Test
    void i64_i64() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertEquals(42l, (Long) instance.exports.get("i64_i64").apply(42l)[0]);

        instance.close();
    }

    @Test
    void f32_f32() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertEquals(42.0f, (Float) instance.exports.get("f32_f32").apply(42.0f)[0]);

        instance.close();
    }

    @Test
    void f64_f64() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertEquals(42.0d, (Double) instance.exports.get("f64_f64").apply(42.0d)[0]);

        instance.close();
    }

    @Test
    void i32_i64_f32_f64_f64() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertEquals(10.0d, (Double) instance.exports.get("i32_i64_f32_f64_f64").apply(1, 2l, 3.0f, 4.0d)[0]);

        instance.close();
    }

    @Test
    void bool_casted_to_i32() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertTrue((Integer) instance.exports.get("bool_casted_to_i32").apply()[0] == 1);

        instance.close();
    }

    @Test
    void nothing() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        assertNull(instance.exports.get("void").apply());

        instance.close();
    }
}
