package java.org.wasmer;

import org.junit.jupiter.api.Test;
import org.wasmer.Instance;
import org.wasmer.Module;
import org.wasmer.Imports;
import org.wasmer.Type;
import org.wasmer.exports.Function;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ImportsTest {
    private byte[] getBytes(String name) throws IOException,Exception {
        URL url = getClass().getClassLoader().getResource(name);
        Path modulePath = Paths.get(url.toURI());
        return Files.readAllBytes(modulePath);
    }

    @Test
    void nothing() throws IOException,Exception {
        Module module = new Module(getBytes("call_back.wat"));
        AtomicReference<Number> arg1r = new AtomicReference<>();
        AtomicReference<Number> arg2r = new AtomicReference<>();
        AtomicReference<Number> ret1r = new AtomicReference<>();
        Imports imports = Imports.from(Collections.singletonList(
            new Imports.Spec("env", "", argv -> {
                arg1r.set(argv.get(0));
                arg2r.set(argv.get(1));
                int arg1 = argv.get(0).intValue();
                int arg2 = argv.get(1).intValue();
                int ret1 = arg1 * arg2;
                argv.set(0, ret1);
                ret1r.set(argv.get(0));
                return argv;
            }, Arrays.asList(Type.I32, Type.I32), Collections.singletonList(Type.I32))
        ));
        Instance instance = module.instantiate(imports);

        Object[] ret = instance.exports.getFunction("mul").apply(2, 3);
        assertEquals(2, arg1r.get());
        assertEquals(3, arg2r.get());
        assertEquals(6, ret1r.get());
        assertEquals(1, ret.length);
        assertEquals(6, ret[0]);

        instance.close();
    }
}
