package org.wasmer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class InstanceTests {
    private byte[] getBytes() throws IOException,Exception {
        Path modulePath = Paths.get(getClass().getClassLoader().getResource("tests.wasm").getPath());
        return Files.readAllBytes(modulePath);
    }

    @Test
    void sum() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        Integer[] arguments = {1, 2};
        assertEquals(3, (Integer) instance.call("sum", arguments)[0]);

        instance.close();
    }

    @Test
    void i32_i32() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        Integer[] arguments = {42};
        assertEquals(42, (Integer) instance.call("i32_i32", arguments)[0]);

        instance.close();
    }

    @Test
    void i64_i64() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        Long[] arguments = {42l};
        assertEquals(42l, (Long) instance.call("i64_i64", arguments)[0]);

        instance.close();
    }

    @Test
    void f32_f32() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        Float[] arguments = {42.0f};
        assertEquals(42.0f, (Float) instance.call("f32_f32", arguments)[0]);

        instance.close();
    }

    @Test
    void f64_f64() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        Double[] arguments = {42.0d};
        assertEquals(42.0d, (Double) instance.call("f64_f64", arguments)[0]);

        instance.close();
    }

    @Test
    void i32_i64_f32_f64_f64() throws IOException,Exception {
        Instance instance = new Instance(getBytes());

        Object[] arguments = {1, 2l, 3.0f, 4.0d};
        assertEquals(10.0d, (Double) instance.call("i32_i64_f32_f64_f64", arguments)[0]);

        instance.close();
    }
}
