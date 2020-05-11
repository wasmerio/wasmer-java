import org.wasmer.Instance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class SimpleExample {
    public static void main(String[] args) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("simple.wasm"));
        Instance instance = new Instance(bytes);

        System.out.println((Integer) instance.exports.getFunction("sum").apply(1, 2)[0]);

        instance.close();
    }
}
