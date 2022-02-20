package org.wasmer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ImportsTest {
    private byte[] getBytes(String name) throws IOException,Exception {
        URL url = getClass().getClassLoader().getResource(name);
        Path modulePath = Paths.get(url.toURI());
        return Files.readAllBytes(modulePath);
    }
    private String getString(Integer ptr, ByteBuffer mbf) {
        StringBuilder sb = new StringBuilder();
        for(int i = ptr, max = mbf.limit(); i < max; i++) {
            mbf.position(i);
            byte b = mbf.get();
            if (b == 0) {
                break;
            }
            sb.appendCodePoint(b);
        }
        String result = sb.toString();
        return result;
    }

    @Test
    void simple() throws IOException,Exception {
        Module module = new Module(getBytes("import_simple.wasm"));
        AtomicReference<Number> arg1r = new AtomicReference<>();
        AtomicReference<Number> arg2r = new AtomicReference<>();
        AtomicReference<Number> ret1r = new AtomicReference<>();
        Imports imports = Imports.from(Collections.singletonList(
            new Imports.Spec("env", "mul_from_java", argv -> {
                arg1r.set(argv.get(0));
                arg2r.set(argv.get(1));
                int arg1 = argv.get(0).intValue();
                int arg2 = argv.get(1).intValue();
                int ret1 = arg1 * arg2;
                argv.set(0, ret1);
                ret1r.set(argv.get(0));
                return argv;
            }, Arrays.asList(Type.I32, Type.I32), Collections.singletonList(Type.I32))
        ), module.modulePointer);
        Instance instance = module.instantiate(imports);

        Object[] ret = instance.exports.getFunction("double_each_arg_then_mul").apply(2, 3);
        assertEquals(4, arg1r.get());
        assertEquals(6, arg2r.get());
        assertEquals(24, ret1r.get());
        assertEquals(1, ret.length);
        assertEquals(24, ret[0]);

        instance.close();
    }

    @Test
    void accessMemory() throws IOException,Exception {
        HashMap<String, String> msg = new HashMap<>();
        String msgKey1 = "key1";
        String msg1 = "World";
        String msgKey2 = "key2";
        String msg2 = "WebAssembly";
        msg.put(msgKey1, msg1);
        msg.put(msgKey2, msg2);

        Module module = new Module(getBytes("import_accessmemory.wasm"));
        AtomicReference<Instance> arInstance = new AtomicReference<>();
        Imports imports = Imports.from(Arrays.asList(
                new Imports.Spec("env", "get_greet_msg_len_from_java", argv -> {
                    Memory memory = arInstance.get().exports.getMemory("memory");
                    int msgKeyPtr = argv.get(0).intValue();
                    ByteBuffer mbf = memory.buffer();
                    String msgKey = getString(msgKeyPtr, mbf);
                    argv.set(0, msgKey.length());
                    return argv;
                }, Collections.singletonList(Type.I32), Collections.singletonList(Type.I32)),
                new Imports.Spec("env", "greet_from_java", argv -> {
                    Memory memory = arInstance.get().exports.getMemory("memory");
                    int msgKeyPtr = argv.get(0).intValue();
                    ByteBuffer mbf = memory.buffer();
                    String msgKey = getString(msgKeyPtr, mbf);
                    String msgValue = msg.get(msgKey);
                    byte[] msgValueBytes = msgValue.getBytes(StandardCharsets.UTF_8);
                    int msgValuePtr = argv.get(1).intValue();
                    mbf.position(msgValuePtr);
                    mbf.put(msgValueBytes);
                    return argv;
                }, Arrays.asList(Type.I32, Type.I32), Collections.singletonList(Type.I32))
        ), module.modulePointer);

        Instance instance = module.instantiate(imports);
        arInstance.set(instance);

        Memory memory = instance.exports.getMemory("memory");
        byte[] msgKey1Bytes = msgKey1.getBytes(StandardCharsets.UTF_8);
        byte[] msgKey2Bytes = msgKey2.getBytes(StandardCharsets.UTF_8);
        Integer msgKey1Ptr = (Integer) instance.exports.getFunction("allocate").apply(msgKey1Bytes.length)[0];
        Integer msgKey2Ptr = (Integer) instance.exports.getFunction("allocate").apply(msgKey2Bytes.length)[0];
        ByteBuffer mbf = memory.buffer();
        mbf.position(msgKey1Ptr);
        mbf.put(msgKey1Bytes);
        mbf.position(msgKey2Ptr);
        mbf.put(msgKey2Bytes);
        Integer ret1Ptr = (Integer) instance.exports.getFunction("greet").apply(msgKey1Ptr)[0];
        String ret1 = getString(ret1Ptr, mbf);
        Integer ret2Ptr = (Integer) instance.exports.getFunction("greet").apply(msgKey2Ptr)[0];
        String ret2 = getString(ret2Ptr, mbf);
        instance.exports.getFunction("deallocate").apply(msgKey1Ptr, msgKey1Bytes.length);
        instance.exports.getFunction("drop_string").apply(ret1Ptr);
        instance.exports.getFunction("deallocate").apply(msgKey2Ptr, msgKey2Bytes.length);
        instance.exports.getFunction("drop_string").apply(ret2Ptr);
        assertEquals("Hello, World!", ret1);
        assertEquals("Hello, WebAssembly!", ret2);

        instance.close();
        module.close();
    }
}
