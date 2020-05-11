import org.wasmer.Instance;
import org.wasmer.Memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class MemoryExample {
    public static void main(String[] args) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("memory.wasm"));
        Instance instance = new Instance(bytes);
        Integer pointer = (Integer) instance.exports.getFunction("return_hello").apply()[0];

        Memory memory = instance.exports.getMemory("memory");
        byte[] data = memory.read(pointer, 13);

        System.out.println("\"" + new String(data) + "\"");

        instance.close();
    }
}
